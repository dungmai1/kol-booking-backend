package kolbooking.datn.product.service;

import jakarta.persistence.criteria.Predicate;
import kolbooking.datn.product.domain.Product;
import kolbooking.datn.product.domain.ProductStatus;
import kolbooking.datn.product.dto.ProductSearchFilter;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class ProductSpecification {

    private ProductSpecification() {}

    /** Public browse: only OPEN postings, narrowed by the optional filter fields. */
    public static Specification<Product> matches(ProductSearchFilter f) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("status"), ProductStatus.OPEN));

            if (f.hasText()) {
                String like = "%" + f.q().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), like),
                        cb.like(cb.lower(root.get("description")), like)));
            }
            if (f.categoryId() != null) {
                predicates.add(cb.equal(root.get("categoryId"), f.categoryId()));
            }
            if (f.platform() != null) {
                predicates.add(cb.equal(root.get("requiredPlatform"), f.platform()));
            }
            if (f.minBudget() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("budget"), f.minBudget()));
            }
            if (f.maxBudget() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("budget"), f.maxBudget()));
            }
            if (f.brandProfileId() != null) {
                predicates.add(cb.equal(root.get("brandProfileId"), f.brandProfileId()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
