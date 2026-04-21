package kolbooking.datn.brand.controller;

import jakarta.validation.Valid;
import kolbooking.datn.brand.dto.BrandProfileResponse;
import kolbooking.datn.brand.dto.BrandProfileUpdateRequest;
import kolbooking.datn.brand.service.BrandProfileService;
import kolbooking.datn.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/brands")
@RequiredArgsConstructor
public class BrandProfileController {

    private final BrandProfileService brandProfileService;

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
}
