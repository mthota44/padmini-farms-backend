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

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class KeycloakAdminClient {

    private final WebClient keycloak;
    private final KeycloakTokenService tokenService;

    @Value("${keycloak.realm:padmini-farms}")
    private String realm;

    /**
     * Register a user → set password → remove required actions → assign role
     */
    public Mono<RegisterResponse> registerUser(RegisterRequest req) {
        return tokenService.getAdminAccessToken()
                .flatMap(adminToken ->
                        createUserInRealm(req, adminToken)
                                .flatMap(userId ->
                                        setUserPassword(userId, req.getPassword(), adminToken)
                                                .then(removeRequiredActions(userId, adminToken))
                                                .then(assignRoleToUser(userId, req.getRole()))
                                )
                )
                .onErrorResume(ex -> {
                    log.error("❌ Registration failed: {}", ex.getMessage(), ex);
                    return Mono.just(RegisterResponse.builder()
                            .message("Registration failed: " + ex.getMessage())
                            .build());
                });
    }

    /**
     * Step 1️⃣ Create the user in Keycloak
     */
    private Mono<String> createUserInRealm(RegisterRequest req, String adminToken) {
        KeycloakUser userPayload = KeycloakUser.of(req.getUsername(), req.getEmail(), req.getPassword());

        return keycloak.post()
                .uri("/admin/realms/{realm}/users", realm)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(userPayload)
                .exchangeToMono(resp -> {
                    if (resp.statusCode().is2xxSuccessful()) {
                        log.info("✅ User created successfully in Keycloak: {}", req.getUsername());
                        return fetchUserId(req.getUsername(), adminToken);
                    } else {
                        log.error("❌ Failed to create user: {}", resp.statusCode());
                        return Mono.error(new RuntimeException("Failed to create user in Keycloak"));
                    }
                });
    }

    /**
     * Step 2️⃣ Get userId by username
     */
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
                        String userId = array.get(0).get("id").asText();
                        log.info("🆔 Retrieved Keycloak userId: {}", userId);
                        return Mono.just(userId);
                    }
                    return Mono.error(new RuntimeException("User not found after creation"));
                });
    }

    /**
     * Step 3️⃣ Set user password
     */
    private Mono<Void> setUserPassword(String userId, String password, String adminToken) {
        Map<String, Object> passwordPayload = Map.of(
                "type", "password",
                "value", password,
                "temporary", false
        );

        return keycloak.put()
                .uri("/admin/realms/{realm}/users/{id}/reset-password", realm, userId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(passwordPayload)
                .retrieve()
                .toBodilessEntity()
                .then(Mono.fromRunnable(() ->
                        log.info("🔑 Password set successfully for user ID: {}", userId)
                ));
    }

    /**
     * Step 4️⃣ Remove required actions (fixes “Account not fully set up”)
     */
    private Mono<Void> removeRequiredActions(String userId, String adminToken) {
        log.info("🧹 Removing required actions for user ID: {}", userId);

        Map<String, Object> updateBody = Map.of(
                "requiredActions", new String[]{}
        );

        return keycloak.put()
                .uri("/admin/realms/{realm}/users/{id}", realm, userId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(updateBody)
                .retrieve()
                .toBodilessEntity()
                .then(Mono.fromRunnable(() ->
                        log.info("✅ Cleared required actions for user ID: {}", userId)
                ));
    }

    /**
     * Step 5️⃣ Assign role to user
     */
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
                                .thenReturn(RegisterResponse.builder()
                                        .userId(userId)
                                        .message("✅ User registered successfully with role: " + roleName)
                                        .build())))
                .doOnSuccess(res -> log.info("🎯 Assigned role {} to user {}", roleName, userId));
    }
}
