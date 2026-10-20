package com.hexawarre.leadgen.dto;

import com.hexawarre.leadgen.model.LeadSource;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record LeadRequest(
        @NotBlank @Size(max = 120) String fullName,
        @NotBlank @Email @Size(max = 160) String email,
        @Size(max = 30) String phone,
        @Size(max = 120) String company,
        @Size(max = 120) String jobTitle,
        @Size(max = 80) String industry,
        @Min(1) Integer companySize,
        @PositiveOrZero BigDecimal budget,
        LeadSource source,
        @Size(max = 2000) String message,
        Long campaignId) {
}
