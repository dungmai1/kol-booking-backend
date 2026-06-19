package kolbooking.datn.booking.repository;

import kolbooking.datn.booking.domain.BookingDeliverable;
import kolbooking.datn.booking.domain.DeliverableStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BookingDeliverableRepository extends JpaRepository<BookingDeliverable, Long> {
    List<BookingDeliverable> findByBookingId(Long bookingId);

    Optional<BookingDeliverable> findTopByBookingIdAndStatusOrderBySubmittedAtDesc(
            Long bookingId, DeliverableStatus status);
}
