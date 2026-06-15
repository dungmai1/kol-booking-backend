package kolbooking.datn.notification.listener;

import kolbooking.datn.auth.domain.AppUser;
import kolbooking.datn.auth.repository.AppUserRepository;
import kolbooking.datn.auth.service.EmailService;
import kolbooking.datn.booking.domain.Booking;
import kolbooking.datn.booking.domain.BookingStatus;
import kolbooking.datn.booking.event.BookingMessageSentEvent;
import kolbooking.datn.booking.event.BookingStatusChangedEvent;
import kolbooking.datn.booking.repository.BookingRepository;
import kolbooking.datn.brand.domain.BrandProfile;
import kolbooking.datn.brand.repository.BrandProfileRepository;
import kolbooking.datn.kol.domain.KolProfile;
import kolbooking.datn.kol.repository.KolProfileRepository;
import kolbooking.datn.notification.domain.NotificationType;
import kolbooking.datn.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Translates booking domain events into persisted notifications + email dispatches.
 * Each handler runs in its own transaction after the publishing transaction commits.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BookingNotificationListener {

    private final NotificationService notificationService;
    private final EmailService emailService;
    private final BookingRepository bookingRepository;
    private final BrandProfileRepository brandProfileRepository;
    private final KolProfileRepository kolProfileRepository;
    private final AppUserRepository userRepository;

    @EventListener
    public void onStatusChanged(BookingStatusChangedEvent event) {
        Booking booking = bookingRepository.findById(event.bookingId()).orElse(null);
        if (booking == null) return;
        BrandProfile brand = brandProfileRepository.findById(booking.getBrandProfileId()).orElse(null);
        KolProfile kol = kolProfileRepository.findById(booking.getKolProfileId()).orElse(null);
        if (brand == null || kol == null) return;

        Long brandUserId = brand.getUserId();
        Long kolUserId = kol.getUserId();
        String title = booking.getCampaignTitle();
        String link = "/bookings/" + booking.getId();

        switch (event.toStatus()) {
            case PENDING -> notify(kolUserId, NotificationType.BOOKING_CREATED,
                    "Booking mới",
                    "Bạn có yêu cầu booking mới: " + title, link);
            case ACCEPTED -> notify(brandUserId, NotificationType.BOOKING_ACCEPTED,
                    "Booking được chấp nhận",
                    "KOL " + kol.getDisplayName() + " đã chấp nhận booking: " + title, link);
            case REJECTED -> notify(brandUserId, NotificationType.BOOKING_REJECTED,
                    "Booking bị từ chối",
                    "KOL " + kol.getDisplayName() + " đã từ chối booking: " + title, link);
            case CANCELLED -> {
                notify(brandUserId, NotificationType.BOOKING_CANCELLED,
                        "Booking đã huỷ", "Booking đã huỷ: " + title, link);
                notify(kolUserId, NotificationType.BOOKING_CANCELLED,
                        "Booking đã huỷ", "Booking đã huỷ: " + title, link);
            }
            case IN_PROGRESS -> notify(kolUserId, NotificationType.PAYMENT_SUCCESS,
                    "Thanh toán thành công",
                    "Brand đã thanh toán cho booking: " + title + ". Hãy thực hiện chiến dịch.", link);
            case DELIVERED -> notify(brandUserId, NotificationType.DELIVERABLE_SUBMITTED,
                    "Có deliverable mới",
                    "KOL " + kol.getDisplayName() + " đã submit deliverable cho: " + title, link);
            case COMPLETED -> {
                notify(kolUserId, NotificationType.BOOKING_COMPLETED,
                        "Booking hoàn tất",
                        "Booking \"" + title + "\" đã hoàn tất. Tiền đã được giải ngân.", link);
                notify(brandUserId, NotificationType.BOOKING_COMPLETED,
                        "Booking hoàn tất",
                        "Booking \"" + title + "\" đã hoàn tất.", link);
            }
            case DISPUTED -> notify(brandUserId, NotificationType.BOOKING_DISPUTED,
                    "Booking đang tranh chấp",
                    "Booking \"" + title + "\" đã được chuyển sang trạng thái tranh chấp.", link);
            case DELIVERY_REJECTED -> {
                notify(brandUserId, NotificationType.BOOKING_CANCELLED,
                        "Đã từ chối nội dung",
                        "Bạn đã từ chối deliverable. Ngân sách đã hoàn về ví: " + title, link);
                notify(kolUserId, NotificationType.BOOKING_CANCELLED,
                        "Brand từ chối nội dung",
                        "Brand đã từ chối deliverable cho: " + title + ". Bạn sẽ không nhận thanh toán.", link);
            }
            case CANCELLED_BY_ADMIN -> {
                notify(brandUserId, NotificationType.BOOKING_CANCELLED,
                        "Booking bị admin huỷ", "Booking \"" + title + "\" đã bị admin huỷ.", link);
                notify(kolUserId, NotificationType.BOOKING_CANCELLED,
                        "Booking bị admin huỷ", "Booking \"" + title + "\" đã bị admin huỷ.", link);
            }
        }
    }

    @EventListener
    public void onMessageSent(BookingMessageSentEvent event) {
        Booking booking = bookingRepository.findById(event.bookingId()).orElse(null);
        if (booking == null) return;
        BrandProfile brand = brandProfileRepository.findById(booking.getBrandProfileId()).orElse(null);
        KolProfile kol = kolProfileRepository.findById(booking.getKolProfileId()).orElse(null);
        if (brand == null || kol == null) return;

        Long recipient = event.senderUserId().equals(brand.getUserId()) ? kol.getUserId() : brand.getUserId();
        notificationService.send(recipient, NotificationType.NEW_MESSAGE,
                "Tin nhắn mới",
                "Bạn có tin nhắn mới trong booking \"" + booking.getCampaignTitle() + "\"",
                "/bookings/" + booking.getId() + "/messages");
        // per spec: no email for in-app messages
    }

    private void notify(Long userId, NotificationType type, String title, String message, String link) {
        if (userId == null) return;
        notificationService.send(userId, type, title, message, link);
        userRepository.findById(userId).map(AppUser::getEmail)
                .ifPresent(email -> emailService.sendNotification(email, title, message, link));
    }
}
