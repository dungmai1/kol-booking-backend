package kolbooking.datn.product.controller;

import jakarta.validation.Valid;
import kolbooking.datn.common.dto.ApiResponse;
import kolbooking.datn.common.dto.PageResponse;
import kolbooking.datn.kol.domain.Platform;
import kolbooking.datn.product.domain.ApplicationStatus;
import kolbooking.datn.product.dto.ProductApplicationCreateRequest;
import kolbooking.datn.product.dto.ProductApplicationResponse;
import kolbooking.datn.product.dto.ProductCreateRequest;
import kolbooking.datn.product.dto.ProductResponse;
import kolbooking.datn.product.dto.ProductSearchFilter;
import kolbooking.datn.product.dto.ProductUpdateRequest;
import kolbooking.datn.product.service.ProductApplicationService;
import kolbooking.datn.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final ProductApplicationService applicationService;

    // ---- Brand: manage own postings ----

    @PostMapping
    @PreAuthorize("hasRole('BRAND')")
    public ResponseEntity<ApiResponse<ProductResponse>> create(@Valid @RequestBody ProductCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(productService.create(request), "Đăng sản phẩm thành công"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('BRAND')")
    public ApiResponse<ProductResponse> update(@PathVariable("id") Long id,
                                               @Valid @RequestBody ProductUpdateRequest request) {
        return ApiResponse.ok(productService.update(id, request));
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("hasRole('BRAND')")
    public ApiResponse<ProductResponse> close(@PathVariable("id") Long id) {
        return ApiResponse.ok(productService.close(id), "Đã đóng sản phẩm");
    }

    @PostMapping("/{id}/reopen")
    @PreAuthorize("hasRole('BRAND')")
    public ApiResponse<ProductResponse> reopen(@PathVariable("id") Long id) {
        return ApiResponse.ok(productService.reopen(id), "Đã mở lại sản phẩm");
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('BRAND')")
    public ApiResponse<Void> delete(@PathVariable("id") Long id) {
        productService.delete(id);
        return ApiResponse.ok("Đã xoá sản phẩm");
    }

    @GetMapping("/mine")
    @PreAuthorize("hasRole('BRAND')")
    public ApiResponse<PageResponse<ProductResponse>> listMine(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        return ApiResponse.ok(productService.listMine(page, size));
    }

    // ---- Public browse ----

    @GetMapping
    public ApiResponse<PageResponse<ProductResponse>> browse(
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "categoryId", required = false) Long categoryId,
            @RequestParam(name = "platform", required = false) Platform platform,
            @RequestParam(name = "minBudget", required = false) BigDecimal minBudget,
            @RequestParam(name = "maxBudget", required = false) BigDecimal maxBudget,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        ProductSearchFilter filter = new ProductSearchFilter(q, categoryId, platform, minBudget, maxBudget);
        return ApiResponse.ok(productService.browse(filter, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<ProductResponse> getOne(@PathVariable("id") Long id) {
        return ApiResponse.ok(productService.getPublic(id));
    }

    // ---- KOL: apply ----

    @PostMapping("/{id}/applications")
    @PreAuthorize("hasRole('KOL')")
    public ResponseEntity<ApiResponse<ProductApplicationResponse>> apply(
            @PathVariable("id") Long id,
            @Valid @RequestBody(required = false) ProductApplicationCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(applicationService.apply(id, request), "Ứng tuyển thành công"));
    }

    // ---- Brand: review applicants ----

    @GetMapping("/{id}/applications")
    @PreAuthorize("hasRole('BRAND')")
    public ApiResponse<PageResponse<ProductApplicationResponse>> listApplicants(
            @PathVariable("id") Long id,
            @RequestParam(name = "status", required = false) ApplicationStatus status,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        return ApiResponse.ok(applicationService.listForProduct(id, status, page, size));
    }

    @GetMapping("/{id}/applications/top")
    @PreAuthorize("hasRole('BRAND')")
    public ApiResponse<List<ProductApplicationResponse>> topApplicants(
            @PathVariable("id") Long id,
            @RequestParam(name = "by", defaultValue = "rating") String by,
            @RequestParam(name = "limit", defaultValue = "5") int limit) {
        return ApiResponse.ok(applicationService.topApplicants(id, by, limit));
    }
}
