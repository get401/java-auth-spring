package com.get401.auth.spring;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "get401.client")
public class Get401ClientProperties {

    /**
     * API key for server-to-server calls to the Get401 backend API (sk_live_...).
     * When set, a {@link com.get401.auth.core.client.Get401Client} bean is auto-configured.
     */
    private String apiKey;

    /**
     * Base URL of the Get401 API. Defaults to https://app.get401.com when not set.
     */
    private String baseUrl;

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }
}
