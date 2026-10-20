package com.hexawarre.leadgen.ai;

import com.hexawarre.leadgen.model.Lead;

/**
 * Evaluates a lead: score (0-100), HOT/WARM/COLD category, a short summary
 * and a draft follow-up email. Implementations are swappable via ai.provider.
 */
public interface AiLeadEngine {

    ScoreResult evaluate(Lead lead);
}
