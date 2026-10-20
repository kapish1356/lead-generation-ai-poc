package com.hexawarre.leadgen.dto;

import com.hexawarre.leadgen.model.Campaign;

import java.time.Instant;

public record CampaignResponse(
        Long id,
        String name,
        String description,
        String targetIndustry,
        String targetTitle,
        long leadCount,
        Instant createdAt) {

    public static CampaignResponse from(Campaign c, long leadCount) {
        return new CampaignResponse(c.getId(), c.getName(), c.getDescription(), c.getTargetIndustry(),
                c.getTargetTitle(), leadCount, c.getCreatedAt());
    }
}
