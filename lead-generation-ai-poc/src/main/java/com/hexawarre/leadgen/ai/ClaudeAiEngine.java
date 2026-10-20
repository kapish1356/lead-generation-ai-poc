package com.hexawarre.leadgen.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hexawarre.leadgen.model.Lead;
import com.hexawarre.leadgen.model.LeadCategory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Optional engine, enabled with ai.provider=claude. Calls the Anthropic Messages API.
 * If the key is missing or the call/parse fails, it falls back to the rule-based engine,
 * so the API never breaks because of the AI provider.
 */
@Component
@Primary
@ConditionalOnProperty(prefix = "ai", name = "provider", havingValue = "claude")
public class ClaudeAiEngine implements AiLeadEngine {

    private static final Logger log = LoggerFactory.getLogger(ClaudeAiEngine.class);

    private final AiProperties props;
    private final RuleBasedAiEngine fallback;
    private final ObjectMapper mapper;
    private final RestClient client;

    public ClaudeAiEngine(AiProperties props, RuleBasedAiEngine fallback, ObjectMapper mapper, RestClient.Builder builder) {
        this.props = props;
        this.fallback = fallback;
        this.mapper = mapper;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5_000);
        factory.setReadTimeout(30_000);
        this.client = builder
                .baseUrl(props.getClaude().getBaseUrl())
                .requestFactory(factory)
                .build();
    }

    @Override
    public ScoreResult evaluate(Lead lead) {
        AiProperties.Claude cfg = props.getClaude();
        if (cfg.getApiKey() == null || cfg.getApiKey().isBlank()) {
            log.warn("ai.provider=claude but no API key is set (ANTHROPIC_API_KEY). Using rule-based fallback.");
            return fallbackResult(lead);
        }
        try {
            Map<String, Object> body = Map.of(
                    "model", cfg.getModel(),
                    "max_tokens", cfg.getMaxTokens(),
                    "messages", List.of(Map.of("role", "user", "content", buildPrompt(lead))));

            String raw = client.post()
                    .uri("/v1/messages")
                    .header("x-api-key", cfg.getApiKey())
                    .header("anthropic-version", cfg.getVersion())
                    .header("content-type", "application/json")
                    .body(body)
                    .retrieve()
                    .body(String.class);

            return parse(raw);
        } catch (Exception e) {
            log.warn("Claude call failed ({}). Using rule-based fallback.", e.getMessage());
            return fallbackResult(lead);
        }
    }

    private ScoreResult fallbackResult(Lead lead) {
        ScoreResult r = fallback.evaluate(lead);
        return new ScoreResult(r.score(), r.category(), r.summary(), r.followUpDraft(), "rule-based (claude fallback)");
    }

    private ScoreResult parse(String raw) throws Exception {
        String text = mapper.readTree(raw).path("content").path(0).path("text").asText("");
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new IllegalStateException("No JSON object in model reply");
        }
        JsonNode json = mapper.readTree(text.substring(start, end + 1));

        int score = Math.max(0, Math.min(100, json.path("score").asInt(-1)));
        if (json.path("score").isMissingNode()) {
            throw new IllegalStateException("Model reply has no score");
        }
        LeadCategory category;
        try {
            category = LeadCategory.valueOf(json.path("category").asText("").toUpperCase());
        } catch (IllegalArgumentException e) {
            category = LeadCategory.fromScore(score);
        }
        String summary = cut(json.path("summary").asText(""), 1000);
        String email = cut(json.path("followUpEmail").asText(""), 4000);
        return new ScoreResult(score, category, summary, email, "claude:" + props.getClaude().getModel());
    }

    private static String buildPrompt(Lead l) {
        return """
                You are a B2B sales assistant. Evaluate this inbound lead and reply with ONLY a JSON object, no other text:
                {"score": <integer 0-100>, "category": "HOT|WARM|COLD", "summary": "<max 2 sentences explaining the score>", "followUpEmail": "<subject line, blank line, then a friendly email body under 120 words>"}

                Rules: HOT >= 70, WARM 40-69, COLD < 40. Budget is in Indian Rupees (INR).

                Lead:
                - Name: %s
                - Email: %s
                - Phone: %s
                - Company: %s
                - Job title: %s
                - Industry: %s
                - Company size (employees): %s
                - Budget (INR): %s
                - Source: %s
                - Message: %s
                """.formatted(n(l.getFullName()), n(l.getEmail()), n(l.getPhone()), n(l.getCompany()),
                n(l.getJobTitle()), n(l.getIndustry()), n(l.getCompanySize()), n(l.getBudget()),
                n(l.getSource()), n(l.getMessage()));
    }

    private static String n(Object o) {
        return o == null ? "not provided" : o.toString();
    }

    private static String cut(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max - 3) + "...";
    }
}
