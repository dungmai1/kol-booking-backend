package kolbooking.datn.booking.repository;

import kolbooking.datn.booking.domain.Booking;
import kolbooking.datn.booking.domain.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    Page<Booking> findByBrandProfileId(Long brandProfileId, Pageable pageable);
    Page<Booking> findByKolProfileId(Long kolProfileId, Pageable pageable);
    Page<Booking> findByKolProfileIdAndStatus(Long kolProfileId, BookingStatus status, Pageable pageable);
    Page<Booking> findByStatus(BookingStatus status, Pageable pageable);
    List<Booking> findAllByStatusAndCreatedAtBefore(BookingStatus status, Instant before);
    List<Booking> findAllByStatusAndUpdatedAtBefore(BookingStatus status, Instant before);
}
