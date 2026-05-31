package kolbooking.datn.auth.controller;

import jakarta.validation.Valid;
import kolbooking.datn.auth.dto.DeactivateAccountRequest;
import kolbooking.datn.auth.dto.DeleteAccountRequest;
import kolbooking.datn.auth.dto.MeResponse;
import kolbooking.datn.auth.service.UserSelfService;
import kolbooking.datn.common.dto.ApiResponse;
import kolbooking.datn.common.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users/me")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class UserSelfController {

    private final UserSelfService userSelfService;

    @GetMapping
    public ApiResponse<MeResponse> me() {
        return ApiResponse.ok(userSelfService.me(SecurityUtils.currentUserId()));
    }

    @PatchMapping("/deactivate")
    public ApiResponse<MeResponse> deactivate(@Valid @RequestBody DeactivateAccountRequest request) {
        return ApiResponse.ok(
                userSelfService.deactivate(SecurityUtils.currentUserId(), request),
                "Account deactivated");
    }

    @DeleteMapping
    public ApiResponse<Void> delete(@Valid @RequestBody DeleteAccountRequest request) {
        userSelfService.delete(SecurityUtils.currentUserId(), request);
        return ApiResponse.ok("Account deleted");
    }
}
