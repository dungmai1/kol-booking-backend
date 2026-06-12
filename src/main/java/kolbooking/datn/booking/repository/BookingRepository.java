package kolbooking.datn.booking.repository;

import kolbooking.datn.booking.domain.Booking;
import kolbooking.datn.booking.domain.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    Page<Booking> findByBrandProfileId(Long brandProfileId, Pageable pageable);
    Page<Booking> findByKolProfileId(Long kolProfileId, Pageable pageable);
    Page<Booking> findByKolProfileIdAndStatus(Long kolProfileId, BookingStatus status, Pageable pageable);
    Page<Booking> findByStatus(BookingStatus status, Pageable pageable);
    List<Booking> findAllByStatusAndCreatedAtBefore(BookingStatus status, Instant before);
    List<Booking> findAllByStatusAndUpdatedAtBefore(BookingStatus status, Instant before);

    @Query("""
            SELECT b.kolProfileId, COUNT(b)
            FROM Booking b
            WHERE b.kolProfileId IN :kolProfileIds
              AND b.status IN :statuses
            GROUP BY b.kolProfileId
            """)
    List<Object[]> countByKolProfileIdsAndStatuses(
            @Param("kolProfileIds") Collection<Long> kolProfileIds,
            @Param("statuses") Collection<BookingStatus> statuses
    );
}
