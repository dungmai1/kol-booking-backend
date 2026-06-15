package kolbooking.datn.auth.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;

/**
 * Thin HTTP client for the <a href="https://resend.com/docs/api-reference/emails/send-email">Resend Emails API</a>.
 */
@Slf4j
@Component
public class ResendEmailClient {

    private static final String API_URL = "https://api.resend.com/emails";

    private final RestClient restClient;

    @Value("${app.mail.resend.api-key:}")
    private String apiKey;

    public ResendEmailClient(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    public void send(String from, String to, String subject, String html) {
        if (!isConfigured()) {
            throw new IllegalStateException("Resend API key is not configured");
        }

        SendEmailRequest body = new SendEmailRequest(from, List.of(to), subject, html);

        try {
            SendEmailResponse response = restClient.post()
                    .uri(API_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + apiKey)
                    .body(body)
                    .retrieve()
                    .body(SendEmailResponse.class);

            log.debug("Resend accepted email to={} id={}", to, response == null ? null : response.id());
        } catch (RestClientResponseException ex) {
            throw new IllegalStateException(
                    "Resend API error status=" + ex.getStatusCode().value() + " body=" + ex.getResponseBodyAsString(),
                    ex);
        }
    }

    private record SendEmailRequest(
            String from,
            List<String> to,
            String subject,
            String html
    ) {}

    private record SendEmailResponse(
            @JsonProperty("id") String id
    ) {}
}
