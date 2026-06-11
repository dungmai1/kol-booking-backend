package kolbooking.datn.product.repository;

import kolbooking.datn.product.domain.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    Page<Product> findByBrandProfileId(Long brandProfileId, Pageable pageable);

    /** Atomic counter bump so concurrent applications never lose an increment. */
    @Modifying
    @Query("UPDATE Product p SET p.applicationCount = p.applicationCount + 1 WHERE p.id = :id")
    void incrementApplicationCount(@Param("id") Long id);
}
