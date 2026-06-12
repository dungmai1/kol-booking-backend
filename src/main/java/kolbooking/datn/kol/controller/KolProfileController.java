package kolbooking.datn.kol.controller;

import jakarta.validation.Valid;
import kolbooking.datn.common.dto.ApiResponse;
import kolbooking.datn.kol.dto.KolPortfolioItemRequest;
import kolbooking.datn.kol.dto.KolPortfolioItemResponse;
import kolbooking.datn.kol.dto.KolPricingPackageRequest;
import kolbooking.datn.kol.dto.KolPricingPackageResponse;
import kolbooking.datn.kol.dto.KolProfileResponse;
import kolbooking.datn.kol.dto.KolProfileUpdateRequest;
import kolbooking.datn.kol.dto.KolPublicResponse;
import kolbooking.datn.kol.dto.KolSocialChannelRequest;
import kolbooking.datn.kol.dto.KolSocialChannelResponse;
import kolbooking.datn.kol.service.KolProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/kols")
@RequiredArgsConstructor
public class KolProfileController {

    private final KolProfileService kolProfileService;

    @GetMapping("/me")
    @PreAuthorize("hasRole('KOL')")
    public ApiResponse<KolProfileResponse> getMe() {
        return ApiResponse.ok(kolProfileService.getMyProfile());
    }

    @GetMapping("/me/channels")
    @PreAuthorize("hasRole('KOL')")
    public ApiResponse<List<KolSocialChannelResponse>> listChannels() {
        return ApiResponse.ok(kolProfileService.listMyChannels());
    }

    @GetMapping("/me/packages")
    @PreAuthorize("hasRole('KOL')")
    public ApiResponse<List<KolPricingPackageResponse>> listPackages() {
        return ApiResponse.ok(kolProfileService.listMyPackages());
    }

    @GetMapping("/me/portfolio")
    @PreAuthorize("hasRole('KOL')")
    public ApiResponse<List<KolPortfolioItemResponse>> listPortfolio() {
        return ApiResponse.ok(kolProfileService.listMyPortfolio());
    }

    @PutMapping("/me")
    @PreAuthorize("hasRole('KOL')")
    public ApiResponse<KolProfileResponse> updateMe(@Valid @RequestBody KolProfileUpdateRequest req) {
        return ApiResponse.ok(kolProfileService.updateMyProfile(req));
    }

    @PostMapping("/me/submit")
    @PreAuthorize("hasRole('KOL')")
    public ApiResponse<KolProfileResponse> submit() {
        return ApiResponse.ok(kolProfileService.submitForReview());
    }

    @PostMapping("/me/channels")
    @PreAuthorize("hasRole('KOL')")
    public ApiResponse<KolSocialChannelResponse> addChannel(@Valid @RequestBody KolSocialChannelRequest req) {
        return ApiResponse.ok(kolProfileService.addChannel(req));
    }

    @DeleteMapping("/me/channels/{id}")
    @PreAuthorize("hasRole('KOL')")
    public ApiResponse<Void> deleteChannel(@PathVariable("id") Long id) {
        kolProfileService.deleteChannel(id);
        return ApiResponse.ok("Channel deleted");
    }

    @PostMapping("/me/packages")
    @PreAuthorize("hasRole('KOL')")
    public ApiResponse<KolPricingPackageResponse> addPackage(@Valid @RequestBody KolPricingPackageRequest req) {
        return ApiResponse.ok(kolProfileService.addPackage(req));
    }

    @DeleteMapping("/me/packages/{id}")
    @PreAuthorize("hasRole('KOL')")
    public ApiResponse<Void> deletePackage(@PathVariable("id") Long id) {
        kolProfileService.deletePackage(id);
        return ApiResponse.ok("Package deleted");
    }

    @PostMapping("/me/portfolio")
    @PreAuthorize("hasRole('KOL')")
    public ApiResponse<KolPortfolioItemResponse> addPortfolio(@Valid @RequestBody KolPortfolioItemRequest req) {
        return ApiResponse.ok(kolProfileService.addPortfolio(req));
    }

    @DeleteMapping("/me/portfolio/{id}")
    @PreAuthorize("hasRole('KOL')")
    public ApiResponse<Void> deletePortfolio(@PathVariable("id") Long id) {
        kolProfileService.deletePortfolio(id);
        return ApiResponse.ok("Portfolio item deleted");
    }

    @GetMapping("/{identifier}")
    public ApiResponse<KolPublicResponse> getPublic(@PathVariable("identifier") String identifier) {
        return ApiResponse.ok(kolProfileService.getPublic(identifier));
    }
}
