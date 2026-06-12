package kolbooking.datn.product.controller;

import jakarta.validation.Valid;
import kolbooking.datn.common.dto.ApiResponse;
import kolbooking.datn.common.dto.PageResponse;
import kolbooking.datn.product.dto.ProductApplicationResponse;
import kolbooking.datn.product.dto.RejectApplicationRequest;
import kolbooking.datn.product.service.ProductApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/applications")
@RequiredArgsConstructor
public class ProductApplicationController {

    private final ProductApplicationService applicationService;

    // ---- KOL ----

    @GetMapping("/mine")
    @PreAuthorize("hasRole('KOL')")
    public ApiResponse<PageResponse<ProductApplicationResponse>> listMine(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        return ApiResponse.ok(applicationService.listMine(page, size));
    }

    @PostMapping("/{id}/withdraw")
    @PreAuthorize("hasRole('KOL')")
    public ApiResponse<ProductApplicationResponse> withdraw(@PathVariable("id") Long id) {
        return ApiResponse.ok(applicationService.withdraw(id), "Đã rút ứng tuyển");
    }

    // ---- Brand ----

    @PostMapping("/{id}/shortlist")
    @PreAuthorize("hasRole('BRAND')")
    public ApiResponse<ProductApplicationResponse> shortlist(@PathVariable("id") Long id) {
        return ApiResponse.ok(applicationService.shortlist(id), "Đã đưa vào shortlist");
    }

    @PostMapping("/{id}/accept")
    @PreAuthorize("hasRole('BRAND')")
    public ApiResponse<ProductApplicationResponse> accept(@PathVariable("id") Long id) {
        return ApiResponse.ok(applicationService.accept(id), "Đã duyệt ứng viên & tạo booking");
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('BRAND')")
    public ApiResponse<ProductApplicationResponse> reject(
            @PathVariable("id") Long id,
            @Valid @RequestBody(required = false) RejectApplicationRequest request) {
        String reason = request == null ? null : request.reason();
        return ApiResponse.ok(applicationService.reject(id, reason), "Đã từ chối ứng tuyển");
    }
}
