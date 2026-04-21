package kolbooking.datn.notification.listener;

import kolbooking.datn.auth.domain.AppUser;
import kolbooking.datn.auth.repository.AppUserRepository;
import kolbooking.datn.auth.service.EmailService;
import kolbooking.datn.notification.domain.NotificationType;
import kolbooking.datn.notification.service.NotificationService;
import kolbooking.datn.review.event.ReviewCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReviewNotificationListener {

    private final NotificationService notificationService;
    private final EmailService emailService;
    private final AppUserRepository userRepository;

    @EventListener
    public void onReviewCreated(ReviewCreatedEvent event) {
        String title = "Bạn vừa nhận đánh giá mới";
        String message = "Bạn vừa nhận được đánh giá " + event.rating() + "/5 từ booking #" + event.bookingId();
        String link = "/bookings/" + event.bookingId();
        notificationService.send(event.targetId(), NotificationType.REVIEW_RECEIVED, title, message, link);
        userRepository.findById(event.targetId()).map(AppUser::getEmail)
                .ifPresent(email -> emailService.sendNotification(email, title, message, link));
    }
}
