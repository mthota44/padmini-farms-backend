package com.padmini.gatewayservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        http
                // Disable CSRF for token-based APIs
                .csrf(csrf -> csrf.disable())

                // Authorization configuration
                .authorizeExchange(auth -> auth
                        .pathMatchers("/actuator/**", "/.well-known/**", "/oauth2/**").permitAll()
                        .anyExchange().authenticated()
                )

                // Resource server (JWT) authentication
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> {}));

                // Enable OAuth2 login (PKCE or browser-based login flow)
                //.oauth2Login(oauth2 -> {});

        return http.build();
    }

    @Bean
    public ReactiveJwtDecoder jwtDecoder() {
        String jwkSetUri = "http://padmini-keycloak:8090/realms/padmini-farms/protocol/openid-connect/certs";
        return NimbusReactiveJwtDecoder.withJwkSetUri(jwkSetUri).build();
    }
}