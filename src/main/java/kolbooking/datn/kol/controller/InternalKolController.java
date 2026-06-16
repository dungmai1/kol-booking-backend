package kolbooking.datn.kol.controller;

import jakarta.validation.Valid;
import kolbooking.datn.common.exception.BusinessException;
import kolbooking.datn.common.exception.ErrorCode;
import kolbooking.datn.kol.dto.KolCandidateSearchRequest;
import kolbooking.datn.kol.dto.KolCandidateSearchResponse;
import kolbooking.datn.kol.service.KolSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/internal/kols")
@RequiredArgsConstructor
public class InternalKolController {

    private final KolSearchService kolSearchService;

    @Value("${app.internal.token:}")
    private String internalToken;

    @PostMapping("/search-candidates")
    public KolCandidateSearchResponse searchCandidates(
            @RequestHeader(name = "X-Internal-Token", required = false) String token,
            @Valid @RequestBody KolCandidateSearchRequest request
    ) {
        if (internalToken == null || internalToken.isBlank() || !internalToken.equals(token)) {
            throw new BusinessException("Invalid internal token", ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
        }
        return kolSearchService.searchCandidates(request);
    }
}
