package kolbooking.datn.notification.listener;

import kolbooking.datn.notification.domain.NotificationType;
import kolbooking.datn.notification.service.NotificationService;
import kolbooking.datn.review.event.ReviewCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class ReviewNotificationListener {

    private final NotificationService notificationService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onReviewCreated(ReviewCreatedEvent event) {
        String title = "Bạn vừa nhận đánh giá mới";
        String message = "Bạn vừa nhận được đánh giá " + event.rating() + "/5 từ booking #" + event.bookingId();
        String link = "/bookings/" + event.bookingId();
        notificationService.send(event.targetId(), NotificationType.REVIEW_RECEIVED, title, message, link);
    }
}
