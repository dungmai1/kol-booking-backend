package kolbooking.datn.booking.service;

import kolbooking.datn.auth.domain.AppUser;
import kolbooking.datn.auth.domain.Role;
import kolbooking.datn.auth.domain.UserStatus;
import kolbooking.datn.auth.security.AppUserPrincipal;
import kolbooking.datn.booking.domain.Booking;
import kolbooking.datn.booking.domain.BookingStatus;
import kolbooking.datn.booking.dto.BookingResponse;
import kolbooking.datn.booking.dto.CreateBookingRequest;
import kolbooking.datn.booking.dto.ReasonRequest;
import kolbooking.datn.booking.event.BookingStatusChangedEvent;
import kolbooking.datn.booking.repository.BookingDeliverableRepository;
import kolbooking.datn.booking.repository.BookingMessageRepository;
import kolbooking.datn.booking.repository.BookingRepository;
import kolbooking.datn.booking.repository.BookingStatusHistoryRepository;
import kolbooking.datn.brand.domain.BrandProfile;
import kolbooking.datn.brand.service.BrandProfileService;
import kolbooking.datn.common.exception.BusinessException;
import kolbooking.datn.kol.domain.KolProfile;
import kolbooking.datn.kol.service.KolProfileService;
import kolbooking.datn.payment.domain.PaymentOrder;
import kolbooking.datn.payment.domain.PaymentOrderStatus;
import kolbooking.datn.payment.repository.PaymentOrderRepository;
import kolbooking.datn.payment.service.WalletService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock BookingRepository bookingRepository;
    @Mock BookingStatusHistoryRepository historyRepository;
    @Mock BookingMessageRepository messageRepository;
    @Mock BookingDeliverableRepository deliverableRepository;
    @Mock KolProfileService kolProfileService;
    @Mock BrandProfileService brandProfileService;
    @Mock WalletService walletService;
    @Mock ApplicationEventPublisher eventPublisher;
    @Mock PaymentOrderRepository paymentOrderRepository;

    @InjectMocks BookingService bookingService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(bookingService, "platformFeePercent", BigDecimal.TEN);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createBooking_persistsAndReturnsAttachmentUrl() {
        setAuthentication(100L, Role.BRAND);
        BrandProfile brand = BrandProfile.builder().id(10L).userId(100L).companyName("Demo Brand").build();
        KolProfile kol = KolProfile.builder().id(20L).userId(200L).displayName("Demo KOL").build();
        CreateBookingRequest request = new CreateBookingRequest(
                20L,
                "Summer Launch",
                "Campaign brief with enough detail",
                "One TikTok video",
                new BigDecimal("5000000"),
                LocalDate.now().plusDays(1),
                LocalDate.now().plusDays(7),
                "/uploads/2026/06/brief.pdf");

        when(brandProfileService.getCurrentBrandProfile()).thenReturn(brand);
        when(kolProfileService.requireApprovedById(20L)).thenReturn(kol);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> {
            Booking saved = invocation.getArgument(0);
            saved.setId(394L);
            return saved;
        });

        BookingResponse response = bookingService.createBooking(request);

        ArgumentCaptor<Booking> bookingCaptor = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository).save(bookingCaptor.capture());
        assertEquals("/uploads/2026/06/brief.pdf", bookingCaptor.getValue().getAttachmentUrl());
        assertEquals("/uploads/2026/06/brief.pdf", response.attachmentUrl());
    }

    @Test
    void cancelByBrand_allowsAcceptedBookingWithoutPaidOrderAndCancelsPendingPaymentOrder() {
        setAuthentication(100L, Role.BRAND);
        Booking booking = acceptedBooking();
        PaymentOrder pendingOrder = PaymentOrder.builder()
                .id(77L)
                .bookingId(394L)
                .status(PaymentOrderStatus.PENDING)
                .build();

        when(bookingRepository.findById(394L)).thenReturn(Optional.of(booking));
        when(brandProfileService.getByUserId(100L))
                .thenReturn(BrandProfile.builder().id(10L).userId(100L).companyName("Demo Brand").build());
        when(paymentOrderRepository.findFirstByBookingIdOrderByCreatedAtDesc(394L))
                .thenReturn(Optional.of(pendingOrder));
        when(bookingRepository.findByIdForUpdate(394L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BookingResponse response = bookingService.cancelByBrand(394L, new ReasonRequest("Brand changed plan"));

        assertEquals(BookingStatus.CANCELLED, response.status());
        assertEquals("Brand changed plan", response.cancelReason());
        assertEquals(PaymentOrderStatus.CANCELLED, pendingOrder.getStatus());
        verify(paymentOrderRepository).save(pendingOrder);
        verify(eventPublisher).publishEvent(any(BookingStatusChangedEvent.class));
    }

    @Test
    void cancelByBrand_rejectsAcceptedBookingWithPaidOrder() {
        setAuthentication(100L, Role.BRAND);
        Booking booking = acceptedBooking();
        PaymentOrder paidOrder = PaymentOrder.builder()
                .id(77L)
                .bookingId(394L)
                .status(PaymentOrderStatus.PAID)
                .build();

        when(bookingRepository.findById(394L)).thenReturn(Optional.of(booking));
        when(brandProfileService.getByUserId(100L))
                .thenReturn(BrandProfile.builder().id(10L).userId(100L).companyName("Demo Brand").build());
        when(paymentOrderRepository.findFirstByBookingIdOrderByCreatedAtDesc(394L))
                .thenReturn(Optional.of(paidOrder));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> bookingService.cancelByBrand(394L, new ReasonRequest("Brand changed plan")));

        assertEquals("Booking đã được thanh toán, không thể hủy bằng luồng Brand thông thường", ex.getMessage());
        verify(bookingRepository, never()).findByIdForUpdate(any());
    }

    private static Booking acceptedBooking() {
        return Booking.builder()
                .id(394L)
                .brandProfileId(10L)
                .brandCompanyName("Demo Brand")
                .kolProfileId(20L)
                .kolDisplayName("Demo KOL")
                .campaignTitle("Summer Launch")
                .budget(new BigDecimal("5000000"))
                .platformFeePercent(BigDecimal.TEN)
                .status(BookingStatus.ACCEPTED)
                .build();
    }

    private static void setAuthentication(Long userId, Role role) {
        AppUser user = AppUser.builder()
                .id(userId)
                .email(role.name().toLowerCase() + userId + "@example.com")
                .passwordHash("pw")
                .role(role)
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .build();
        AppUserPrincipal principal = new AppUserPrincipal(user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }
}
