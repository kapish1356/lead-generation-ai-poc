package com.hexawarre.leadgen.dto;

import java.util.Map;

public record StatsResponse(
        long totalLeads,
        double averageScore,
        double conversionRatePercent,
        Map<String, Long> byCategory,
        Map<String, Long> byStatus) {
}
