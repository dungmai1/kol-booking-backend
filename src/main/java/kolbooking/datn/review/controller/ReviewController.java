package kolbooking.datn.review.controller;

import jakarta.validation.Valid;
import kolbooking.datn.review.dto.ReviewCreateRequest;
import kolbooking.datn.review.dto.ReviewResponse;
import kolbooking.datn.review.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping("/bookings/{bookingId}/reviews")
    @PreAuthorize("hasAnyRole('BRAND','KOL')")
    public ResponseEntity<ReviewResponse> create(@PathVariable Long bookingId,
                                                 @Valid @RequestBody ReviewCreateRequest request) {
        return ResponseEntity.ok(reviewService.create(bookingId, request));
    }

    @PutMapping("/reviews/{reviewId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ReviewResponse> update(@PathVariable Long reviewId,
                                                 @Valid @RequestBody ReviewCreateRequest request) {
        return ResponseEntity.ok(reviewService.update(reviewId, request));
    }

    @GetMapping("/users/{userId}/reviews")
    public ResponseEntity<Page<ReviewResponse>> listForUser(@PathVariable Long userId,
                                                            @RequestParam(defaultValue = "0") int page,
                                                            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(reviewService.listForUser(userId, PageRequest.of(page, size)));
    }
}
