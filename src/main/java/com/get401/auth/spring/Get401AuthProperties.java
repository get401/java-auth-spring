package com.get401.auth.spring;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "get401.auth")
public class Get401AuthProperties {

    /**
     * Application ID used to fetch the public key.
     */
    private String appId;

    /**
     * Origin header to send when fetching the public key.
     */
    private String origin;

    /**
     * Base URL of the Get401 API.
     */
    private String baseUrl = "https://app.get401.com";

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }
}
