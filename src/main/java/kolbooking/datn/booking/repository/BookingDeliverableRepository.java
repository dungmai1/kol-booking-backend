package kolbooking.datn.booking.repository;

import kolbooking.datn.booking.domain.BookingDeliverable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BookingDeliverableRepository extends JpaRepository<BookingDeliverable, Long> {
    List<BookingDeliverable> findByBookingId(Long bookingId);
}
