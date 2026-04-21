package kolbooking.datn.booking.service;

import jakarta.transaction.Transactional;
import kolbooking.datn.auth.domain.Role;
import kolbooking.datn.booking.domain.Booking;
import kolbooking.datn.booking.domain.BookingDeliverable;
import kolbooking.datn.booking.domain.BookingMessage;
import kolbooking.datn.booking.domain.BookingStatus;
import kolbooking.datn.booking.domain.BookingStatusHistory;
import kolbooking.datn.booking.domain.DeliverableStatus;
import kolbooking.datn.booking.dto.BookingMessageRequest;
import kolbooking.datn.booking.dto.BookingMessageResponse;
import kolbooking.datn.booking.dto.BookingResponse;
import kolbooking.datn.booking.dto.CreateBookingRequest;
import kolbooking.datn.booking.dto.ReasonRequest;
import kolbooking.datn.booking.dto.SubmitDeliverableRequest;
import kolbooking.datn.booking.event.BookingMessageSentEvent;
import kolbooking.datn.booking.event.BookingStatusChangedEvent;
import kolbooking.datn.booking.repository.BookingDeliverableRepository;
import kolbooking.datn.booking.repository.BookingMessageRepository;
import kolbooking.datn.booking.repository.BookingRepository;
import kolbooking.datn.booking.repository.BookingStatusHistoryRepository;
import kolbooking.datn.brand.domain.BrandProfile;
import kolbooking.datn.brand.service.BrandProfileService;
import kolbooking.datn.common.dto.PageResponse;
import kolbooking.datn.common.exception.BusinessException;
import kolbooking.datn.common.exception.ErrorCode;
import kolbooking.datn.common.exception.ResourceNotFoundException;
import kolbooking.datn.common.util.SecurityUtils;
import kolbooking.datn.kol.domain.KolProfile;
import kolbooking.datn.kol.service.KolProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final BookingStatusHistoryRepository historyRepository;
    private final BookingMessageRepository messageRepository;
    private final BookingDeliverableRepository deliverableRepository;
    private final KolProfileService kolProfileService;
    private final BrandProfileService brandProfileService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public BookingResponse createBooking(CreateBookingRequest req) {
        if (SecurityUtils.currentRole() != Role.BRAND) {
            throw new BusinessException("Only BRAND can create bookings",
                    ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
        }
        BrandProfile brand = brandProfileService.getCurrentBrandProfile();
        KolProfile kol = kolProfileService.requireApprovedById(req.kolProfileId());

        Booking b = Booking.builder()
                .brandProfileId(brand.getId())
                .kolProfileId(kol.getId())
                .campaignTitle(req.campaignTitle())
                .campaignBrief(req.campaignBrief())
                .deliverables(req.deliverables())
                .budget(req.budget())
                .startDate(req.startDate())
                .endDate(req.endDate())
                .status(BookingStatus.PENDING)
                .build();
        b = bookingRepository.save(b);

        recordHistory(b.getId(), null, BookingStatus.PENDING, SecurityUtils.currentUserId(), null);
        eventPublisher.publishEvent(new BookingStatusChangedEvent(
                b.getId(), null, BookingStatus.PENDING, SecurityUtils.currentUserId()));
        log.info("Booking created: bookingId={}, brand={}, kol={}, budget={}",
                b.getId(), brand.getId(), kol.getId(), b.getBudget());
        return BookingMapper.toDto(b);
    }

    @Transactional
    public BookingResponse acceptByKol(Long bookingId) {
        Booking b = requireBookingFor(bookingId, Role.KOL);
        transition(b, BookingStatus.ACCEPTED, null);
        return BookingMapper.toDto(b);
    }

    @Transactional
    public BookingResponse rejectByKol(Long bookingId, ReasonRequest req) {
        Booking b = requireBookingFor(bookingId, Role.KOL);
        transition(b, BookingStatus.REJECTED, req == null ? null : req.reason());
        b.setRejectReason(req == null ? null : req.reason());
        return BookingMapper.toDto(b);
    }

    @Transactional
    public BookingResponse cancelByBrand(Long bookingId, ReasonRequest req) {
        Booking b = requireBookingFor(bookingId, Role.BRAND);
        if (b.getStatus() != BookingStatus.PENDING) {
            throw new BusinessException("Only PENDING bookings can be cancelled by Brand",
                    ErrorCode.BUSINESS_ERROR, HttpStatus.CONFLICT);
        }
        transition(b, BookingStatus.CANCELLED, req == null ? null : req.reason());
        b.setCancelReason(req == null ? null : req.reason());
        return BookingMapper.toDto(b);
    }

    @Transactional
    public BookingResponse markInProgress(Long bookingId) {
        Booking b = bookingRepository.findById(bookingId)
                .orElseThrow(() -> ResourceNotFoundException.of("Booking", bookingId));
        transition(b, BookingStatus.IN_PROGRESS, "Payment received");
        return BookingMapper.toDto(b);
    }

    @Transactional
    public BookingResponse submitDeliverable(Long bookingId, SubmitDeliverableRequest req) {
        Booking b = requireBookingFor(bookingId, Role.KOL);
        if (b.getStatus() != BookingStatus.IN_PROGRESS) {
            throw new BusinessException("Booking must be IN_PROGRESS to submit deliverable",
                    ErrorCode.BUSINESS_ERROR, HttpStatus.CONFLICT);
        }
        BookingDeliverable d = BookingDeliverable.builder()
                .bookingId(b.getId())
                .type(req.type())
                .platform(req.platform())
                .submittedUrl(req.submittedUrl())
                .submittedAt(java.time.Instant.now())
                .status(DeliverableStatus.SUBMITTED)
                .build();
        deliverableRepository.save(d);

        transition(b, BookingStatus.DELIVERED, "Deliverable submitted");
        return BookingMapper.toDto(b);
    }

    @Transactional
    public BookingResponse approveDelivery(Long bookingId) {
        Booking b = requireBookingFor(bookingId, Role.BRAND);
        transition(b, BookingStatus.COMPLETED, "Brand approved delivery");
        return BookingMapper.toDto(b);
    }

    @Transactional
    public BookingResponse dispute(Long bookingId, ReasonRequest req) {
        Booking b = requireBookingFor(bookingId, Role.BRAND);
        if (b.getStatus() != BookingStatus.DELIVERED) {
            throw new BusinessException("Only DELIVERED bookings can be disputed",
                    ErrorCode.BUSINESS_ERROR, HttpStatus.CONFLICT);
        }
        transition(b, BookingStatus.DISPUTED, req == null ? null : req.reason());
        return BookingMapper.toDto(b);
    }

    @Transactional
    public BookingResponse adminCancel(Long bookingId, ReasonRequest req) {
        Booking b = bookingRepository.findById(bookingId)
                .orElseThrow(() -> ResourceNotFoundException.of("Booking", bookingId));
        transition(b, BookingStatus.CANCELLED_BY_ADMIN, req == null ? null : req.reason());
        b.setCancelReason(req == null ? null : req.reason());
        return BookingMapper.toDto(b);
    }

    public BookingResponse getBookingForParticipant(Long bookingId) {
        return BookingMapper.toDto(requireBookingForEither(bookingId));
    }

    public Booking getBookingEntity(Long bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> ResourceNotFoundException.of("Booking", bookingId));
    }

    public PageResponse<BookingResponse> listMineForBrand(int page, int size) {
        if (size <= 0 || size > 100) size = 20;
        if (page < 0) page = 0;
        BrandProfile brand = brandProfileService.getCurrentBrandProfile();
        Page<Booking> result = bookingRepository.findByBrandProfileId(
                brand.getId(), PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return PageResponse.of(result.map(BookingMapper::toDto));
    }

    public PageResponse<BookingResponse> listIncomingForKol(int page, int size) {
        if (size <= 0 || size > 100) size = 20;
        if (page < 0) page = 0;
        KolProfile kol = kolProfileService.getByUserId(SecurityUtils.currentUserId());
        Page<Booking> result = bookingRepository.findByKolProfileId(
                kol.getId(), PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return PageResponse.of(result.map(BookingMapper::toDto));
    }

    @Transactional
    public BookingMessageResponse sendMessage(Long bookingId, BookingMessageRequest req) {
        Booking b = requireBookingForEither(bookingId);
        BookingMessage m = BookingMessage.builder()
                .bookingId(b.getId())
                .senderUserId(SecurityUtils.currentUserId())
                .content(req.content())
                .attachmentUrl(req.attachmentUrl())
                .build();
        m = messageRepository.save(m);
        eventPublisher.publishEvent(new BookingMessageSentEvent(b.getId(), m.getSenderUserId()));
        return BookingMapper.toDto(m);
    }

    public PageResponse<BookingMessageResponse> listMessages(Long bookingId, int page, int size) {
        requireBookingForEither(bookingId);
        if (size <= 0 || size > 200) size = 50;
        if (page < 0) page = 0;
        Page<BookingMessage> result = messageRepository.findByBookingIdOrderByCreatedAtAsc(
                bookingId, PageRequest.of(page, size));
        return PageResponse.of(result.map(BookingMapper::toDto));
    }

    @Transactional
    public void transition(Booking b, BookingStatus target, String note) {
        BookingStatus prev = b.getStatus();
        BookingStateMachine.ensureTransition(prev, target);
        b.setStatus(target);
        bookingRepository.save(b);
        recordHistory(b.getId(), prev, target, SecurityUtils.currentUserIdSafe(), note);
        eventPublisher.publishEvent(new BookingStatusChangedEvent(
                b.getId(), prev, target, SecurityUtils.currentUserIdSafe()));
        log.info("Booking {} transitioned {} -> {}", b.getId(), prev, target);
    }

    private void recordHistory(Long bookingId, BookingStatus from, BookingStatus to, Long userId, String note) {
        historyRepository.save(BookingStatusHistory.builder()
                .bookingId(bookingId)
                .fromStatus(from)
                .toStatus(to)
                .changedByUser(userId)
                .note(note)
                .build());
    }

    private Booking requireBookingFor(Long bookingId, Role role) {
        Booking b = bookingRepository.findById(bookingId)
                .orElseThrow(() -> ResourceNotFoundException.of("Booking", bookingId));
        Long userId = SecurityUtils.currentUserId();
        if (role == Role.BRAND) {
            BrandProfile brand = brandProfileService.getByUserId(userId);
            if (!brand.getId().equals(b.getBrandProfileId())) {
                throw new BusinessException("Not your booking", ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
            }
        } else if (role == Role.KOL) {
            KolProfile kol = kolProfileService.getByUserId(userId);
            if (!kol.getId().equals(b.getKolProfileId())) {
                throw new BusinessException("Not your booking", ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
            }
        }
        return b;
    }

    private Booking requireBookingForEither(Long bookingId) {
        Booking b = bookingRepository.findById(bookingId)
                .orElseThrow(() -> ResourceNotFoundException.of("Booking", bookingId));
        Long userId = SecurityUtils.currentUserId();
        Role role = SecurityUtils.currentRole();
        if (role == Role.ADMIN) return b;
        if (role == Role.BRAND) {
            BrandProfile brand = brandProfileService.getByUserId(userId);
            if (brand.getId().equals(b.getBrandProfileId())) return b;
        } else if (role == Role.KOL) {
            KolProfile kol = kolProfileService.getByUserId(userId);
            if (kol.getId().equals(b.getKolProfileId())) return b;
        }
        throw new BusinessException("Not your booking", ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
    }
}
