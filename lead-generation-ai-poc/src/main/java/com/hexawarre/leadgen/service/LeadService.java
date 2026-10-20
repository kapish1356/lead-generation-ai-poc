package com.hexawarre.leadgen.service;

import com.hexawarre.leadgen.ai.AiLeadEngine;
import com.hexawarre.leadgen.ai.ScoreResult;
import com.hexawarre.leadgen.dto.LeadRequest;
import com.hexawarre.leadgen.dto.LeadResponse;
import com.hexawarre.leadgen.dto.PageResponse;
import com.hexawarre.leadgen.dto.StatsResponse;
import com.hexawarre.leadgen.exception.DuplicateLeadException;
import com.hexawarre.leadgen.exception.ResourceNotFoundException;
import com.hexawarre.leadgen.model.Campaign;
import com.hexawarre.leadgen.model.Lead;
import com.hexawarre.leadgen.model.LeadCategory;
import com.hexawarre.leadgen.model.LeadSource;
import com.hexawarre.leadgen.model.LeadStatus;
import com.hexawarre.leadgen.repository.CampaignRepository;
import com.hexawarre.leadgen.repository.LeadRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Service
public class LeadService {

    private final LeadRepository leads;
    private final CampaignRepository campaigns;
    private final AiLeadEngine engine;

    public LeadService(LeadRepository leads, CampaignRepository campaigns, AiLeadEngine engine) {
        this.leads = leads;
        this.campaigns = campaigns;
        this.engine = engine;
    }

    @Transactional
    public LeadResponse create(LeadRequest req) {
        Campaign campaign = null;
        if (req.campaignId() != null) {
            campaign = campaigns.findById(req.campaignId())
                    .orElseThrow(() -> new ResourceNotFoundException("Campaign", req.campaignId()));
        }
        return createForCampaign(req, campaign);
    }

    @Transactional
    public LeadResponse createForCampaign(LeadRequest req, Campaign campaign) {
        String email = req.email().trim().toLowerCase(Locale.ROOT);
        if (leads.existsByEmailIgnoreCase(email)) {
            throw new DuplicateLeadException(email);
        }

        Lead lead = new Lead();
        lead.setFullName(req.fullName().trim());
        lead.setEmail(email);
        lead.setPhone(req.phone());
        lead.setCompany(req.company());
        lead.setJobTitle(req.jobTitle());
        lead.setIndustry(req.industry());
        lead.setCompanySize(req.companySize());
        lead.setBudget(req.budget());
        lead.setSource(req.source() != null ? req.source() : LeadSource.WEBSITE);
        lead.setMessage(req.message());
        lead.setCampaign(campaign);
        applyAi(lead);

        return LeadResponse.from(leads.save(lead));
    }

    @Transactional(readOnly = true)
    public boolean emailExists(String email) {
        return leads.existsByEmailIgnoreCase(email);
    }

    @Transactional(readOnly = true)
    public LeadResponse get(Long id) {
        return LeadResponse.from(find(id));
    }

    @Transactional(readOnly = true)
    public PageResponse<LeadResponse> list(LeadCategory category, LeadStatus status, int page, int size) {
        Specification<Lead> spec = (root, query, cb) -> cb.conjunction();
        if (category != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("category"), category));
        }
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Order.desc("score"), Sort.Order.desc("createdAt")));
        return PageResponse.from(leads.findAll(spec, pageable).map(LeadResponse::from));
    }

    @Transactional
    public LeadResponse updateStatus(Long id, LeadStatus status) {
        Lead lead = find(id);
        lead.setStatus(status);
        return LeadResponse.from(leads.save(lead));
    }

    @Transactional
    public LeadResponse rescore(Long id) {
        Lead lead = find(id);
        applyAi(lead);
        return LeadResponse.from(leads.save(lead));
    }

    @Transactional
    public void delete(Long id) {
        leads.delete(find(id));
    }

    @Transactional(readOnly = true)
    public StatsResponse stats() {
        long total = leads.count();
        Double avg = leads.averageScore();

        Map<String, Long> byCategory = new LinkedHashMap<>();
        for (LeadCategory c : LeadCategory.values()) {
            byCategory.put(c.name(), leads.countByCategory(c));
        }
        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (LeadStatus s : LeadStatus.values()) {
            byStatus.put(s.name(), leads.countByStatus(s));
        }
        long converted = byStatus.get(LeadStatus.CONVERTED.name());
        double conversion = total == 0 ? 0 : Math.round(converted * 1000.0 / total) / 10.0;
        double average = avg == null ? 0 : Math.round(avg * 10.0) / 10.0;
        return new StatsResponse(total, average, conversion, byCategory, byStatus);
    }

    private void applyAi(Lead lead) {
        ScoreResult r = engine.evaluate(lead);
        lead.setScore(r.score());
        lead.setCategory(r.category());
        lead.setAiSummary(r.summary());
        lead.setFollowUpDraft(r.followUpDraft());
        lead.setAiProvider(r.provider());
    }

    private Lead find(Long id) {
        return leads.findById(id).orElseThrow(() -> new ResourceNotFoundException("Lead", id));
    }
}
