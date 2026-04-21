package kolbooking.datn.auth.controller;

import jakarta.validation.Valid;
import kolbooking.datn.auth.dto.AuthTokens;
import kolbooking.datn.auth.dto.ForgotPasswordRequest;
import kolbooking.datn.auth.dto.LoginRequest;
import kolbooking.datn.auth.dto.LogoutRequest;
import kolbooking.datn.auth.dto.RefreshRequest;
import kolbooking.datn.auth.dto.RegisterRequest;
import kolbooking.datn.auth.dto.ResetPasswordRequest;
import kolbooking.datn.auth.dto.VerifyEmailRequest;
import kolbooking.datn.auth.service.AuthService;
import kolbooking.datn.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthTokens>> register(@Valid @RequestBody RegisterRequest request) {
        AuthTokens tokens = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(tokens, "Registered. Check email to verify account."));
    }

    @PostMapping("/login")
    public ApiResponse<AuthTokens> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ApiResponse<AuthTokens> refresh(@Valid @RequestBody RefreshRequest request) {
        return ApiResponse.ok(authService.refresh(request));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@Valid @RequestBody LogoutRequest request) {
        authService.logout(request.refreshToken());
        return ApiResponse.ok("Logged out");
    }

    @PostMapping("/verify-email")
    public ApiResponse<Void> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        authService.verifyEmail(request);
        return ApiResponse.ok("Email verified");
    }

    @PostMapping("/forgot-password")
    public ApiResponse<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ApiResponse.ok("If the email exists, a reset link has been sent");
    }

    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ApiResponse.ok("Password reset successful");
    }
}
