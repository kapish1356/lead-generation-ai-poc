package com.hexawarre.leadgen.dto;

import com.hexawarre.leadgen.model.LeadStatus;
import jakarta.validation.constraints.NotNull;

public record StatusUpdateRequest(@NotNull LeadStatus status) {
}
