package com.hexawarre.leadgen;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "app.seed.enabled=false")
@AutoConfigureMockMvc
class LeadApiTest {

    @Autowired
    private MockMvc mvc;

    private static final String HOT_LEAD = """
            {"fullName":"Asha Mehta","email":"%s","phone":"+919800000001","company":"BigCorp",
             "jobTitle":"CEO","companySize":500,"budget":800000,"source":"REFERRAL",
             "message":"Need a demo and pricing urgently"}""";

    @Test
    void createsAndScoresHotLead() throws Exception {
        mvc.perform(post("/api/leads").contentType(MediaType.APPLICATION_JSON)
                        .content(HOT_LEAD.formatted("asha.hot@bigcorp.com")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.category").value("HOT"))
                .andExpect(jsonPath("$.score").value(greaterThanOrEqualTo(70)))
                .andExpect(jsonPath("$.status").value("NEW"))
                .andExpect(jsonPath("$.followUpDraft").isNotEmpty());
    }

    @Test
    void rejectsDuplicateEmail() throws Exception {
        String body = HOT_LEAD.formatted("dup@bigcorp.com");
        mvc.perform(post("/api/leads").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/leads").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void validatesInput() throws Exception {
        mvc.perform(post("/api/leads").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"\",\"email\":\"not-an-email\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.email").exists())
                .andExpect(jsonPath("$.fieldErrors.fullName").exists());
    }

    @Test
    void updatesStatusAndReturns404ForUnknownLead() throws Exception {
        String json = mvc.perform(post("/api/leads").contentType(MediaType.APPLICATION_JSON)
                        .content(HOT_LEAD.formatted("status@bigcorp.com")))
                .andReturn().getResponse().getContentAsString();
        String id = json.replaceAll(".*\"id\":(\\d+).*", "$1");

        mvc.perform(patch("/api/leads/" + id + "/status").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"QUALIFIED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("QUALIFIED"));

        mvc.perform(delete("/api/leads/" + id)).andExpect(status().isNoContent());
        mvc.perform(get("/api/leads/" + id)).andExpect(status().isNotFound());
    }

    @Test
    void campaignGeneratesScoredLeadsAndStatsReflectThem() throws Exception {
        String json = mvc.perform(post("/api/campaigns").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Test Campaign\",\"targetIndustry\":\"Software\",\"targetTitle\":\"CTO\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String id = json.replaceAll(".*\"id\":(\\d+).*", "$1");

        mvc.perform(post("/api/campaigns/" + id + "/generate-leads?count=3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.generated").value(3))
                .andExpect(jsonPath("$.leads[0].source").value("AI_GENERATED"))
                .andExpect(jsonPath("$.leads[0].score").isNumber());

        mvc.perform(get("/api/campaigns/" + id + "/leads"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));

        mvc.perform(get("/api/leads/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalLeads").value(greaterThanOrEqualTo(3)))
                .andExpect(jsonPath("$.byCategory.HOT").exists());

        mvc.perform(post("/api/campaigns/" + id + "/generate-leads?count=99"))
                .andExpect(status().isBadRequest());
    }
}
