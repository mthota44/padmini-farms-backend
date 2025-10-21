package com.padmini.gatewayservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.padmini.gatewayservice.dto.RegisterRequest;
import com.padmini.gatewayservice.dto.RegisterResponse;
import com.padmini.gatewayservice.model.KeycloakUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class KeycloakAdminClient {

    private final WebClient keycloak;
    private final KeycloakTokenService tokenService;

    @Value("${keycloak.realm:padmini-farms}")
    private String realm;


    public Mono<RegisterResponse> registerUser(RegisterRequest req) {
        return tokenService.getAdminAccessToken()
                .flatMap(adminToken -> createUserInRealm(req, adminToken))
                .flatMap(userId -> assignRoleToUser(userId, req.getRole()))
                .onErrorResume(ex -> {
                    log.error("Registration failed: {}", ex.getMessage(), ex);
                    return Mono.just(RegisterResponse.builder()
                            .message("Registration failed: " + ex.getMessage())
                            .build());
                });
    }

    private Mono<String> createUserInRealm(RegisterRequest req, String adminToken) {
        KeycloakUser userPayload = KeycloakUser.of(req.getUsername(), req.getEmail(), req.getPassword());

        return keycloak.post()
                .uri("/admin/realms/{realm}/users", realm)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(userPayload)
                .exchangeToMono(resp -> {
                    if (resp.statusCode().is2xxSuccessful()) {
                        log.info("User created successfully in Keycloak.");
                        return fetchUserId(req.getUsername(), adminToken);
                    } else {
                        log.error("Failed to create user: {}", resp.statusCode());
                        return Mono.error(new RuntimeException("Failed to create user"));
                    }
                });
    }

    private Mono<String> fetchUserId(String username, String adminToken) {
        return keycloak.get()
                .uri(uriBuilder -> uriBuilder.path("/admin/realms/{realm}/users")
                        .queryParam("username", username)
                        .build(realm))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .flatMap(array -> {
                    if (array.isArray() && array.size() > 0) {
                        return Mono.just(array.get(0).get("id").asText());
                    }
                    return Mono.error(new RuntimeException("User not found after creation"));
                });
    }

    private Mono<RegisterResponse> assignRoleToUser(String userId, String roleName) {
        return tokenService.getAdminAccessToken()
                .flatMap(adminToken -> keycloak.get()
                        .uri("/admin/realms/{realm}/roles/{roleName}", realm, roleName)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .retrieve()
                        .bodyToMono(JsonNode.class)
                        .flatMap(role -> keycloak.post()
                                .uri("/admin/realms/{realm}/users/{id}/role-mappings/realm", realm, userId)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(new JsonNode[]{role})
                                .retrieve()
                                .toBodilessEntity()
                                .map(v -> RegisterResponse.builder()
                                        .userId(userId)
                                        .message("User registered successfully")
                                        .build())));
    }
}
