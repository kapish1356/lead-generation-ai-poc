package com.hexawarre.leadgen.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CampaignRequest(
        @NotBlank @Size(max = 120) String name,
        @Size(max = 1000) String description,
        @Size(max = 80) String targetIndustry,
        @Size(max = 120) String targetTitle) {
}
