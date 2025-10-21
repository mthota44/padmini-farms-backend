package com.padmini.gatewayservice.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.*;

/**
 * Represents a user payload for Keycloak Admin REST API.
 * Used during user registration from our gateway.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class KeycloakUser {

    private String username;
    private String email;
    private boolean enabled = true;
    private Object[] credentials;

    /**
     * Static factory method to build Keycloak user payload
     * with password credentials.
     */
    public static KeycloakUser of(String username, String email, String password) {
        return new KeycloakUser(
                username,
                email,
                true,
                new Object[]{
                        Map.of(
                                "type", "password",
                                "value", password,
                                "temporary", false
                        )
                }
        );
    }
}