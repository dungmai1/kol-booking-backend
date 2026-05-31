package kolbooking.datn.admin.controller;

import kolbooking.datn.admin.domain.AdminAuditLog;
import kolbooking.datn.admin.repository.AdminAuditLogRepository;
import kolbooking.datn.common.dto.ApiResponse;
import kolbooking.datn.common.dto.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/audit-logs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminAuditLogController {

    private final AdminAuditLogRepository auditRepository;

    @GetMapping
    public ApiResponse<PageResponse<AdminAuditLog>> list(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "50") int size) {
        return ApiResponse.ok(PageResponse.of(
                auditRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size))));
    }
}
