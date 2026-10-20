package com.hexawarre.leadgen.service;

import com.hexawarre.leadgen.dto.LeadRequest;
import com.hexawarre.leadgen.model.Campaign;
import com.hexawarre.leadgen.model.LeadSource;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Creates SYNTHETIC prospects for demos. All data is invented and uses the
 * reserved example.com domain, so nothing here is a real person or company.
 */
@Component
public class ProspectGenerator {

    private static final List<String> FIRST = List.of("Aarav", "Diya", "Rohan", "Priya", "Kabir", "Ananya",
            "Vihaan", "Isha", "Arjun", "Meera", "Nikhil", "Sana");
    private static final List<String> LAST = List.of("Sharma", "Verma", "Iyer", "Reddy", "Khan", "Gupta",
            "Nair", "Patel", "Singh", "Das", "Joshi", "Mehta");
    private static final List<String> ROOT = List.of("Nova", "Zenith", "Orbit", "Pinnacle", "Lumen",
            "Vertex", "Apex", "Cobalt", "Helix", "Quanta");
    private static final List<String> SUFFIX = List.of("Technologies", "Systems", "Labs", "Solutions", "Networks", "Digital");
    private static final List<String> TITLES = List.of("CEO", "CTO", "Founder", "VP Engineering", "Director of IT",
            "Head of Operations", "Product Manager", "Software Engineer", "Operations Executive", "Marketing Associate");
    private static final List<Integer> SIZES = List.of(8, 25, 60, 120, 250, 600, 1500);
    private static final List<Integer> BUDGETS = List.of(0, 20_000, 60_000, 150_000, 400_000, 900_000);
    private static final List<String> MESSAGES = List.of(
            "We are looking for a demo and pricing for our team. Need to decide this month.",
            "Interested in a trial. Please share a proposal and contract terms.",
            "Exploring options, no fixed timeline yet.",
            "We want to buy a solution urgently and implement it within 6 weeks.",
            "Just browsing for ideas.",
            "Can you send a quote for 50 users?");

    public LeadRequest generate(Campaign campaign) {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        String first = pick(FIRST, r);
        String last = pick(LAST, r);
        String companyName = pick(ROOT, r) + " " + pick(SUFFIX, r);
        String domain = companyName.replace(" ", "").toLowerCase() + ".example.com";
        String email = (first + "." + last + r.nextInt(100, 1000)).toLowerCase() + "@" + domain;

        String title = pick(TITLES, r);
        if (campaign.getTargetTitle() != null && !campaign.getTargetTitle().isBlank() && r.nextBoolean()) {
            title = campaign.getTargetTitle();
        }
        int budget = pick(BUDGETS, r);
        String phone = "+91" + (7 + r.nextInt(3)) + String.format("%09d", r.nextInt(1_000_000_000));

        return new LeadRequest(
                first + " " + last,
                email,
                phone,
                companyName,
                title,
                campaign.getTargetIndustry(),
                pick(SIZES, r),
                budget == 0 ? null : BigDecimal.valueOf(budget),
                LeadSource.AI_GENERATED,
                pick(MESSAGES, r),
                campaign.getId());
    }

    private static <T> T pick(List<T> list, ThreadLocalRandom r) {
        return list.get(r.nextInt(list.size()));
    }
}
