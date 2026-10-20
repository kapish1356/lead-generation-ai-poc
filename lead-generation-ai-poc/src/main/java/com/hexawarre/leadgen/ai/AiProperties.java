package com.hexawarre.leadgen.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "ai")
public class AiProperties {

    private String provider = "rule";
    private final Claude claude = new Claude();

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public Claude getClaude() { return claude; }

    public static class Claude {
        private String apiKey = "";
        private String model = "claude-sonnet-5-5";
        private int maxTokens = 800;
        private String baseUrl = "https://api.anthropic.com";
        private String version = "2023-06-01";

        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
        public int getMaxTokens() { return maxTokens; }
        public void setMaxTokens(int maxTokens) { this.maxTokens = maxTokens; }
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public String getVersion() { return version; }
        public void setVersion(String version) { this.version = version; }
    }
}
