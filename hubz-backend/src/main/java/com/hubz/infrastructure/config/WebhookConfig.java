package com.hubz.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class WebhookConfig {

    @Bean
    public RestTemplate webhookRestTemplate() {
        return new RestTemplate();
    }
}
