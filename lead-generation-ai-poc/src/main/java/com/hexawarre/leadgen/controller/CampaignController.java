package com.hexawarre.leadgen.controller;

import com.hexawarre.leadgen.dto.CampaignRequest;
import com.hexawarre.leadgen.dto.CampaignResponse;
import com.hexawarre.leadgen.dto.GenerateLeadsResponse;
import com.hexawarre.leadgen.dto.LeadResponse;
import com.hexawarre.leadgen.service.CampaignService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/campaigns")
@Validated
public class CampaignController {

    private final CampaignService service;

    public CampaignController(CampaignService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<CampaignResponse> create(@Valid @RequestBody CampaignRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @GetMapping
    public List<CampaignResponse> list() {
        return service.list();
    }

    @GetMapping("/{id}")
    public CampaignResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @GetMapping("/{id}/leads")
    public List<LeadResponse> leads(@PathVariable Long id) {
        return service.leadsOf(id);
    }

    /** Generates synthetic prospects (demo data) and scores them with the AI engine. */
    @PostMapping("/{id}/generate-leads")
    public GenerateLeadsResponse generate(@PathVariable Long id,
                                          @RequestParam(defaultValue = "5") @Min(1) @Max(20) int count) {
        return service.generateLeads(id, count);
    }
}
