package com.hexawarre.leadgen.service;

import com.hexawarre.leadgen.dto.CampaignRequest;
import com.hexawarre.leadgen.dto.CampaignResponse;
import com.hexawarre.leadgen.dto.GenerateLeadsResponse;
import com.hexawarre.leadgen.dto.LeadRequest;
import com.hexawarre.leadgen.dto.LeadResponse;
import com.hexawarre.leadgen.exception.ResourceNotFoundException;
import com.hexawarre.leadgen.model.Campaign;
import com.hexawarre.leadgen.repository.CampaignRepository;
import com.hexawarre.leadgen.repository.LeadRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class CampaignService {

    private final CampaignRepository campaigns;
    private final LeadRepository leads;
    private final LeadService leadService;
    private final ProspectGenerator generator;

    public CampaignService(CampaignRepository campaigns, LeadRepository leads,
                           LeadService leadService, ProspectGenerator generator) {
        this.campaigns = campaigns;
        this.leads = leads;
        this.leadService = leadService;
        this.generator = generator;
    }

    @Transactional
    public CampaignResponse create(CampaignRequest req) {
        Campaign c = new Campaign();
        c.setName(req.name().trim());
        c.setDescription(req.description());
        c.setTargetIndustry(req.targetIndustry());
        c.setTargetTitle(req.targetTitle());
        Campaign saved = campaigns.save(c);
        return CampaignResponse.from(saved, 0);
    }

    @Transactional(readOnly = true)
    public List<CampaignResponse> list() {
        return campaigns.findAll().stream()
                .map(c -> CampaignResponse.from(c, leads.countByCampaignId(c.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public CampaignResponse get(Long id) {
        Campaign c = find(id);
        return CampaignResponse.from(c, leads.countByCampaignId(id));
    }

    @Transactional(readOnly = true)
    public List<LeadResponse> leadsOf(Long id) {
        find(id);
        return leads.findByCampaignIdOrderByScoreDescCreatedAtDesc(id).stream().map(LeadResponse::from).toList();
    }

    /** Generates synthetic prospects for a campaign and runs each through the AI engine. */
    @Transactional
    public GenerateLeadsResponse generateLeads(Long id, int count) {
        Campaign campaign = find(id);
        List<LeadResponse> created = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            for (int attempt = 0; attempt < 5; attempt++) {
                LeadRequest req = generator.generate(campaign);
                if (!leadService.emailExists(req.email())) {
                    created.add(leadService.createForCampaign(req, campaign));
                    break;
                }
            }
        }
        return new GenerateLeadsResponse(id, created.size(), created);
    }

    private Campaign find(Long id) {
        return campaigns.findById(id).orElseThrow(() -> new ResourceNotFoundException("Campaign", id));
    }
}
