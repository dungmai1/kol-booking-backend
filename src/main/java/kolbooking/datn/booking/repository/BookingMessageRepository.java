package kolbooking.datn.booking.repository;

import kolbooking.datn.booking.domain.BookingMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingMessageRepository extends JpaRepository<BookingMessage, Long> {
    Page<BookingMessage> findByBookingIdOrderByCreatedAtAsc(Long bookingId, Pageable pageable);
}
