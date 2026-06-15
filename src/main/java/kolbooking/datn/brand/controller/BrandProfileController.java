package kolbooking.datn.brand.controller;

import jakarta.validation.Valid;
import kolbooking.datn.brand.dto.BrandProfileResponse;
import kolbooking.datn.brand.dto.BrandProfileUpdateRequest;
import kolbooking.datn.brand.dto.BrandPublicResponse;
import kolbooking.datn.brand.service.BrandProfileService;
import kolbooking.datn.common.dto.ApiResponse;
import kolbooking.datn.common.dto.PageResponse;
import kolbooking.datn.product.dto.ProductResponse;
import kolbooking.datn.product.dto.ProductSearchFilter;
import kolbooking.datn.product.service.ProductService;
import lombok.RequiredArgsConstructor;
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
@RequestMapping("/api/v1/brands")
@RequiredArgsConstructor
public class BrandProfileController {

    private final BrandProfileService brandProfileService;
    private final ProductService productService;

    @GetMapping("/me")
    @PreAuthorize("hasRole('BRAND')")
    public ApiResponse<BrandProfileResponse> getMe() {
        return ApiResponse.ok(brandProfileService.getMyProfile());
    }

    @PutMapping("/me")
    @PreAuthorize("hasRole('BRAND')")
    public ApiResponse<BrandProfileResponse> updateMe(@Valid @RequestBody BrandProfileUpdateRequest req) {
        return ApiResponse.ok(brandProfileService.updateMyProfile(req));
    }

    @PostMapping("/me/submit")
    @PreAuthorize("hasRole('BRAND')")
    public ApiResponse<BrandProfileResponse> submit() {
        return ApiResponse.ok(brandProfileService.submitForReview());
    }

    @GetMapping("/{id:\\d+}")
    public ApiResponse<BrandPublicResponse> getPublic(@PathVariable("id") Long id) {
        return ApiResponse.ok(brandProfileService.getPublicById(id));
    }

    @GetMapping("/{id:\\d+}/products")
    public ApiResponse<PageResponse<ProductResponse>> getPublicProducts(
            @PathVariable("id") Long id,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        brandProfileService.requirePublicBrand(id);
        ProductSearchFilter filter = new ProductSearchFilter(null, null, null, null, null, id);
        return ApiResponse.ok(productService.browse(filter, page, size));
    }
}
