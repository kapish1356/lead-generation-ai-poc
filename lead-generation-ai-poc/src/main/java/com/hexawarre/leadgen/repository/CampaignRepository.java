package com.hexawarre.leadgen.repository;

import com.hexawarre.leadgen.model.Campaign;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CampaignRepository extends JpaRepository<Campaign, Long> {
}
