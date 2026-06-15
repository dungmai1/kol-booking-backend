package kolbooking.datn.admin.service;

import kolbooking.datn.admin.dto.DisputeResolutionRequest;
import kolbooking.datn.booking.domain.Booking;
import kolbooking.datn.booking.domain.BookingStatus;
import kolbooking.datn.booking.domain.BookingStatusHistory;
import kolbooking.datn.booking.dto.BookingResponse;
import kolbooking.datn.booking.dto.ReasonRequest;
import kolbooking.datn.booking.repository.BookingRepository;
import kolbooking.datn.booking.repository.BookingStatusHistoryRepository;
import kolbooking.datn.booking.service.BookingService;
import kolbooking.datn.brand.domain.BrandProfile;
import kolbooking.datn.brand.repository.BrandProfileRepository;
import kolbooking.datn.common.exception.BusinessException;
import kolbooking.datn.common.exception.ErrorCode;
import kolbooking.datn.common.exception.ResourceNotFoundException;
import kolbooking.datn.common.util.SecurityUtils;
import kolbooking.datn.kol.domain.KolProfile;
import kolbooking.datn.kol.repository.KolProfileRepository;
import kolbooking.datn.payment.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminBookingService {

    private final BookingRepository bookingRepository;
    private final BookingStatusHistoryRepository historyRepository;
    private final BookingService bookingService;
    private final KolProfileRepository kolProfileRepository;
    private final BrandProfileRepository brandProfileRepository;
    private final WalletService walletService;
    private final AuditService auditService;

    @Value("${app.platform.fee-percent:10}")
    private BigDecimal platformFeePercent;

    @Transactional(readOnly = true)
    public Page<Booking> list(BookingStatus status, Pageable pageable) {
        return status == null
                ? bookingRepository.findAll(pageable)
                : bookingRepository.findByStatus(status, pageable);
    }

    @Transactional
    public Booking resolveDispute(Long bookingId, DisputeResolutionRequest req) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> ResourceNotFoundException.of("Booking", bookingId));
        if (booking.getStatus() != BookingStatus.DISPUTED) {
            throw new BusinessException("Booking is not in DISPUTED state",
                    ErrorCode.BUSINESS_ERROR, HttpStatus.CONFLICT);
        }

        BrandProfile brand = brandProfileRepository.findById(booking.getBrandProfileId())
                .orElseThrow(() -> ResourceNotFoundException.of("BrandProfile", booking.getBrandProfileId()));
        KolProfile kol = kolProfileRepository.findById(booking.getKolProfileId())
                .orElseThrow(() -> ResourceNotFoundException.of("KolProfile", booking.getKolProfileId()));
        BigDecimal amount = booking.getBudget();

        switch (req.action()) {
            case REFUND_BRAND -> {
                walletService.refundBrand(brand.getUserId(), amount, bookingId,
                        "Dispute refund: " + safeNote(req));
                booking.setStatus(BookingStatus.CANCELLED_BY_ADMIN);
            }
            case PAY_KOL -> {
                walletService.releaseToKol(brand.getUserId(), kol.getUserId(),
                        amount, platformFeePercent, bookingId);
                booking.setStatus(BookingStatus.COMPLETED);
            }
            case SPLIT -> {
                BigDecimal splitPct = req.splitPercentToKol() == null
                        ? BigDecimal.valueOf(50) : req.splitPercentToKol();
                BigDecimal kolShare = amount.multiply(splitPct)
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                BigDecimal brandShare = amount.subtract(kolShare);
                if (brandShare.signum() > 0) {
                    walletService.refundBrand(brand.getUserId(), brandShare, bookingId,
                            "Dispute split refund: " + safeNote(req));
                }
                if (kolShare.signum() > 0) {
                    walletService.releaseToKol(brand.getUserId(), kol.getUserId(),
                            kolShare, platformFeePercent, bookingId);
                }
                booking.setStatus(BookingStatus.COMPLETED);
            }
        }

        booking.setCancelReason(safeNote(req));
        bookingRepository.save(booking);
        recordHistory(bookingId, BookingStatus.DISPUTED, booking.getStatus(), safeNote(req));
        auditService.record("DISPUTE_RESOLVE_" + req.action().name(),
                "Booking", bookingId, safeNote(req));
        log.info("Dispute resolved: bookingId={}, action={}", bookingId, req.action());
        return booking;
    }

    /** Admin-cancel a booking (any non-terminal status). Audit-logged. */
    @Transactional
    public BookingResponse cancelBooking(Long bookingId, ReasonRequest req) {
        BookingResponse response = bookingService.adminCancel(bookingId, req);
        auditService.record("BOOKING_CANCEL_BY_ADMIN", "Booking", bookingId,
                req == null ? "" : req.reason());
        return response;
    }

    private void recordHistory(Long bookingId, BookingStatus from, BookingStatus to, String note) {
        historyRepository.save(BookingStatusHistory.builder()
                .bookingId(bookingId)
                .fromStatus(from)
                .toStatus(to)
                .changedByUser(SecurityUtils.currentUserIdSafe())
                .note(note)
                .build());
    }

    private String safeNote(DisputeResolutionRequest req) {
        return req.note() == null ? "" : req.note();
    }
}
