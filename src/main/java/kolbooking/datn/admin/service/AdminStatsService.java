package kolbooking.datn.admin.service;

import jakarta.persistence.EntityManager;
import kolbooking.datn.admin.dto.AdminCommissionTransactionResponse;
import kolbooking.datn.auth.domain.Role;
import kolbooking.datn.auth.repository.AppUserRepository;
import kolbooking.datn.booking.domain.Booking;
import kolbooking.datn.booking.repository.BookingRepository;
import kolbooking.datn.brand.domain.BrandProfile;
import kolbooking.datn.brand.repository.BrandProfileRepository;
import kolbooking.datn.common.dto.PageResponse;
import kolbooking.datn.kol.domain.KolProfile;
import kolbooking.datn.kol.repository.KolProfileRepository;
import kolbooking.datn.payment.domain.TransactionType;
import kolbooking.datn.payment.domain.WalletTransaction;
import kolbooking.datn.payment.repository.WalletTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminStatsService {

    private final AppUserRepository userRepository;
    private final WalletTransactionRepository transactionRepository;
    private final BookingRepository bookingRepository;
    private final BrandProfileRepository brandProfileRepository;
    private final KolProfileRepository kolProfileRepository;
    private final EntityManager em;

    @Value("${app.platform.fee-percent:10}")
    private BigDecimal defaultFeePercent;

    @Transactional(readOnly = true)
    public Map<String, Object> overview(Instant from, Instant to) {
        Map<String, Object> out = new LinkedHashMap<>();
        Map<String, Long> users = new HashMap<>();
        for (Role r : Role.values()) {
            users.put(r.name(), userRepository.countByRole(r));
        }
        out.put("users", users);

        // Bookings and GMV are filtered by the requested date range (booking creation time).
        Number bookingCount = (Number) em.createNativeQuery(
                "SELECT COUNT(*) FROM booking WHERE created_at BETWEEN :from AND :to")
                .setParameter("from", from)
                .setParameter("to", to)
                .getSingleResult();
        out.put("totalBookings", bookingCount.longValue());

        Number gmv = (Number) em.createNativeQuery(
                "SELECT COALESCE(SUM(budget),0) FROM booking WHERE status = 'COMPLETED' AND created_at BETWEEN :from AND :to")
                .setParameter("from", from)
                .setParameter("to", to)
                .getSingleResult();
        out.put("totalGmv", gmv == null ? BigDecimal.ZERO : new BigDecimal(gmv.toString()));

        Number platformRevenue = (Number) em.createNativeQuery(
                "SELECT COALESCE(SUM(amount),0) FROM wallet_transaction WHERE type = 'FEE' AND created_at BETWEEN :from AND :to")
                .setParameter("from", from)
                .setParameter("to", to)
                .getSingleResult();
        out.put("platformRevenue", platformRevenue == null
                ? BigDecimal.ZERO : new BigDecimal(platformRevenue.toString()));

        // Snapshot counts — date range does not apply.
        Number activeBookings = (Number) em.createNativeQuery(
                "SELECT COUNT(*) FROM booking WHERE status IN " +
                "('ACCEPTED','IN_PROGRESS','DELIVERED','DISPUTED')").getSingleResult();
        out.put("activeBookings", activeBookings.longValue());

        Number disputeCount = (Number) em.createNativeQuery(
                "SELECT COUNT(*) FROM booking WHERE status = 'DISPUTED'").getSingleResult();
        out.put("disputeCount", disputeCount.longValue());

        Number pendingKols = (Number) em.createNativeQuery(
                "SELECT COUNT(*) FROM kol_profile WHERE status = 'PENDING_REVIEW'").getSingleResult();
        out.put("pendingKolApprovals", pendingKols.longValue());

        Number pendingBrands = (Number) em.createNativeQuery(
                "SELECT COUNT(*) FROM brand_profile WHERE status = 'PENDING_REVIEW'").getSingleResult();
        out.put("pendingBrandApprovals", pendingBrands.longValue());

        return out;
    }

    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> bookingsByMonth(Instant from, Instant to) {
        List<Object[]> rows = em.createNativeQuery(
                "SELECT to_char(date_trunc('month', created_at), 'YYYY-MM') AS month, " +
                "       COUNT(*)                                         AS cnt, " +
                "       COALESCE(SUM(budget), 0)                         AS total " +
                "FROM booking " +
                "WHERE created_at BETWEEN :from AND :to " +
                "GROUP BY month ORDER BY month")
                .setParameter("from", from)
                .setParameter("to", to)
                .getResultList();
        return rows.stream().map(r -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("month", r[0]);
            m.put("count", ((Number) r[1]).longValue());
            m.put("total", r[2]);
            return m;
        }).toList();
    }

    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> topKols(int limit, Instant from, Instant to) {
        List<Object[]> rows = em.createNativeQuery(
                "SELECT b.kol_profile_id, " +
                "       SUM(COALESCE(b.kol_net_amount, " +
                "           ROUND(b.budget * (1 - b.platform_fee_percent / 100), 2))) AS revenue, " +
                "       COUNT(*) AS bookings, kp.display_name, kp.avg_rating " +
                "FROM booking b JOIN kol_profile kp ON kp.id = b.kol_profile_id " +
                "WHERE b.status = 'COMPLETED' AND b.created_at BETWEEN :from AND :to " +
                "GROUP BY b.kol_profile_id, kp.display_name, kp.avg_rating " +
                "ORDER BY revenue DESC LIMIT :limit")
                .setParameter("limit", limit)
                .setParameter("from", from)
                .setParameter("to", to)
                .getResultList();
        return rows.stream().map(r -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("kolProfileId", ((Number) r[0]).longValue());
            m.put("revenue", r[1]);
            m.put("bookings", ((Number) r[2]).longValue());
            m.put("displayName", r[3]);
            m.put("avgRating", r[4]);
            return m;
        }).toList();
    }

    /** Commission overview for the admin: current rate, accumulated platform fees, platform wallet. */
    @Transactional(readOnly = true)
    public Map<String, Object> commissionSummary() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("defaultFeePercent", defaultFeePercent);

        Number wallet = (Number) em.createNativeQuery(
                "SELECT COALESCE((SELECT balance_available FROM wallet WHERE user_id = 0), 0)")
                .getSingleResult();
        out.put("platformWalletAvailable", toBigDecimal(wallet));

        Number totalFees = (Number) em.createNativeQuery(
                "SELECT COALESCE(SUM(amount), 0) FROM wallet_transaction WHERE type = 'FEE'")
                .getSingleResult();
        out.put("totalCommission", toBigDecimal(totalFees));

        Number feeCount = (Number) em.createNativeQuery(
                "SELECT COUNT(*) FROM wallet_transaction WHERE type = 'FEE'")
                .getSingleResult();
        out.put("commissionTransactions", feeCount.longValue());
        return out;
    }

    /** Paginated FEE ledger with booking context so admin can trace commission sources. */
    @Transactional(readOnly = true)
    public PageResponse<AdminCommissionTransactionResponse> commissionTransactions(int page, int size) {
        Page<WalletTransaction> txs = transactionRepository.findByTypeOrderByCreatedAtDesc(
                TransactionType.FEE, PageRequest.of(page, size));

        Set<Long> bookingIds = txs.getContent().stream()
                .map(WalletTransaction::getBookingId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, Booking> bookings = bookingRepository.findAllById(bookingIds).stream()
                .collect(Collectors.toMap(Booking::getId, Function.identity()));

        Set<Long> brandIds = bookings.values().stream()
                .map(Booking::getBrandProfileId)
                .collect(Collectors.toSet());
        Set<Long> kolIds = bookings.values().stream()
                .map(Booking::getKolProfileId)
                .collect(Collectors.toSet());

        Map<Long, BrandProfile> brands = brandProfileRepository.findAllById(brandIds).stream()
                .collect(Collectors.toMap(BrandProfile::getId, Function.identity()));
        Map<Long, KolProfile> kols = kolProfileRepository.findAllById(kolIds).stream()
                .collect(Collectors.toMap(KolProfile::getId, Function.identity()));

        Page<AdminCommissionTransactionResponse> mapped = txs.map(tx -> {
            Booking booking = tx.getBookingId() == null ? null : bookings.get(tx.getBookingId());
            BrandProfile brand = booking == null ? null : brands.get(booking.getBrandProfileId());
            KolProfile kol = booking == null ? null : kols.get(booking.getKolProfileId());
            return new AdminCommissionTransactionResponse(
                    tx.getId(),
                    tx.getAmount(),
                    tx.getCreatedAt(),
                    tx.getBookingId(),
                    booking == null ? null : booking.getCampaignTitle(),
                    booking == null ? null : booking.getBudget(),
                    booking == null ? null : booking.getPlatformFeePercent(),
                    brand == null ? null : brand.getCompanyName(),
                    kol == null ? null : kol.getDisplayName(),
                    booking == null ? null : booking.getStatus().name(),
                    tx.getNote());
        });
        return PageResponse.of(mapped);
    }

    private static BigDecimal toBigDecimal(Number n) {
        return n == null ? BigDecimal.ZERO : new BigDecimal(n.toString());
    }

    /**
     * Financial-risk metrics for admin ops:
     * 1. totalEscrowHeld         — total brand funds currently frozen in escrow
     * 2. bookingsPendingApproval — DELIVERED bookings waiting for brand review/auto-complete
     * 3. refundRate              — DELIVERY_REJECTED / (COMPLETED + DELIVERY_REJECTED) in range
     * 4. totalRefunded           — sum of REFUND wallet-transactions in range
     */
    @Transactional(readOnly = true)
    public Map<String, Object> escrowMetrics(Instant from, Instant to) {
        Map<String, Object> out = new LinkedHashMap<>();

        // 1. Sum of balance_held across all brand wallets (current snapshot, not date-ranged).
        Number escrowHeld = (Number) em.createNativeQuery(
                "SELECT COALESCE(SUM(w.balance_held), 0) " +
                "FROM wallet w " +
                "JOIN app_user u ON u.id = w.user_id " +
                "WHERE u.role = 'BRAND'")
                .getSingleResult();
        out.put("totalEscrowHeld", toBigDecimal(escrowHeld));

        // 2. Count DELIVERED bookings (current snapshot — all are pending brand action).
        Number pendingApproval = (Number) em.createNativeQuery(
                "SELECT COUNT(*) FROM booking WHERE status = 'DELIVERED'")
                .getSingleResult();
        out.put("bookingsPendingApproval", pendingApproval.longValue());

        // 3. Refund rate = DELIVERY_REJECTED / (COMPLETED + DELIVERY_REJECTED) in date range.
        Number completed = (Number) em.createNativeQuery(
                "SELECT COUNT(*) FROM booking WHERE status = 'COMPLETED' AND created_at BETWEEN :from AND :to")
                .setParameter("from", from)
                .setParameter("to", to)
                .getSingleResult();
        Number rejected = (Number) em.createNativeQuery(
                "SELECT COUNT(*) FROM booking WHERE status = 'DELIVERY_REJECTED' AND created_at BETWEEN :from AND :to")
                .setParameter("from", from)
                .setParameter("to", to)
                .getSingleResult();
        long completedL = completed.longValue();
        long rejectedL  = rejected.longValue();
        long denominator = completedL + rejectedL;
        BigDecimal refundRate = denominator == 0
                ? BigDecimal.ZERO
                : new BigDecimal(rejectedL).divide(new BigDecimal(denominator), 4, RoundingMode.HALF_UP);
        out.put("refundRate", refundRate);
        out.put("completedBookings", completedL);
        out.put("rejectedDeliveries", rejectedL);

        // 4. Total refunded in date range (REFUND ledger entries).
        Number totalRefunded = (Number) em.createNativeQuery(
                "SELECT COALESCE(SUM(amount), 0) FROM wallet_transaction " +
                "WHERE type = 'REFUND' AND created_at BETWEEN :from AND :to")
                .setParameter("from", from)
                .setParameter("to", to)
                .getSingleResult();
        out.put("totalRefunded", toBigDecimal(totalRefunded));

        return out;
    }

    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> revenueByMonth(Instant from, Instant to) {
        List<Object[]> rows = em.createNativeQuery(
                "SELECT to_char(date_trunc('month', created_at), 'YYYY-MM') AS month, " +
                "       COALESCE(SUM(amount), 0)                         AS fee " +
                "FROM wallet_transaction " +
                "WHERE type = 'FEE' AND created_at BETWEEN :from AND :to " +
                "GROUP BY month ORDER BY month")
                .setParameter("from", from)
                .setParameter("to", to)
                .getResultList();
        return rows.stream().map(r -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("month", r[0]);
            m.put("fee", r[1]);
            return m;
        }).toList();
    }
}
