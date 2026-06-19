package kolbooking.datn.product.repository;

import kolbooking.datn.product.domain.ApplicationMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicationMessageRepository extends JpaRepository<ApplicationMessage, Long> {

    Page<ApplicationMessage> findByApplicationId(Long applicationId, Pageable pageable);
}
