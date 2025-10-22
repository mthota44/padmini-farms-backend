package com.padmini.gatewayservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Value("${keycloak.base-url:http://padmini-keycloak:8090}")
    private String keycloakBaseUrl;

    @Bean
    public WebClient keycloakWebClient(WebClient.Builder builder) {
        return builder.baseUrl(keycloakBaseUrl).build();
    }
}
