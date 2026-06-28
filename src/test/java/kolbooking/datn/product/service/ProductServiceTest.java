package kolbooking.datn.product.service;

import kolbooking.datn.auth.domain.AppUser;
import kolbooking.datn.auth.domain.Role;
import kolbooking.datn.auth.domain.UserStatus;
import kolbooking.datn.auth.security.AppUserPrincipal;
import kolbooking.datn.brand.domain.BrandProfile;
import kolbooking.datn.brand.repository.BrandProfileRepository;
import kolbooking.datn.brand.service.BrandProfileService;
import kolbooking.datn.common.exception.BusinessException;
import kolbooking.datn.common.exception.ErrorCode;
import kolbooking.datn.common.exception.ResourceNotFoundException;
import kolbooking.datn.common.repository.CategoryRepository;
import kolbooking.datn.kol.repository.KolProfileRepository;
import kolbooking.datn.product.domain.Product;
import kolbooking.datn.product.domain.ProductStatus;
import kolbooking.datn.product.repository.ProductApplicationRepository;
import kolbooking.datn.product.repository.ProductRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock ProductRepository productRepository;
    @Mock ProductApplicationRepository applicationRepository;
    @Mock BrandProfileRepository brandProfileRepository;
    @Mock BrandProfileService brandProfileService;
    @Mock CategoryRepository categoryRepository;
    @Mock KolProfileRepository kolProfileRepository;

    @InjectMocks ProductService productService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void requireOwnedProduct_returns404OnlyWhenProductDoesNotExist() {
        setAuthentication(100L, Role.BRAND);
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> productService.requireOwnedProduct(99L));
    }

    @Test
    void requireOwnedProduct_returns403WhenBrandDoesNotOwnProduct() {
        setAuthentication(100L, Role.BRAND);
        when(productRepository.findById(9L)).thenReturn(Optional.of(productOwnedBy(10L)));
        when(brandProfileRepository.findByUserId(100L))
                .thenReturn(Optional.of(BrandProfile.builder().id(11L).userId(100L).companyName("Other Brand").build()));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> productService.requireOwnedProduct(9L));

        assertEquals(ErrorCode.FORBIDDEN, ex.getErrorCode());
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    }

    @Test
    void requireOwnedProduct_returns403WhenCurrentBrandProfileIsMissing() {
        setAuthentication(100L, Role.BRAND);
        when(productRepository.findById(9L)).thenReturn(Optional.of(productOwnedBy(10L)));
        when(brandProfileRepository.findByUserId(100L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> productService.requireOwnedProduct(9L));

        assertEquals(ErrorCode.FORBIDDEN, ex.getErrorCode());
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    }

    private static Product productOwnedBy(Long brandProfileId) {
        return Product.builder()
                .id(9L)
                .brandProfileId(brandProfileId)
                .title("Product campaign")
                .status(ProductStatus.OPEN)
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
