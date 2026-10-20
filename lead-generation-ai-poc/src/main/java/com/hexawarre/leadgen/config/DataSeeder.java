package com.hexawarre.leadgen.config;

import com.hexawarre.leadgen.dto.CampaignRequest;
import com.hexawarre.leadgen.dto.CampaignResponse;
import com.hexawarre.leadgen.dto.LeadRequest;
import com.hexawarre.leadgen.model.LeadSource;
import com.hexawarre.leadgen.repository.CampaignRepository;
import com.hexawarre.leadgen.service.CampaignService;
import com.hexawarre.leadgen.service.LeadService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** Loads a small demo data set on startup (disable with app.seed.enabled=false). */
@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true", matchIfMissing = true)
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final CampaignRepository campaigns;
    private final CampaignService campaignService;
    private final LeadService leadService;

    public DataSeeder(CampaignRepository campaigns, CampaignService campaignService, LeadService leadService) {
        this.campaigns = campaigns;
        this.campaignService = campaignService;
        this.leadService = leadService;
    }

    @Override
    public void run(String... args) {
        if (campaigns.count() > 0) {
            return;
        }

        CampaignResponse c = campaignService.create(new CampaignRequest(
                "Q4 SaaS Outreach",
                "Outbound campaign for a workflow automation product",
                "Software",
                "Head of Operations"));
        campaignService.generateLeads(c.id(), 6);

        leadService.create(new LeadRequest("Asha Mehta", "asha.mehta@bigcorp-demo.example.com", "+919800000001",
                "BigCorp Demo", "CEO", "Manufacturing", 500, new BigDecimal("800000"), LeadSource.REFERRAL,
                "We need a demo and pricing urgently, ready to purchase this quarter.", null));
        leadService.create(new LeadRequest("Ravi Kumar", "ravi.kumar@midsize-demo.example.com", "+919800000002",
                "Midsize Demo", "Operations Manager", "Logistics", 80, new BigDecimal("120000"), LeadSource.WEBSITE,
                "Looking for a trial of your product.", null));
        leadService.create(new LeadRequest("Sam Student", "sam.student@gmail.com", null,
                null, "Student", null, null, null, LeadSource.WEBSITE,
                "Just curious about what you do.", null));

        log.info("Demo data loaded: 1 campaign, 9 leads");
    }
}
