package com.padmini.gatewayservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.padmini.gatewayservice.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final WebClient keycloak;
    private final KeycloakAdminClient adminClient;

    @Value("${keycloak.realm:padmini-farms}")
    private String realm;

    @Value("${keycloak.gateway-client-id:padmini-gateway}")
    private String clientId;

    @Value("${keycloak.gateway-client-secret:gateway-secret}")
    private String clientSecret;

    public Mono<RegisterResponse> register(RegisterRequest req) {
        System.out.println("hii:" + "hii");
        return adminClient.registerUser(req);
    }

    public Mono<LoginResponse> login(LoginRequest req) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "password");
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("username", req.getUsername());
        form.add("password", req.getPassword());

        return keycloak.post()
                .uri("/realms/{realm}/protocol/openid-connect/token", realm)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(form))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(json -> LoginResponse.builder()
                        .accessToken(json.get("access_token").asText())
                        .refreshToken(json.get("refresh_token").asText())
                        .expiresIn(json.get("expires_in").asLong())
                        .tokenType(json.get("token_type").asText())
                        .build());
    }
}
