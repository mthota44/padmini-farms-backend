package com.padmini.gatewayservice.dto;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Response structure from Keycloak Token API.
 */
@Data
public class KeycloakTokenResponse {
    @JsonProperty("access_token")
    private String accessToken;

    @JsonProperty("refresh_token")
    private String refreshToken;

    @JsonProperty("expires_in")
    private long expiresIn;

    @JsonProperty("token_type")
    private String tokenType;
}