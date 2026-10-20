package com.hexawarre.leadgen.ai;

import com.hexawarre.leadgen.model.LeadCategory;

public record ScoreResult(int score, LeadCategory category, String summary, String followUpDraft, String provider) {
}
