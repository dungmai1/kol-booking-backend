package kolbooking.datn.kol.service;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
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
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("status"), KolProfileStatus.APPROVED));

            if (f.hasText()) {
                String like = "%" + f.q().toLowerCase().trim() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("displayName")), like),
                        cb.like(root.get("slug"), like)
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
            if (f.hasCategories() && query != null) {
                Subquery<Long> sub = query.subquery(Long.class);
                Root<KolProfile> corr = sub.correlate(root);
                Join<KolProfile, Category> catJoin = corr.join("categories");
                sub.select(cb.literal(1L))
                        .where(catJoin.get("id").in(f.categoryIds()));
                predicates.add(cb.exists(sub));
            }
            if ((f.hasPlatforms() || f.minFollower() != null || f.maxFollower() != null) && query != null) {
                Subquery<Long> sub = query.subquery(Long.class);
                Root<KolProfile> corr = sub.correlate(root);
                Join<KolProfile, KolSocialChannel> chJoin = corr.join("channels");
                List<Predicate> chPreds = new ArrayList<>();
                if (f.hasPlatforms()) {
                    chPreds.add(chJoin.get("platform").in(f.platforms()));
                }
                if (f.minFollower() != null) {
                    chPreds.add(cb.greaterThanOrEqualTo(chJoin.get("followerCount"), f.minFollower()));
                }
                if (f.maxFollower() != null) {
                    chPreds.add(cb.lessThanOrEqualTo(chJoin.get("followerCount"), f.maxFollower()));
                }
                sub.select(cb.literal(1L));
                if (!chPreds.isEmpty()) {
                    sub.where(chPreds.toArray(new Predicate[0]));
                }
                predicates.add(cb.exists(sub));
            }
            if ((f.minPrice() != null || f.maxPrice() != null) && query != null) {
                Subquery<Long> sub = query.subquery(Long.class);
                Root<KolProfile> corr = sub.correlate(root);
                Join<KolProfile, KolPricingPackage> pkJoin = corr.join("pricingPackages");
                List<Predicate> pkPreds = new ArrayList<>();
                if (f.minPrice() != null) {
                    pkPreds.add(cb.greaterThanOrEqualTo(pkJoin.get("price"), f.minPrice()));
                }
                if (f.maxPrice() != null) {
                    pkPreds.add(cb.lessThanOrEqualTo(pkJoin.get("price"), f.maxPrice()));
                }
                sub.select(cb.literal(1L));
                if (!pkPreds.isEmpty()) {
                    sub.where(pkPreds.toArray(new Predicate[0]));
                }
                predicates.add(cb.exists(sub));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
