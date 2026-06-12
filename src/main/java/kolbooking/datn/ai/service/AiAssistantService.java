package kolbooking.datn.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import kolbooking.datn.ai.dto.AiChatRequest;
import kolbooking.datn.ai.dto.AiChatResponse;
import kolbooking.datn.ai.dto.AiHealthResponse;
import kolbooking.datn.ai.dto.AiServiceChatRequest;
import kolbooking.datn.brand.domain.BrandProfile;
import kolbooking.datn.brand.service.BrandProfileService;
import kolbooking.datn.common.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Service
public class AiAssistantService {

    private final BrandProfileService brandProfileService;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public AiAssistantService(
            BrandProfileService brandProfileService,
            ObjectMapper objectMapper,
            RestClient.Builder restClientBuilder,
            @Value("${app.ai-service.base-url:http://localhost:8001/api/v1}") String aiServiceBaseUrl
    ) {
        this.brandProfileService = brandProfileService;
        this.objectMapper = objectMapper;
        this.restClient = restClientBuilder
                .requestFactory(new SimpleClientHttpRequestFactory())
                .baseUrl(aiServiceBaseUrl)
                .build();
    }

    public AiChatResponse chat(AiChatRequest request) {
        BrandProfile brandProfile = brandProfileService.getCurrentBrandProfile();
        AiServiceChatRequest aiRequest = new AiServiceChatRequest(
                brandProfile.getId(),
                request.conversationId(),
                request.message()
        );

        try {
            return restClient.post()
                    .uri("/chat")
                    .body(aiRequest)
                    .retrieve()
                    .body(AiChatResponse.class);
        } catch (RestClientResponseException ex) {
            throw mapAiServiceError(ex);
        } catch (ResourceAccessException ex) {
            throw new BusinessException(
                    "Không thể kết nối AI service",
                    "AI_SERVICE_UNAVAILABLE",
                    HttpStatus.BAD_GATEWAY
            );
        }
    }

    public AiHealthResponse health() {
        try {
            return restClient.get()
                    .uri("/health")
                    .retrieve()
                    .body(AiHealthResponse.class);
        } catch (RestClientResponseException ex) {
            throw mapAiServiceError(ex);
        } catch (ResourceAccessException ex) {
            throw new BusinessException(
                    "Không thể kết nối AI service",
                    "AI_SERVICE_UNAVAILABLE",
                    HttpStatus.BAD_GATEWAY
            );
        }
    }

    private BusinessException mapAiServiceError(RestClientResponseException ex) {
        AiErrorEnvelope envelope = parseAiError(ex.getResponseBodyAsString());
        String code = envelope != null && envelope.error() != null && envelope.error().code() != null
                ? envelope.error().code()
                : "AI_SERVICE_ERROR";
        String message = envelope != null && envelope.error() != null && envelope.error().message() != null
                ? envelope.error().message()
                : "AI service trả về lỗi";
        HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
        return new BusinessException(message, code, status != null ? status : HttpStatus.BAD_GATEWAY);
    }

    private AiErrorEnvelope parseAiError(String body) {
        if (body == null || body.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(body, AiErrorEnvelope.class);
        } catch (Exception ignored) {
            return null;
        }
    }

    private record AiErrorEnvelope(AiError error) {}

    private record AiError(String code, String message, Object details) {}
}
