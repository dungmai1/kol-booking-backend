package kolbooking.datn.payment.repository;

import kolbooking.datn.payment.domain.PaymentOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentOrderRepository extends JpaRepository<PaymentOrder, Long> {
    Optional<PaymentOrder> findByExternalRef(String externalRef);
    Optional<PaymentOrder> findFirstByBookingIdOrderByCreatedAtDesc(Long bookingId);
}
