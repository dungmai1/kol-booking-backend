package kolbooking.datn.auth.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class EmailService {

    @Value("${app.mail.dev-mode:true}")
    private boolean devMode;

    @Value("${app.mail.app-url}")
    private String appUrl;

    @Async
    public void sendEmailVerification(String to, String token) {
        String link = appUrl + "/api/v1/auth/verify-email?token=" + token;
        if (devMode) {
            log.info("[DEV-MAIL] Email verification to={} link={}", to, link);
            return;
        }
        // TODO: wire real SMTP via JavaMailSender in prod
        log.info("Sending verification email to {}", to);
    }

    @Async
    public void sendPasswordReset(String to, String token) {
        String link = appUrl + "/api/v1/auth/reset-password?token=" + token;
        if (devMode) {
            log.info("[DEV-MAIL] Password reset to={} link={}", to, link);
            return;
        }
        log.info("Sending password reset email to {}", to);
    }

    @Async
    public void sendNotification(String to, String subject, String body, String link) {
        if (to == null) return;
        if (devMode) {
            log.info("[DEV-MAIL] Notification to={} subject={} link={}", to, subject, link);
            return;
        }
        // TODO: wire real SMTP via JavaMailSender in prod
        log.info("Sending notification email to {}", to);
    }
}
