package kolbooking.datn.kol.dto;

import java.util.List;

public record KolCandidateSearchResponse(
        List<KolCandidateResponse> items
) {
}
