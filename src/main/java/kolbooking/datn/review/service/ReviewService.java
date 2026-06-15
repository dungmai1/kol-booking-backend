package kolbooking.datn.review.service;

import kolbooking.datn.auth.domain.Role;
import kolbooking.datn.booking.domain.Booking;
import kolbooking.datn.booking.domain.BookingStatus;
import kolbooking.datn.booking.service.BookingService;
import kolbooking.datn.brand.domain.BrandProfile;
import kolbooking.datn.brand.service.BrandProfileService;
import kolbooking.datn.common.exception.BusinessException;
import kolbooking.datn.common.exception.ErrorCode;
import kolbooking.datn.common.exception.ResourceNotFoundException;
import kolbooking.datn.common.util.SecurityUtils;
import kolbooking.datn.kol.domain.KolProfile;
import kolbooking.datn.kol.repository.KolProfileRepository;
import kolbooking.datn.review.domain.Review;
import kolbooking.datn.review.domain.ReviewDirection;
import kolbooking.datn.review.dto.ReviewCreateRequest;
import kolbooking.datn.review.dto.ReviewResponse;
import kolbooking.datn.review.event.ReviewCreatedEvent;
import kolbooking.datn.review.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService {

    private static final Duration EDIT_WINDOW = Duration.ofDays(7);

    private final ReviewRepository reviewRepository;
    private final BookingService bookingService;
    private final BrandProfileService brandProfileService;
    private final KolProfileRepository kolProfileRepository;
    private final ReviewAuthorEnricher reviewAuthorEnricher;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public ReviewResponse create(Long bookingId, ReviewCreateRequest req) {
        Booking booking = bookingService.getBookingEntity(bookingId);
        if (booking.getStatus() != BookingStatus.COMPLETED) {
            throw new BusinessException("Only COMPLETED bookings can be reviewed",
                    ErrorCode.BUSINESS_ERROR, HttpStatus.CONFLICT);
        }

        Long authorUserId = SecurityUtils.currentUserId();
        Role role = SecurityUtils.currentRole();

        ReviewDirection direction;
        Long targetUserId;
        KolProfile kol = kolProfileRepository.findById(booking.getKolProfileId())
                .orElseThrow(() -> new ResourceNotFoundException("KOL profile not found"));
        BrandProfile brand = brandProfileService.getById(booking.getBrandProfileId());

        if (role == Role.BRAND) {
            if (!brand.getUserId().equals(authorUserId)) {
                throw new BusinessException("Not your booking", ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
            }
            direction = ReviewDirection.BRAND_TO_KOL;
            targetUserId = kol.getUserId();
        } else if (role == Role.KOL) {
            if (!kol.getUserId().equals(authorUserId)) {
                throw new BusinessException("Not your booking", ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
            }
            direction = ReviewDirection.KOL_TO_BRAND;
            targetUserId = brand.getUserId();
        } else {
            throw new BusinessException("Only booking participants can review",
                    ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
        }

        if (reviewRepository.existsByBookingIdAndDirection(bookingId, direction)) {
            throw new BusinessException("You have already reviewed this booking",
                    ErrorCode.BUSINESS_ERROR, HttpStatus.CONFLICT);
        }

        Review review = Review.builder()
                .bookingId(bookingId)
                .authorId(authorUserId)
                .targetId(targetUserId)
                .direction(direction)
                .rating(req.rating())
                .comment(req.comment())
                .build();
        review = reviewRepository.save(review);

        if (direction == ReviewDirection.BRAND_TO_KOL) {
            recomputeKolRating(kol);
        }

        eventPublisher.publishEvent(new ReviewCreatedEvent(
                review.getId(), bookingId, authorUserId, targetUserId, direction, review.getRating()));
        log.info("Review created: id={}, bookingId={}, direction={}, rating={}",
                review.getId(), bookingId, direction, review.getRating());
        return reviewAuthorEnricher.toDto(review);
    }

    @Transactional
    public ReviewResponse update(Long reviewId, ReviewCreateRequest req) {
        Review r = reviewRepository.findById(reviewId)
                .orElseThrow(() -> ResourceNotFoundException.of("Review", reviewId));
        Long userId = SecurityUtils.currentUserId();
        if (!r.getAuthorId().equals(userId)) {
            throw new BusinessException("Not your review", ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
        }
        if (Duration.between(r.getCreatedAt(), Instant.now()).compareTo(EDIT_WINDOW) > 0) {
            throw new BusinessException("Edit window (7 days) elapsed",
                    ErrorCode.BUSINESS_ERROR, HttpStatus.CONFLICT);
        }
        r.setRating(req.rating());
        r.setComment(req.comment());
        Review saved = reviewRepository.save(r);
        if (saved.getDirection() == ReviewDirection.BRAND_TO_KOL) {
            kolProfileRepository.findById(bookingService.getBookingEntity(saved.getBookingId()).getKolProfileId())
                    .ifPresent(this::recomputeKolRating);
        }
        return reviewAuthorEnricher.toDto(saved);
    }

    @Transactional(readOnly = true)
    public Page<ReviewResponse> listForUser(Long targetUserId, Pageable pageable) {
        return reviewRepository.findByTargetIdOrderByCreatedAtDesc(targetUserId, pageable)
                .map(reviewAuthorEnricher::toDto);
    }

    private void recomputeKolRating(KolProfile kol) {
        Page<Review> all = reviewRepository.findByTargetIdOrderByCreatedAtDesc(
                kol.getUserId(), Pageable.unpaged());
        long count = all.getTotalElements();
        BigDecimal avg = BigDecimal.ZERO;
        if (count > 0) {
            int sum = all.getContent().stream().mapToInt(Review::getRating).sum();
            avg = BigDecimal.valueOf(sum).divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
        }
        kol.setAvgRating(avg);
        kol.setReviewCount((int) count);
        kolProfileRepository.save(kol);
    }
}
