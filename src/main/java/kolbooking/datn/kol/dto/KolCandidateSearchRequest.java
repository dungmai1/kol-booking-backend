package kolbooking.datn.kol.dto;

import java.math.BigDecimal;
import java.util.List;

public record KolCandidateSearchRequest(
        String category,
        List<String> platforms,
        Long minFollowers,
        Long maxFollowers,
        BigDecimal minBudget,
        BigDecimal maxBudget,
        String location,
        String gender,
        String serviceType,
        Integer limit
) {
}
