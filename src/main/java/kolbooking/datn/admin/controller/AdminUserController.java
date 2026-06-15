package kolbooking.datn.admin.controller;

import kolbooking.datn.admin.dto.AdminUserResponse;
import kolbooking.datn.admin.service.AdminUserService;
import kolbooking.datn.auth.domain.AppUser;
import kolbooking.datn.auth.domain.Role;
import kolbooking.datn.auth.domain.UserStatus;
import kolbooking.datn.brand.repository.BrandProfileRepository;
import kolbooking.datn.common.dto.ApiResponse;
import kolbooking.datn.common.dto.PageResponse;
import kolbooking.datn.kol.repository.KolProfileRepository;
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
    private final KolProfileRepository kolProfileRepository;
    private final BrandProfileRepository brandProfileRepository;

    @GetMapping
    public ApiResponse<PageResponse<AdminUserResponse>> search(
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "role", required = false) Role role,
            @RequestParam(name = "status", required = false) UserStatus status,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        return ApiResponse.ok(PageResponse.of(adminUserService.search(q, role, status, PageRequest.of(page, size))
                .map(this::toDto)));
    }

    @PostMapping("/{id}/ban")
    public ApiResponse<AdminUserResponse> ban(@PathVariable("id") Long id) {
        return ApiResponse.ok(toDto(adminUserService.ban(id)));
    }

    @PostMapping("/{id}/unban")
    public ApiResponse<AdminUserResponse> unban(@PathVariable("id") Long id) {
        return ApiResponse.ok(toDto(adminUserService.unban(id)));
    }

    private AdminUserResponse toDto(AppUser u) {
        String profileDisplayName = null;
        String kolSlug = null;
        Long brandProfileId = null;

        if (u.getRole() == Role.KOL) {
            var kol = kolProfileRepository.findByUserId(u.getId());
            if (kol.isPresent()) {
                profileDisplayName = kol.get().getDisplayName();
                kolSlug = kol.get().getSlug();
            }
        } else if (u.getRole() == Role.BRAND) {
            var brand = brandProfileRepository.findByUserId(u.getId());
            if (brand.isPresent()) {
                profileDisplayName = brand.get().getCompanyName();
                brandProfileId = brand.get().getId();
            }
        }

        return new AdminUserResponse(
                u.getId(),
                u.getEmail(),
                u.getRole(),
                u.getStatus(),
                u.isEmailVerified(),
                u.getCreatedAt(),
                profileDisplayName,
                kolSlug,
                brandProfileId
        );
    }
}
