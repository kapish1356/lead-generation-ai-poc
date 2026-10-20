package com.hexawarre.leadgen.ai;

import com.hexawarre.leadgen.model.Lead;
import com.hexawarre.leadgen.model.LeadCategory;
import com.hexawarre.leadgen.model.LeadSource;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Default engine. Deterministic, offline, explainable scoring (max 100 points):
 * seniority 20, company size 15, budget 20, buying-intent words 20,
 * source 10, business e-mail 10, phone 5.
 */
@Component
public class RuleBasedAiEngine implements AiLeadEngine {

    public static final String PROVIDER = "rule-based";

    private static final Set<String> FREE_EMAIL_DOMAINS =
            Set.of("gmail.com", "yahoo.com", "outlook.com", "hotmail.com", "icloud.com", "proton.me", "rediffmail.com");

    private static final List<String> INTENT_WORDS =
            List.of("demo", "pricing", "quote", "buy", "purchase", "urgent", "trial", "proposal", "asap", "contract", "implement");

    private static final Pattern SENIOR = Pattern.compile(
            "\\b(ceo|cto|cfo|coo|cio|founder|co-founder|owner|president|director|vp|vice president|chief)\\b");
    private static final Pattern MID = Pattern.compile("\\b(head|manager|lead|principal)\\b");

    @Override
    public ScoreResult evaluate(Lead lead) {
        int score = 0;
        List<String> reasons = new ArrayList<>();

        // 1. Seniority (max 20)
        String title = lower(lead.getJobTitle());
        if (SENIOR.matcher(title).find()) {
            score += 20;
            reasons.add("senior decision maker (" + lead.getJobTitle() + ")");
        } else if (MID.matcher(title).find()) {
            score += 12;
            reasons.add("manager-level contact (" + lead.getJobTitle() + ")");
        } else if (!title.isBlank()) {
            score += 4;
            reasons.add("contact role: " + lead.getJobTitle());
        }

        // 2. Company size (max 15)
        Integer size = lead.getCompanySize();
        if (size != null) {
            if (size >= 200) {
                score += 15;
                reasons.add("large company (" + size + " employees)");
            } else if (size >= 50) {
                score += 10;
                reasons.add("mid-size company (" + size + " employees)");
            } else if (size >= 10) {
                score += 6;
                reasons.add("small company (" + size + " employees)");
            } else {
                score += 2;
                reasons.add("very small company (" + size + " employees)");
            }
        }

        // 3. Budget in INR (max 20)
        BigDecimal budget = lead.getBudget();
        if (budget != null && budget.signum() > 0) {
            long v = budget.longValue();
            if (v >= 500_000) {
                score += 20;
            } else if (v >= 100_000) {
                score += 14;
            } else if (v >= 25_000) {
                score += 8;
            } else {
                score += 3;
            }
            reasons.add("stated budget INR " + String.format(Locale.US, "%,d", v));
        }

        // 4. Buying intent in the message (max 20)
        String msg = lower(lead.getMessage());
        List<String> hits = INTENT_WORDS.stream().filter(msg::contains).toList();
        if (!hits.isEmpty()) {
            score += Math.min(20, hits.size() * 5);
            reasons.add("buying-intent signals in message (" + String.join(", ", hits) + ")");
        }

        // 5. Source (max 10)
        LeadSource source = lead.getSource() != null ? lead.getSource() : LeadSource.OTHER;
        int sourcePoints = switch (source) {
            case REFERRAL -> 10;
            case EVENT -> 8;
            case WEBSITE -> 6;
            case LINKEDIN -> 5;
            case COLD_EMAIL, AI_GENERATED -> 2;
            case OTHER -> 1;
        };
        score += sourcePoints;
        if (sourcePoints >= 8) {
            reasons.add("high-trust source (" + source + ")");
        }

        // 6. Business e-mail domain (max 10)
        String domain = emailDomain(lead.getEmail());
        if (!domain.isEmpty() && !FREE_EMAIL_DOMAINS.contains(domain)) {
            score += 10;
            reasons.add("business e-mail domain (" + domain + ")");
        }

        // 7. Phone provided (max 5)
        if (lead.getPhone() != null && !lead.getPhone().isBlank()) {
            score += 5;
            reasons.add("phone number provided");
        }

        score = Math.min(100, score);
        LeadCategory category = LeadCategory.fromScore(score);

        String summary = String.format("Score %d/100 (%s). %s", score, category,
                reasons.isEmpty() ? "Limited information provided." : "Signals: " + String.join("; ", reasons) + ".");
        if (summary.length() > 1000) {
            summary = summary.substring(0, 997) + "...";
        }

        return new ScoreResult(score, category, summary, buildFollowUp(lead, category), PROVIDER);
    }

    private String buildFollowUp(Lead lead, LeadCategory category) {
        String first = firstName(lead.getFullName());
        String company = lead.getCompany() != null && !lead.getCompany().isBlank() ? lead.getCompany() : "your team";
        return switch (category) {
            case HOT -> "Subject: Let's talk this week about " + company + "\n\n"
                    + "Hi " + first + ",\n\n"
                    + "Thanks for reaching out. Based on what you shared, I think we can help " + company
                    + " move quickly. Could we do a 20-minute call in the next 24 hours? "
                    + "I'll bring a tailored demo and pricing options so you can decide fast.\n\n"
                    + "Best regards,\nSales Team";
            case WARM -> "Subject: A quick look at how we help teams like " + company + "\n\n"
                    + "Hi " + first + ",\n\n"
                    + "Thanks for your interest. I've attached a short case study from a similar company. "
                    + "If it looks relevant, I can set up a 15-minute demo this week. "
                    + "What day works best for you?\n\n"
                    + "Best regards,\nSales Team";
            case COLD -> "Subject: Useful resources for " + company + "\n\n"
                    + "Hi " + first + ",\n\n"
                    + "Thanks for getting in touch. I'm sharing a few guides that might help while you evaluate options. "
                    + "Whenever you're ready to talk, just reply to this email.\n\n"
                    + "Best regards,\nSales Team";
        };
    }

    private static String firstName(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return "there";
        }
        return fullName.trim().split("\\s+")[0];
    }

    private static String emailDomain(String email) {
        if (email == null || !email.contains("@")) {
            return "";
        }
        return email.substring(email.indexOf('@') + 1).toLowerCase(Locale.ROOT);
    }

    private static String lower(String s) {
        return s == null ? "" : s.toLowerCase(Locale.ROOT);
    }
}
