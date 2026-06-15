package kolbooking.datn.booking.service;

import jakarta.transaction.Transactional;
import kolbooking.datn.booking.domain.Booking;
import kolbooking.datn.booking.domain.BookingStatus;
import kolbooking.datn.booking.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookingScheduler {

    private final BookingRepository bookingRepository;
    private final BookingService bookingService;

    @Value("${app.booking.pending-expire-days:7}")
    private long pendingExpireDays;

    @Value("${app.booking.auto-complete-days:3}")
    private long autoCompleteDays;

    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void expireStalePendingBookings() {
        Instant threshold = Instant.now().minus(pendingExpireDays, ChronoUnit.DAYS);
        List<Booking> stale = bookingRepository.findAllByStatusAndCreatedAtBefore(
                BookingStatus.PENDING, threshold);
        stale.forEach(b -> {
            try {
                bookingService.transition(b, BookingStatus.CANCELLED, "Auto-cancelled: PENDING exceeded " + pendingExpireDays + " days");
            } catch (Exception ex) {
                log.warn("Failed to auto-cancel booking {}: {}", b.getId(), ex.getMessage());
            }
        });
    }

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void autoCompleteDeliveredBookings() {
        Instant threshold = Instant.now().minus(autoCompleteDays, ChronoUnit.DAYS);
        List<Booking> stale = bookingRepository.findAllByStatusAndUpdatedAtBefore(
                BookingStatus.DELIVERED, threshold);
        stale.forEach(b -> {
            try {
                bookingService.transition(b, BookingStatus.COMPLETED,
                        "Auto-completed: Brand did not respond within " + autoCompleteDays + " days");
            } catch (Exception ex) {
                log.warn("Failed to auto-complete booking {}: {}", b.getId(), ex.getMessage());
            }
        });
    }
}
