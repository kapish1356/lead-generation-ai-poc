package com.hexawarre.leadgen.controller;

import com.hexawarre.leadgen.dto.LeadRequest;
import com.hexawarre.leadgen.dto.LeadResponse;
import com.hexawarre.leadgen.dto.PageResponse;
import com.hexawarre.leadgen.dto.StatsResponse;
import com.hexawarre.leadgen.dto.StatusUpdateRequest;
import com.hexawarre.leadgen.model.LeadCategory;
import com.hexawarre.leadgen.model.LeadStatus;
import com.hexawarre.leadgen.service.LeadService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/leads")
public class LeadController {

    private final LeadService service;

    public LeadController(LeadService service) {
        this.service = service;
    }

    /** Capture a lead (landing page form, webhook, etc.). The AI engine scores it immediately. */
    @PostMapping
    public ResponseEntity<LeadResponse> create(@Valid @RequestBody LeadRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @GetMapping
    public PageResponse<LeadResponse> list(@RequestParam(required = false) LeadCategory category,
                                           @RequestParam(required = false) LeadStatus status,
                                           @RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "20") int size) {
        return service.list(category, status, page, size);
    }

    @GetMapping("/stats")
    public StatsResponse stats() {
        return service.stats();
    }

    @GetMapping("/{id}")
    public LeadResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @PatchMapping("/{id}/status")
    public LeadResponse updateStatus(@PathVariable Long id, @Valid @RequestBody StatusUpdateRequest request) {
        return service.updateStatus(id, request.status());
    }

    /** Re-run AI scoring and regenerate the follow-up draft. */
    @PostMapping("/{id}/rescore")
    public LeadResponse rescore(@PathVariable Long id) {
        return service.rescore(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
