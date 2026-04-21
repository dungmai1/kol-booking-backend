package kolbooking.datn.review.repository;

import kolbooking.datn.review.domain.Review;
import kolbooking.datn.review.domain.ReviewDirection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    boolean existsByBookingIdAndDirection(Long bookingId, ReviewDirection direction);
    Optional<Review> findByBookingIdAndDirection(Long bookingId, ReviewDirection direction);
    Page<Review> findByTargetIdOrderByCreatedAtDesc(Long targetId, Pageable pageable);
}
