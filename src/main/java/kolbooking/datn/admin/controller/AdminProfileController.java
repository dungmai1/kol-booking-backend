package kolbooking.datn.admin.controller;

import jakarta.validation.Valid;
import kolbooking.datn.admin.dto.RejectReasonRequest;
import kolbooking.datn.admin.service.AdminProfileService;
import kolbooking.datn.brand.domain.BrandProfileStatus;
import kolbooking.datn.common.dto.ApiResponse;
import kolbooking.datn.common.dto.PageResponse;
import kolbooking.datn.kol.domain.KolProfileStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminProfileController {

    private final AdminProfileService adminProfileService;

    @GetMapping("/kols")
    public ApiResponse<PageResponse<Map<String, Object>>> listKols(
            @RequestParam(name = "status", required = false) KolProfileStatus status,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        return ApiResponse.ok(PageResponse.of(adminProfileService
                .listKolByStatus(status, PageRequest.of(page, size))
                .map(k -> Map.of(
                        "id", k.getId(),
                        "userId", k.getUserId(),
                        "displayName", k.getDisplayName(),
                        "slug", k.getSlug(),
                        "status", k.getStatus(),
                        "avgRating", k.getAvgRating(),
                        "reviewCount", k.getReviewCount(),
                        "rejectReason", k.getRejectReason() == null ? "" : k.getRejectReason(),
                        "createdAt", k.getCreatedAt()
                ))));
    }

    @PostMapping("/kols/{id}/approve")
    public ApiResponse<Void> approveKol(@PathVariable("id") Long id) {
        adminProfileService.approveKol(id);
        return ApiResponse.ok("KOL approved");
    }

    @PostMapping("/kols/{id}/reject")
    public ApiResponse<Void> rejectKol(@PathVariable("id") Long id,
                                       @Valid @RequestBody(required = false) RejectReasonRequest request) {
        adminProfileService.rejectKol(id, request == null ? null : request.reason());
        return ApiResponse.ok("KOL rejected");
    }

    @GetMapping("/brands")
    public ApiResponse<PageResponse<Map<String, Object>>> listBrands(
            @RequestParam(name = "status", required = false) BrandProfileStatus status,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        return ApiResponse.ok(PageResponse.of(adminProfileService
                .listBrandByStatus(status, PageRequest.of(page, size))
                .map(b -> Map.of(
                        "id", b.getId(),
                        "userId", b.getUserId(),
                        "companyName", b.getCompanyName(),
                        "industry", b.getIndustry() == null ? "" : b.getIndustry(),
                        "status", b.getStatus(),
                        "rejectReason", b.getRejectReason() == null ? "" : b.getRejectReason(),
                        "createdAt", b.getCreatedAt()
                ))));
    }

    @PostMapping("/brands/{id}/approve")
    public ApiResponse<Void> approveBrand(@PathVariable("id") Long id) {
        adminProfileService.approveBrand(id);
        return ApiResponse.ok("Brand approved");
    }

    @PostMapping("/brands/{id}/reject")
    public ApiResponse<Void> rejectBrand(@PathVariable("id") Long id,
                                         @Valid @RequestBody(required = false) RejectReasonRequest request) {
        adminProfileService.rejectBrand(id, request == null ? null : request.reason());
        return ApiResponse.ok("Brand rejected");
    }
}
