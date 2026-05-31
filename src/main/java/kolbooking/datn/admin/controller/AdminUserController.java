package kolbooking.datn.admin.controller;

import kolbooking.datn.admin.dto.AdminUserResponse;
import kolbooking.datn.admin.service.AdminUserService;
import kolbooking.datn.auth.domain.AppUser;
import kolbooking.datn.auth.domain.Role;
import kolbooking.datn.common.dto.ApiResponse;
import kolbooking.datn.common.dto.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final AdminUserService adminUserService;

    @GetMapping
    public ApiResponse<PageResponse<AdminUserResponse>> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Role role,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(PageResponse.of(adminUserService.search(q, role, PageRequest.of(page, size))
                .map(this::toDto)));
    }

    @PostMapping("/{id}/ban")
    public ApiResponse<AdminUserResponse> ban(@PathVariable Long id) {
        return ApiResponse.ok(toDto(adminUserService.ban(id)));
    }

    @PostMapping("/{id}/unban")
    public ApiResponse<AdminUserResponse> unban(@PathVariable Long id) {
        return ApiResponse.ok(toDto(adminUserService.unban(id)));
    }

    private AdminUserResponse toDto(AppUser u) {
        return new AdminUserResponse(u.getId(), u.getEmail(), u.getRole(), u.getStatus(),
                u.isEmailVerified(), u.getCreatedAt());
    }
}
