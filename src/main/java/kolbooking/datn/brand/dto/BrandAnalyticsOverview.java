package kolbooking.datn.brand.dto;

import java.math.BigDecimal;
import java.util.Map;

public record BrandAnalyticsOverview(
        long totalCampaigns,
        long activeCampaigns,
        long completedCampaigns,
        BigDecimal totalSpend,
        BigDecimal avgBudget,
        BigDecimal pendingEscrow,
        Map<String, Long> campaignsByStatus
) {}
