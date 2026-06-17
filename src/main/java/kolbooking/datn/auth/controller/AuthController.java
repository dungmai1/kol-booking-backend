package kolbooking.datn.auth.controller;

import jakarta.validation.Valid;
import kolbooking.datn.auth.dto.AuthTokens;
import kolbooking.datn.auth.dto.ForgotPasswordRequest;
import kolbooking.datn.auth.dto.LoginRequest;
import kolbooking.datn.auth.dto.LogoutRequest;
import kolbooking.datn.auth.dto.RefreshRequest;
import kolbooking.datn.auth.dto.RegisterRequest;
import kolbooking.datn.auth.dto.ResendVerificationRequest;
import kolbooking.datn.auth.dto.ResetPasswordRequest;
import kolbooking.datn.auth.dto.VerifyEmailRequest;
import kolbooking.datn.auth.service.AuthService;
import kolbooking.datn.common.dto.ApiResponse;
import kolbooking.datn.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Value("${app.mail.frontend-url:http://localhost:3000}")
    private String frontendUrl;

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

    /**
     * GET variant so the link inside the verification email is directly clickable from a mail
     * client. Renders a small HTML page (success or error) instead of JSON.
     */
    @GetMapping(value = "/verify-email", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> verifyEmailViaLink(@RequestParam("token") String token) {
        try {
            authService.verifyEmail(new VerifyEmailRequest(token));
            return ResponseEntity.ok(resultPage(true,
                    "Email của bạn đã được xác nhận thành công. Bạn có thể đăng nhập và sử dụng đầy đủ tính năng."));
        } catch (BusinessException ex) {
            return ResponseEntity.status(ex.getStatus())
                    .contentType(MediaType.TEXT_HTML)
                    .body(resultPage(false, ex.getMessage()));
        }
    }

    @PostMapping("/resend-verification")
    public ApiResponse<Void> resendVerification(@Valid @RequestBody ResendVerificationRequest request) {
        authService.resendVerification(request.email());
        return ApiResponse.ok("If the email exists and is unverified, a new verification link has been sent");
    }

    private String resultPage(boolean ok, String message) {
        String color = ok ? "#16a34a" : "#dc2626";
        String title = ok ? "Xác nhận thành công" : "Xác nhận thất bại";
        return """
                <!doctype html><html lang="vi"><head><meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <title>%s</title></head>
                <body style="font-family:Arial,Helvetica,sans-serif;background:#f9fafb;margin:0;padding:48px;">
                  <div style="max-width:480px;margin:0 auto;background:#fff;border-radius:12px;
                       padding:32px;text-align:center;box-shadow:0 1px 3px rgba(0,0,0,.1);">
                    <h2 style="color:%s;">%s</h2>
                    <p style="color:#374151;">%s</p>
                    <a href="%s/auth/login" style="background:#4f46e5;color:#fff;text-decoration:none;
                       padding:12px 28px;border-radius:8px;display:inline-block;margin-top:16px;">Đăng nhập</a>
                  </div>
                </body></html>
                """.formatted(title, color, title, message, frontendUrl);
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
