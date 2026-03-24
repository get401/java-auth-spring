package com.get401.auth.spring;

import com.get401.auth.core.client.Get401Client;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(Get401ClientProperties.class)
@ConditionalOnProperty(prefix = "get401.client", name = "api-key")
public class Get401ClientAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public Get401Client get401Client(Get401ClientProperties properties) {
        if (properties.getBaseUrl() != null && !properties.getBaseUrl().isBlank()) {
            return new Get401Client(properties.getApiKey(), properties.getBaseUrl());
        }
        return new Get401Client(properties.getApiKey());
    }
}
