package kolbooking.datn.kol.service;

import jakarta.persistence.EntityManager;
import kolbooking.datn.common.exception.ResourceNotFoundException;
import kolbooking.datn.common.util.SecurityUtils;
import kolbooking.datn.kol.domain.KolProfile;
import kolbooking.datn.kol.dto.KolAnalyticsOverview;
import kolbooking.datn.kol.dto.KolEarningsPoint;
import kolbooking.datn.kol.repository.KolProfileRepository;
import kolbooking.datn.payment.domain.Wallet;
import kolbooking.datn.payment.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class KolAnalyticsService {

    private final KolProfileRepository kolProfileRepository;
    private final WalletRepository walletRepository;
    private final EntityManager em;

    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public KolAnalyticsOverview overview() {
        Long userId = SecurityUtils.currentUserId();

        KolProfile kol = kolProfileRepository.findByUserId(userId)
                .orElseThrow(() -> ResourceNotFoundException.of("KolProfile", userId));

        Wallet wallet = walletRepository.findByUserId(userId)
                .orElse(null);

        BigDecimal available = wallet != null ? wallet.getBalanceAvailable() : BigDecimal.ZERO;
        BigDecimal pending   = wallet != null ? wallet.getBalanceHeld()      : BigDecimal.ZERO;

        // Total earned = sum of all RELEASE transactions on KOL's wallet (successful)
        BigDecimal totalEarned = BigDecimal.ZERO;
        if (wallet != null) {
            Number earned = (Number) em.createNativeQuery(
                    "SELECT COALESCE(SUM(amount), 0) FROM wallet_transaction " +
                    "WHERE wallet_id = :wid AND type = 'RELEASE' AND status = 'SUCCESS'")
                    .setParameter("wid", wallet.getId())
                    .getSingleResult();
            totalEarned = new BigDecimal(earned.toString());
        }

        // Booking counts grouped by status
        List<Object[]> rows = em.createNativeQuery(
                "SELECT status, COUNT(*) FROM booking " +
                "WHERE kol_profile_id = :kid GROUP BY status")
                .setParameter("kid", kol.getId())
                .getResultList();

        Map<String, Long> byStatus = new LinkedHashMap<>();
        long total = 0L;
        long completed = 0L;
        for (Object[] r : rows) {
            String status = (String) r[0];
            long count    = ((Number) r[1]).longValue();
            byStatus.put(status, count);
            total += count;
            if ("COMPLETED".equals(status)) completed = count;
        }

        BigDecimal completionRate = total == 0
                ? BigDecimal.ZERO
                : new BigDecimal(completed).divide(new BigDecimal(total), 4, RoundingMode.HALF_UP);

        return new KolAnalyticsOverview(
                available,
                pending,
                totalEarned,
                total,
                byStatus,
                completionRate,
                kol.getAvgRating(),
                kol.getReviewCount() != null ? kol.getReviewCount() : 0
        );
    }

    /**
     * Monthly earnings for the last {@code months} months, based on RELEASE wallet transactions.
     * Months with zero earnings are included (zero-filled).
     */
    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public List<KolEarningsPoint> earningsChart(int months) {
        Long userId = SecurityUtils.currentUserId();

        KolProfile kol = kolProfileRepository.findByUserId(userId)
                .orElseThrow(() -> ResourceNotFoundException.of("KolProfile", userId));
        Wallet wallet = walletRepository.findByUserId(userId).orElse(null);

        // Generate last N months labels
        List<String> labels = buildMonthLabels(months);

        if (wallet == null) {
            return labels.stream().map(m -> new KolEarningsPoint(m, BigDecimal.ZERO, 0L)).toList();
        }

        Instant from = Instant.now().minus(months * 31L, ChronoUnit.DAYS);

        // Earnings from RELEASE transactions (money received by KOL)
        List<Object[]> rows = em.createNativeQuery(
                "SELECT to_char(date_trunc('month', created_at), 'YYYY-MM') AS month, " +
                "       COALESCE(SUM(amount), 0)                           AS amount, " +
                "       COUNT(*)                                           AS cnt " +
                "FROM wallet_transaction " +
                "WHERE wallet_id = :wid AND type = 'RELEASE' AND status = 'SUCCESS' " +
                "  AND created_at >= :from " +
                "GROUP BY month ORDER BY month")
                .setParameter("wid", wallet.getId())
                .setParameter("from", from)
                .getResultList();

        Map<String, KolEarningsPoint> byMonth = new HashMap<>();
        for (Object[] r : rows) {
            String month = (String) r[0];
            BigDecimal amount = new BigDecimal(r[1].toString());
            long cnt = ((Number) r[2]).longValue();
            byMonth.put(month, new KolEarningsPoint(month, amount, cnt));
        }

        // Zero-fill all labels
        return labels.stream()
                .map(m -> byMonth.getOrDefault(m, new KolEarningsPoint(m, BigDecimal.ZERO, 0L)))
                .toList();
    }

    /**
     * Booking status breakdown for the KOL's dashboard — useful for a status pie chart.
     */
    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public Map<String, Long> bookingStatusBreakdown() {
        Long userId = SecurityUtils.currentUserId();
        KolProfile kol = kolProfileRepository.findByUserId(userId)
                .orElseThrow(() -> ResourceNotFoundException.of("KolProfile", userId));

        List<Object[]> rows = em.createNativeQuery(
                "SELECT status, COUNT(*) FROM booking " +
                "WHERE kol_profile_id = :kid GROUP BY status ORDER BY status")
                .setParameter("kid", kol.getId())
                .getResultList();

        Map<String, Long> result = new LinkedHashMap<>();
        for (Object[] r : rows) {
            result.put((String) r[0], ((Number) r[1]).longValue());
        }
        return result;
    }

    // Returns YYYY-MM labels for the last N months (oldest first)
    private static List<String> buildMonthLabels(int months) {
        java.time.YearMonth current = java.time.YearMonth.now();
        return java.util.stream.IntStream.range(0, months)
                .mapToObj(i -> current.minusMonths(months - 1 - i).toString())
                .toList();
    }
}
