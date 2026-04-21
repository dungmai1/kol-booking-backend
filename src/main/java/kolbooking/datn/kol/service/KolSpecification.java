package kolbooking.datn.kol.service;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import kolbooking.datn.common.domain.Category;
import kolbooking.datn.kol.domain.KolPricingPackage;
import kolbooking.datn.kol.domain.KolProfile;
import kolbooking.datn.kol.domain.KolProfileStatus;
import kolbooking.datn.kol.domain.KolSocialChannel;
import kolbooking.datn.kol.dto.KolSearchFilter;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class KolSpecification {

    private KolSpecification() {}

    public static Specification<KolProfile> matches(KolSearchFilter f) {
        return (root, query, cb) -> {
            boolean isCount = query != null && query.getResultType() != null
                    && Long.class.equals(query.getResultType());
            if (query != null && !isCount) {
                query.distinct(true);
            }

            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("status"), KolProfileStatus.APPROVED));

            if (f.hasText()) {
                String like = "%" + f.q().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("displayName")), like),
                        cb.like(cb.lower(root.get("bio")), like)
                ));
            }
            if (f.city() != null && !f.city().isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("city")), f.city().toLowerCase()));
            }
            if (f.country() != null && !f.country().isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("country")), f.country().toLowerCase()));
            }
            if (f.gender() != null) {
                predicates.add(cb.equal(root.get("gender"), f.gender()));
            }
            if (f.minRating() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("avgRating"), f.minRating()));
            }
            if (f.hasCategories()) {
                Join<KolProfile, Category> cats = root.join("categories", JoinType.INNER);
                predicates.add(cats.get("id").in(f.categoryIds()));
            }
            if (f.hasPlatforms() || f.minFollower() != null || f.maxFollower() != null) {
                Join<KolProfile, KolSocialChannel> channels = root.join("channels", JoinType.INNER);
                if (f.hasPlatforms()) {
                    predicates.add(channels.get("platform").in(f.platforms()));
                }
                if (f.minFollower() != null) {
                    predicates.add(cb.greaterThanOrEqualTo(channels.get("followerCount"), f.minFollower()));
                }
                if (f.maxFollower() != null) {
                    predicates.add(cb.lessThanOrEqualTo(channels.get("followerCount"), f.maxFollower()));
                }
            }
            if (f.minPrice() != null || f.maxPrice() != null) {
                Join<KolProfile, KolPricingPackage> pkgs = root.join("pricingPackages", JoinType.INNER);
                if (f.minPrice() != null) {
                    predicates.add(cb.greaterThanOrEqualTo(pkgs.get("price"), f.minPrice()));
                }
                if (f.maxPrice() != null) {
                    predicates.add(cb.lessThanOrEqualTo(pkgs.get("price"), f.maxPrice()));
                }
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
