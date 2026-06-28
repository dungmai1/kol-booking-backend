package kolbooking.datn.product.service;

import kolbooking.datn.auth.domain.AppUser;
import kolbooking.datn.auth.domain.Role;
import kolbooking.datn.auth.domain.UserStatus;
import kolbooking.datn.auth.security.AppUserPrincipal;
import kolbooking.datn.booking.service.BookingService;
import kolbooking.datn.brand.domain.BrandProfile;
import kolbooking.datn.brand.service.BrandProfileService;
import kolbooking.datn.common.dto.PageResponse;
import kolbooking.datn.common.exception.BusinessException;
import kolbooking.datn.kol.domain.KolProfile;
import kolbooking.datn.kol.domain.KolProfileStatus;
import kolbooking.datn.kol.repository.KolProfileRepository;
import kolbooking.datn.kol.service.KolProfileService;
import kolbooking.datn.notification.service.NotificationService;
import kolbooking.datn.product.domain.ApplicationStatus;
import kolbooking.datn.product.domain.Product;
import kolbooking.datn.product.domain.ProductApplication;
import kolbooking.datn.product.domain.ProductStatus;
import kolbooking.datn.product.dto.ProductApplicationCreateRequest;
import kolbooking.datn.product.dto.ProductApplicationResponse;
import kolbooking.datn.product.repository.ProductApplicationRepository;
import kolbooking.datn.product.repository.ProductRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductApplicationServiceTest {

    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    @Mock ProductRepository productRepository;
    @Mock ProductApplicationRepository applicationRepository;
    @Mock ProductService productService;
    @Mock KolProfileService kolProfileService;
    @Mock KolProfileRepository kolProfileRepository;
    @Mock BrandProfileService brandProfileService;
    @Mock BookingService bookingService;
    @Mock NotificationService notificationService;

    @InjectMocks ProductApplicationService applicationService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void apply_allowsOpenProductWhenDeadlineIsTodayInVietnam() {
        setAuthentication(200L, Role.KOL);
        KolProfile kol = approvedKol();
        Product product = openProduct(LocalDate.now(VIETNAM_ZONE));
        when(kolProfileService.getByUserId(200L)).thenReturn(kol);
        when(productRepository.findById(9L)).thenReturn(Optional.of(product));
        when(applicationRepository.findByProductIdAndKolProfileId(9L, 20L)).thenReturn(Optional.empty());
        when(applicationRepository.saveAndFlush(any(ProductApplication.class))).thenAnswer(invocation -> {
            ProductApplication saved = invocation.getArgument(0);
            saved.setId(91L);
            return saved;
        });
        when(brandProfileService.getById(10L))
                .thenReturn(BrandProfile.builder().id(10L).userId(100L).companyName("Demo Brand").build());

        ProductApplicationResponse response = applicationService.apply(
                9L, new ProductApplicationCreateRequest("I can do this campaign", new BigDecimal("3000000")));

        assertEquals(ApplicationStatus.PENDING, response.status());
        verify(productRepository).incrementApplicationCount(9L);
    }

    @Test
    void apply_allowsOpenProductWhenDeadlineIsNull() {
        setAuthentication(200L, Role.KOL);
        KolProfile kol = approvedKol();
        Product product = openProduct(null);
        when(kolProfileService.getByUserId(200L)).thenReturn(kol);
        when(productRepository.findById(9L)).thenReturn(Optional.of(product));
        when(applicationRepository.findByProductIdAndKolProfileId(9L, 20L)).thenReturn(Optional.empty());
        when(applicationRepository.saveAndFlush(any(ProductApplication.class))).thenAnswer(invocation -> {
            ProductApplication saved = invocation.getArgument(0);
            saved.setId(91L);
            return saved;
        });
        when(brandProfileService.getById(10L))
                .thenReturn(BrandProfile.builder().id(10L).userId(100L).companyName("Demo Brand").build());

        ProductApplicationResponse response = applicationService.apply(9L, null);

        assertEquals(ApplicationStatus.PENDING, response.status());
        verify(productRepository).incrementApplicationCount(9L);
    }

    @Test
    void apply_rejectsOpenProductWhenDeadlineHasPassed() {
        setAuthentication(200L, Role.KOL);
        KolProfile kol = approvedKol();
        Product product = openProduct(LocalDate.now(VIETNAM_ZONE).minusDays(1));
        when(kolProfileService.getByUserId(200L)).thenReturn(kol);
        when(productRepository.findById(9L)).thenReturn(Optional.of(product));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> applicationService.apply(9L, null));

        assertEquals("Chiến dịch đã quá hạn ứng tuyển", ex.getMessage());
        verify(applicationRepository, never()).saveAndFlush(any());
        verify(productRepository, never()).incrementApplicationCount(any());
    }

    @Test
    void listForProduct_returnsEmptyPageWhenProductExistsAndHasNoApplications() {
        setAuthentication(100L, Role.BRAND);
        Product product = openProduct(LocalDate.now(VIETNAM_ZONE).plusDays(1));
        when(productService.requireOwnedProduct(9L)).thenReturn(product);
        when(applicationRepository.findByProductId(eq(9L), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 12), 0));

        PageResponse<ProductApplicationResponse> response = applicationService.listForProduct(9L, null, 0, 12);

        assertTrue(response.content().isEmpty());
        assertEquals(0, response.totalElements());
        assertEquals(0, response.totalPages());
        assertEquals(0, response.currentPage());
        assertEquals(12, response.pageSize());
    }

    private static Product openProduct(LocalDate deadline) {
        return Product.builder()
                .id(9L)
                .brandProfileId(10L)
                .title("Product campaign")
                .description("Campaign description")
                .budget(new BigDecimal("3000000"))
                .slots(1)
                .status(ProductStatus.OPEN)
                .deadline(deadline)
                .applicationCount(0)
                .build();
    }

    private static KolProfile approvedKol() {
        return KolProfile.builder()
                .id(20L)
                .userId(200L)
                .displayName("Demo KOL")
                .slug("demo-kol")
                .status(KolProfileStatus.APPROVED)
                .avgRating(BigDecimal.ZERO)
                .reviewCount(0)
                .maxFollowerCount(0L)
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
