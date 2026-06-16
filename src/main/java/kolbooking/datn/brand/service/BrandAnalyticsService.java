package kolbooking.datn.brand.service;

import jakarta.persistence.EntityManager;
import kolbooking.datn.brand.domain.BrandProfile;
import kolbooking.datn.brand.dto.BrandAnalyticsOverview;
import kolbooking.datn.brand.dto.BrandSpendingPoint;
import kolbooking.datn.brand.repository.BrandProfileRepository;
import kolbooking.datn.common.exception.ResourceNotFoundException;
import kolbooking.datn.common.util.SecurityUtils;
import kolbooking.datn.payment.domain.Wallet;
import kolbooking.datn.payment.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class BrandAnalyticsService {

    private final BrandProfileRepository brandProfileRepository;
    private final WalletRepository walletRepository;
    private final EntityManager em;

    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public BrandAnalyticsOverview overview() {
        Long userId = SecurityUtils.currentUserId();

        BrandProfile brand = brandProfileRepository.findByUserId(userId)
                .orElseThrow(() -> ResourceNotFoundException.of("BrandProfile", userId));

        Wallet wallet = walletRepository.findByUserId(userId).orElse(null);
        BigDecimal pendingEscrow = wallet != null ? wallet.getBalanceHeld() : BigDecimal.ZERO;

        // Booking counts by status
        List<Object[]> rows = em.createNativeQuery(
                "SELECT status, COUNT(*) FROM booking " +
                "WHERE brand_profile_id = :bid GROUP BY status")
                .setParameter("bid", brand.getId())
                .getResultList();

        Map<String, Long> byStatus = new LinkedHashMap<>();
        long total = 0L, active = 0L, completed = 0L;
        for (Object[] r : rows) {
            String status = (String) r[0];
            long count    = ((Number) r[1]).longValue();
            byStatus.put(status, count);
            total += count;
            if ("COMPLETED".equals(status)) completed = count;
            if ("ACCEPTED".equals(status) || "IN_PROGRESS".equals(status) || "DELIVERED".equals(status)) {
                active += count;
            }
        }

        // Total spend on COMPLETED bookings
        Number spendRaw = (Number) em.createNativeQuery(
                "SELECT COALESCE(SUM(budget), 0) FROM booking " +
                "WHERE brand_profile_id = :bid AND status = 'COMPLETED'")
                .setParameter("bid", brand.getId())
                .getSingleResult();
        BigDecimal totalSpend = new BigDecimal(spendRaw.toString());

        // Average budget across all bookings
        BigDecimal avgBudget = total == 0
                ? BigDecimal.ZERO
                : totalSpend.divide(new BigDecimal(total), 2, RoundingMode.HALF_UP);

        return new BrandAnalyticsOverview(
                total, active, completed,
                totalSpend, avgBudget, pendingEscrow,
                byStatus
        );
    }

    /**
     * Monthly spending for the last {@code months} months (zero-filled).
     * Spend is counted when the booking was CREATED (matches budget committed at that time).
     */
    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public List<BrandSpendingPoint> spendingChart(int months) {
        Long userId = SecurityUtils.currentUserId();

        BrandProfile brand = brandProfileRepository.findByUserId(userId)
                .orElseThrow(() -> ResourceNotFoundException.of("BrandProfile", userId));

        List<String> labels = buildMonthLabels(months);
        Instant from = Instant.now().minus(months * 31L, ChronoUnit.DAYS);

        List<Object[]> rows = em.createNativeQuery(
                "SELECT to_char(date_trunc('month', created_at), 'YYYY-MM') AS month, " +
                "       COALESCE(SUM(budget), 0)                            AS spend, " +
                "       COUNT(*)                                            AS cnt " +
                "FROM booking " +
                "WHERE brand_profile_id = :bid " +
                "  AND status = 'COMPLETED' " +
                "  AND created_at >= :from " +
                "GROUP BY month ORDER BY month")
                .setParameter("bid", brand.getId())
                .setParameter("from", from)
                .getResultList();

        Map<String, BrandSpendingPoint> byMonth = new HashMap<>();
        for (Object[] r : rows) {
            String month   = (String) r[0];
            BigDecimal spend = new BigDecimal(r[1].toString());
            long cnt = ((Number) r[2]).longValue();
            byMonth.put(month, new BrandSpendingPoint(month, spend, cnt));
        }

        return labels.stream()
                .map(m -> byMonth.getOrDefault(m, new BrandSpendingPoint(m, BigDecimal.ZERO, 0L)))
                .toList();
    }

    /** Campaign breakdown by status — for a pie/donut chart on Brand dashboard. */
    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public Map<String, Long> campaignStatusBreakdown() {
        Long userId = SecurityUtils.currentUserId();
        BrandProfile brand = brandProfileRepository.findByUserId(userId)
                .orElseThrow(() -> ResourceNotFoundException.of("BrandProfile", userId));

        List<Object[]> rows = em.createNativeQuery(
                "SELECT status, COUNT(*) FROM booking " +
                "WHERE brand_profile_id = :bid GROUP BY status ORDER BY status")
                .setParameter("bid", brand.getId())
                .getResultList();

        Map<String, Long> result = new LinkedHashMap<>();
        for (Object[] r : rows) {
            result.put((String) r[0], ((Number) r[1]).longValue());
        }
        return result;
    }

    private static List<String> buildMonthLabels(int months) {
        YearMonth current = YearMonth.now();
        return IntStream.range(0, months)
                .mapToObj(i -> current.minusMonths(months - 1 - i).toString())
                .toList();
    }
}
