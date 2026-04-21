package kolbooking.datn.admin.service;

import jakarta.persistence.EntityManager;
import kolbooking.datn.auth.domain.Role;
import kolbooking.datn.auth.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminStatsService {

    private final AppUserRepository userRepository;
    private final EntityManager em;

    @Transactional(readOnly = true)
    public Map<String, Object> overview() {
        Map<String, Object> out = new LinkedHashMap<>();
        Map<String, Long> users = new HashMap<>();
        for (Role r : Role.values()) {
            users.put(r.name(), userRepository.countByRole(r));
        }
        out.put("users", users);

        Number bookingCount = (Number) em.createQuery("select count(b) from Booking b").getSingleResult();
        out.put("totalBookings", bookingCount.longValue());

        Number gmv = (Number) em.createNativeQuery(
                "SELECT COALESCE(SUM(budget),0) FROM booking WHERE status = 'COMPLETED'").getSingleResult();
        out.put("totalGmv", gmv == null ? BigDecimal.ZERO : new BigDecimal(gmv.toString()));

        Number platformRevenue = (Number) em.createNativeQuery(
                "SELECT COALESCE(SUM(amount),0) FROM wallet_transaction WHERE type = 'FEE'")
                .getSingleResult();
        out.put("platformRevenue", platformRevenue == null
                ? BigDecimal.ZERO : new BigDecimal(platformRevenue.toString()));

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
    public List<Map<String, Object>> topKols(int limit) {
        List<Object[]> rows = em.createNativeQuery(
                "SELECT b.kol_profile_id, COALESCE(SUM(b.budget), 0) AS revenue, COUNT(*) AS bookings " +
                "FROM booking b WHERE b.status = 'COMPLETED' " +
                "GROUP BY b.kol_profile_id ORDER BY revenue DESC LIMIT :limit")
                .setParameter("limit", limit)
                .getResultList();
        return rows.stream().map(r -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("kolProfileId", ((Number) r[0]).longValue());
            m.put("revenue", r[1]);
            m.put("bookings", ((Number) r[2]).longValue());
            return m;
        }).toList();
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
