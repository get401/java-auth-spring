package com.get401.auth.spring;

import com.get401.auth.core.JwtPublicKeyProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(Get401AuthProperties.class)
public class Get401AuthAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public JwtPublicKeyProvider jwtPublicKeyProvider(Get401AuthProperties properties) {
        if (properties.getAppId() == null || properties.getAppId().isBlank()) {
            throw new IllegalArgumentException("get401.auth.app-id must be configured");
        }
        if (properties.getOrigin() == null || properties.getOrigin().isBlank()) {
            throw new IllegalArgumentException("get401.auth.origin must be configured");
        }
        return new JwtPublicKeyProvider(properties.getAppId(), properties.getOrigin(), properties.getBaseUrl());
    }
}
