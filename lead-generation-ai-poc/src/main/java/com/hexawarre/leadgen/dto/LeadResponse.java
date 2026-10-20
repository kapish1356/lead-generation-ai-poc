package com.hexawarre.leadgen.dto;

import com.hexawarre.leadgen.model.Lead;
import com.hexawarre.leadgen.model.LeadCategory;
import com.hexawarre.leadgen.model.LeadSource;
import com.hexawarre.leadgen.model.LeadStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record LeadResponse(
        Long id,
        String fullName,
        String email,
        String phone,
        String company,
        String jobTitle,
        String industry,
        Integer companySize,
        BigDecimal budget,
        LeadSource source,
        String message,
        Long campaignId,
        String campaignName,
        Integer score,
        LeadCategory category,
        String aiSummary,
        String followUpDraft,
        String aiProvider,
        LeadStatus status,
        Instant createdAt,
        Instant updatedAt) {

    public static LeadResponse from(Lead l) {
        return new LeadResponse(
                l.getId(),
                l.getFullName(),
                l.getEmail(),
                l.getPhone(),
                l.getCompany(),
                l.getJobTitle(),
                l.getIndustry(),
                l.getCompanySize(),
                l.getBudget(),
                l.getSource(),
                l.getMessage(),
                l.getCampaign() != null ? l.getCampaign().getId() : null,
                l.getCampaign() != null ? l.getCampaign().getName() : null,
                l.getScore(),
                l.getCategory(),
                l.getAiSummary(),
                l.getFollowUpDraft(),
                l.getAiProvider(),
                l.getStatus(),
                l.getCreatedAt(),
                l.getUpdatedAt());
    }
}
