package kolbooking.datn.auth.service;

import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

/**
 * Transactional email dispatcher. All public methods are {@link Async} so a slow or failing
 * mail provider never blocks (or fails) the HTTP request that triggered the email.
 *
 * <p>Default provider is {@code smtp} ({@link JavaMailSender} via {@code spring.mail.*}).
 * Set {@code app.mail.provider=resend} to send through {@link ResendEmailClient} instead.
 *
 * <p>In {@code dev-mode} (or when the active provider is not configured) the message is logged
 * instead of sent, so local flows still surface the verification/reset link.
 */
@Slf4j
@Service
public class EmailService {

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final ResendEmailClient resendEmailClient;

    public EmailService(ObjectProvider<JavaMailSender> mailSenderProvider, ResendEmailClient resendEmailClient) {
        this.mailSenderProvider = mailSenderProvider;
        this.resendEmailClient = resendEmailClient;
    }

    @Value("${app.mail.provider:smtp}")
    private String provider;

    @Value("${app.mail.dev-mode:true}")
    private boolean devMode;

    @Value("${app.mail.app-url:http://localhost:8080}")
    private String appUrl;

    @Value("${app.mail.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    @Value("${app.mail.from:no-reply@kolbooking.local}")
    private String from;

    @Async
    public void sendEmailVerification(String to, String token) {
        String link = normalizedFrontendUrl() + "/auth/verify-email?token=" + token;
        String html = layout("Xác nhận địa chỉ email",
                """
                <p>Chào bạn,</p>
                <p>Cảm ơn bạn đã đăng ký <b>KOL Booking</b>. Vui lòng xác nhận địa chỉ email để
                kích hoạt đầy đủ tính năng của tài khoản.</p>
                """,
                "Xác nhận email", link,
                "Liên kết có hiệu lực trong 24 giờ. Nếu bạn không tạo tài khoản này, hãy bỏ qua email.");
        dispatch(to, "Xác nhận email - KOL Booking", html,
                () -> log.info("[DEV-MAIL] Email verification to={} link={}", to, link));
    }

    @Async
    public void sendPasswordReset(String to, String token) {
        String link = frontendUrl + "/auth/reset-password?token=" + token;
        String html = layout("Đặt lại mật khẩu",
                """
                <p>Chào bạn,</p>
                <p>Chúng tôi nhận được yêu cầu đặt lại mật khẩu cho tài khoản của bạn. Nhấn nút bên
                dưới để chọn mật khẩu mới.</p>
                """,
                "Đặt lại mật khẩu", link,
                "Liên kết có hiệu lực trong 2 giờ. Nếu bạn không yêu cầu, hãy bỏ qua email này.");
        dispatch(to, "Đặt lại mật khẩu - KOL Booking", html,
                () -> log.info("[DEV-MAIL] Password reset to={} link={}", to, link));
    }

    private String normalizedFrontendUrl() {
        return frontendUrl.endsWith("/") ? frontendUrl.substring(0, frontendUrl.length() - 1) : frontendUrl;
    }

    private void dispatch(String to, String subject, String html, Runnable devFallback) {
        if (devMode) {
            devFallback.run();
            return;
        }
        if ("resend".equalsIgnoreCase(provider)) {
            sendViaResend(to, subject, html, devFallback);
        } else {
            sendViaSmtp(to, subject, html, devFallback);
        }
    }

    private void sendViaSmtp(String to, String subject, String html, Runnable devFallback) {
        JavaMailSender sender = mailSenderProvider.getIfAvailable();
        if (sender == null) {
            devFallback.run();
            return;
        }
        try {
            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            sender.send(message);
            log.info("Email sent via SMTP to={} subject={}", to, subject);
        } catch (Exception ex) {
            log.error("Failed to send email via SMTP to={} subject={}: {}", to, subject, ex.getMessage());
        }
    }

    private void sendViaResend(String to, String subject, String html, Runnable devFallback) {
        if (!resendEmailClient.isConfigured()) {
            devFallback.run();
            return;
        }
        try {
            resendEmailClient.send(from, to, subject, html);
            log.info("Email sent via Resend to={} subject={}", to, subject);
        } catch (Exception ex) {
            String message = ex.getMessage();
            if (message != null && message.contains("domain is not verified")) {
                log.error(
                        "Failed to send email via Resend to={} subject={}: domain chưa xác minh. "
                                + "Thêm và xác minh domain tại https://resend.com/domains. Chi tiết: {}",
                        to, subject, message);
            } else if (message != null && message.contains("only send testing emails to your own email address")) {
                log.error(
                        "Failed to send email via Resend to={} subject={}: sender test chỉ gửi được tới "
                                + "email chủ tài khoản Resend. Chi tiết: {}",
                        to, subject, message);
            } else {
                log.error("Failed to send email via Resend to={} subject={}: {}", to, subject, message);
            }
        }
    }

    private String layout(String heading, String bodyHtml, String ctaLabel, String ctaLink, String footerNote) {
        String button = (ctaLabel == null || ctaLink == null) ? "" : """
                <p style="text-align:center;margin:28px 0;">
                  <a href="%s" style="background:#4f46e5;color:#ffffff;text-decoration:none;
                     padding:12px 28px;border-radius:8px;display:inline-block;font-weight:600;">%s</a>
                </p>
                <p style="font-size:12px;color:#6b7280;word-break:break-all;">Hoặc mở liên kết: %s</p>
                """.formatted(escape(ctaLink), escape(ctaLabel), escape(ctaLink));
        String footer = footerNote == null ? "" :
                "<p style=\"font-size:12px;color:#9ca3af;\">" + escape(footerNote) + "</p>";
        return """
                <div style="font-family:Arial,Helvetica,sans-serif;max-width:560px;margin:0 auto;
                     padding:24px;color:#111827;">
                  <h2 style="color:#4f46e5;">KOL Booking</h2>
                  <h3>%s</h3>
                  %s
                  %s
                  %s
                  <hr style="border:none;border-top:1px solid #e5e7eb;margin:24px 0;">
                  <p style="font-size:12px;color:#9ca3af;">© KOL Booking. Email tự động, vui lòng không trả lời.</p>
                </div>
                """.formatted(escape(heading), bodyHtml, button, footer);
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
