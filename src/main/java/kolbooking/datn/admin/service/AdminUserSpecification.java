package kolbooking.datn.admin.service;

import jakarta.persistence.criteria.Predicate;
import kolbooking.datn.auth.domain.AppUser;
import kolbooking.datn.auth.domain.Role;
import kolbooking.datn.auth.domain.UserStatus;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class AdminUserSpecification {

    private AdminUserSpecification() {}

    public static Specification<AppUser> matches(String q, Role role, UserStatus status) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (role != null) {
                predicates.add(cb.equal(root.get("role"), role));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (q != null && !q.isBlank()) {
                String like = "%" + q.trim().toLowerCase() + "%";
                predicates.add(cb.like(cb.lower(root.get("email")), like));
            }

            if (predicates.isEmpty()) {
                return cb.conjunction();
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
