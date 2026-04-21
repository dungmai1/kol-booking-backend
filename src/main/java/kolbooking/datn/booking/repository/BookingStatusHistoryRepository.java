package kolbooking.datn.booking.repository;

import kolbooking.datn.booking.domain.BookingStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BookingStatusHistoryRepository extends JpaRepository<BookingStatusHistory, Long> {
    List<BookingStatusHistory> findByBookingIdOrderByChangedAtAsc(Long bookingId);
}
