package com.hexawarre.leadgen;

import com.hexawarre.leadgen.ai.RuleBasedAiEngine;
import com.hexawarre.leadgen.ai.ScoreResult;
import com.hexawarre.leadgen.model.Lead;
import com.hexawarre.leadgen.model.LeadCategory;
import com.hexawarre.leadgen.model.LeadSource;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuleBasedAiEngineTest {

    private final RuleBasedAiEngine engine = new RuleBasedAiEngine();

    @Test
    void strongLeadIsHot() {
        Lead l = new Lead();
        l.setFullName("Asha Mehta");
        l.setEmail("asha@bigcorp.com");
        l.setPhone("+919800000001");
        l.setCompany("BigCorp");
        l.setJobTitle("CEO");
        l.setCompanySize(500);
        l.setBudget(new BigDecimal("800000"));
        l.setSource(LeadSource.REFERRAL);
        l.setMessage("Need a demo and pricing urgently");

        ScoreResult r = engine.evaluate(l);

        assertEquals(LeadCategory.HOT, r.category());
        assertEquals(95, r.score());
        assertTrue(r.followUpDraft().contains("Hi Asha"));
    }

    @Test
    void weakLeadIsCold() {
        Lead l = new Lead();
        l.setFullName("Sam Student");
        l.setEmail("sam@gmail.com");
        l.setSource(LeadSource.WEBSITE);
        l.setMessage("just curious");

        ScoreResult r = engine.evaluate(l);

        assertEquals(LeadCategory.COLD, r.category());
        assertTrue(r.score() < 40);
    }

    @Test
    void doctorIsNotTreatedAsCto() {
        Lead l = new Lead();
        l.setFullName("Dr Who");
        l.setEmail("who@gmail.com");
        l.setJobTitle("Doctor");
        l.setSource(LeadSource.OTHER);

        // contact role (4) + source OTHER (1) = 5; no seniority bonus
        assertEquals(5, engine.evaluate(l).score());
    }
}
