package kolbooking.datn.ai.dto;

import java.util.List;

public record KolSearchCriteria(
        String category,
        List<String> platforms,
        Integer minFollowers,
        Integer maxFollowers,
        Integer minBudget,
        Integer maxBudget,
        String location,
        String gender,
        String campaignGoal,
        String serviceType
) {}
