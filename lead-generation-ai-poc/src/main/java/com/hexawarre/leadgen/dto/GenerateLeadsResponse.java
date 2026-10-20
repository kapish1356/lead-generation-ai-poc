package com.hexawarre.leadgen.dto;

import java.util.List;

public record GenerateLeadsResponse(Long campaignId, int generated, List<LeadResponse> leads) {
}
