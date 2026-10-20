package com.hexawarre.leadgen.repository;

import com.hexawarre.leadgen.model.Lead;
import com.hexawarre.leadgen.model.LeadCategory;
import com.hexawarre.leadgen.model.LeadStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface LeadRepository extends JpaRepository<Lead, Long>, JpaSpecificationExecutor<Lead> {

    boolean existsByEmailIgnoreCase(String email);

    long countByCategory(LeadCategory category);

    long countByStatus(LeadStatus status);

    long countByCampaignId(Long campaignId);

    List<Lead> findByCampaignIdOrderByScoreDescCreatedAtDesc(Long campaignId);

    @Query("select avg(l.score) from Lead l")
    Double averageScore();
}
