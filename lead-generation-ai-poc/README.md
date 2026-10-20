# Lead Generation AI POC (Spring Boot + H2)

A small proof of concept for AI-assisted lead generation:

1. **Capture** leads (landing-page form or API).
2. **AI scoring**: every lead gets a score (0-100), a HOT / WARM / COLD category, a short explanation and a draft follow-up email.
3. **Generate prospects**: create demo prospects for a campaign and score them the same way.
4. **Track** leads through a simple pipeline (NEW, CONTACTED, QUALIFIED, CONVERTED, LOST) and view stats.

Stack: Java 17+, Spring Boot 3.3, Spring Data JPA, H2 (in-memory), Maven. No other services needed.

## Run it

Requirements: JDK 17 or newer, Maven 3.9+ (or use the Maven bundled in IntelliJ).

```bash
mvn spring-boot:run
```

IntelliJ: **File > Open** the `pom.xml`, wait for Maven to import, then run `LeadGenerationApplication`.

Once it is running:

| What | URL |
|------|-----|
| Landing page (lead form) | http://localhost:8080/ |
| Dashboard | http://localhost:8080/dashboard.html |
| H2 console | http://localhost:8080/h2-console |
| API base | http://localhost:8080/api |

H2 console login: JDBC URL `jdbc:h2:mem:leadgendb`, user `sa`, empty password.

Data is in memory, so it resets on every restart. On startup the app loads 1 campaign, 6 generated prospects and 3 hand-written leads (one hot, one warm, one cold). Turn this off with `app.seed.enabled=false`.

Run the tests: `mvn test`

## API

| Method | Path | Purpose |
|--------|------|---------|
| POST | `/api/leads` | Capture a lead, AI scores it immediately (201) |
| GET | `/api/leads?category=HOT&status=NEW&page=0&size=20` | List leads, best score first |
| GET | `/api/leads/{id}` | One lead |
| PATCH | `/api/leads/{id}/status` | Body `{"status":"QUALIFIED"}` |
| POST | `/api/leads/{id}/rescore` | Re-run AI scoring and regenerate the follow-up draft |
| DELETE | `/api/leads/{id}` | Delete (204) |
| GET | `/api/leads/stats` | Totals, average score, counts by category and status, conversion % |
| POST | `/api/campaigns` | Create a campaign |
| GET | `/api/campaigns` | List campaigns with lead counts |
| GET | `/api/campaigns/{id}` | One campaign |
| GET | `/api/campaigns/{id}/leads` | Leads of a campaign |
| POST | `/api/campaigns/{id}/generate-leads?count=5` | Generate 1-20 demo prospects and score them |

Errors come back as JSON: 400 validation (with `fieldErrors`), 404 not found, 409 duplicate email.

### Example

```bash
curl -X POST http://localhost:8080/api/leads -H "Content-Type: application/json" -d '{
  "fullName": "Asha Mehta",
  "email": "asha@bigcorp.com",
  "phone": "+919800000001",
  "company": "BigCorp",
  "jobTitle": "CEO",
  "companySize": 500,
  "budget": 800000,
  "source": "REFERRAL",
  "message": "Need a demo and pricing urgently"
}'
```

`source` values: WEBSITE, LINKEDIN, REFERRAL, EVENT, COLD_EMAIL, AI_GENERATED, OTHER. `budget` is in INR.

A Postman collection is in `postman/lead-generation-ai-poc.postman_collection.json` (import it and run the requests top to bottom).

## How the AI scoring works

The AI part sits behind one interface, `AiLeadEngine`, with two implementations. Choose with `ai.provider` in `application.properties`.

**`rule` (default).** Offline and explainable, no key needed. Points (max 100):

| Signal | Max points |
|--------|-----------|
| Seniority of job title (CEO/founder/director = 20, manager = 12, other = 4) | 20 |
| Company size (200+ = 15, 50+ = 10, 10+ = 6, smaller = 2) | 15 |
| Budget (500k+ = 20, 100k+ = 14, 25k+ = 8, any = 3) | 20 |
| Buying-intent words in the message (demo, pricing, quote, trial, urgent...), 5 each | 20 |
| Source (referral 10, event 8, website 6, LinkedIn 5, cold/generated 2) | 10 |
| Business e-mail domain (not gmail/yahoo/etc.) | 10 |
| Phone provided | 5 |

HOT is 70 and above, WARM is 40-69, COLD is below 40. The follow-up email is a template chosen by category.

**`claude` (optional).** Sends the lead to the Anthropic Messages API and asks for a JSON answer (score, category, summary, follow-up email). To enable it:

```bash
export ANTHROPIC_API_KEY=your-key        # Windows PowerShell: $env:ANTHROPIC_API_KEY="your-key"
mvn spring-boot:run -Dspring-boot.run.arguments=--ai.provider=claude
```

The model name is `ai.claude.model` (default `claude-sonnet-5-5`). If the key is missing or the call fails, the app falls back to the rule-based engine for that lead and records `rule-based (claude fallback)` in the `aiProvider` field, so the API never breaks because of the AI provider.

## Project layout

```
src/main/java/com/hexawarre/leadgen
  ai/          AiLeadEngine, RuleBasedAiEngine, ClaudeAiEngine, AiProperties
  config/      CorsConfig, DataSeeder
  controller/  LeadController, CampaignController
  dto/         request/response records
  exception/   GlobalExceptionHandler and custom exceptions
  model/       Lead, Campaign and enums
  repository/  Spring Data repositories
  service/     LeadService, CampaignService, ProspectGenerator
src/main/resources
  application.properties
  static/      index.html (lead form), dashboard.html
src/test/java  unit test for scoring + MockMvc API tests
```

## Notes and next steps

- **Generated prospects are fake.** `ProspectGenerator` invents names, companies and phone numbers, and uses the reserved `example.com` domain for e-mails. It is for demos only. For real lead generation, replace it with a data provider or import source you are allowed to use, and respect consent rules for outreach (GDPR, India's DPDP Act, CAN-SPAM).
- No authentication in this POC. Add Spring Security (for example JWT) before exposing it. CORS is open to all origins for local development.
- To move from H2 to MySQL or PostgreSQL: swap the driver dependency and change the `spring.datasource.*` properties.
- Natural extensions: send the follow-up e-mails (Spring Mail), push hot leads to a CRM or Slack webhook, add a React or React Native client on top of this API.
