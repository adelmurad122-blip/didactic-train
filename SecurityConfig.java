package com.egxai.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Reactive Security Configuration.
 *
 * <p>All inbound requests must carry a valid Keycloak-issued JWT except:
 * <ul>
 *   <li>{@code GET /actuator/health}  — liveness / readiness probes</li>
 *   <li>{@code OPTIONS  /**}          — CORS preflight</li>
 * </ul>
 *
 * <p>JWT validation is done locally (JWKS endpoint cached from Keycloak) — no
 * round-trip to the auth server on each request.
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    /** Comma-separated list of allowed CORS origins (injected from application.yml). */
    @Value("${gateway.cors.allowed-origins:http://localhost:3000}")
    private List<String> allowedOrigins;

    @Bean
    public SecurityWebFilterChain filterChain(ServerHttpSecurity http) {
        http
            // CSRF is not applicable for stateless REST/JWT APIs
            .csrf(ServerHttpSecurity.CsrfSpec::disable)

            // CORS — delegate to the bean below
            .cors(corsSpec -> corsSpec.configurationSource(corsConfigurationSource()))

            // Authorization rules
            .authorizeExchange(ex -> ex
                // Infrastructure probes — always public
                .pathMatchers(HttpMethod.GET,  "/actuator/health",
                                               "/actuator/info").permitAll()
                // Prometheus scrape endpoint — restrict in production via network policy
                .pathMatchers(HttpMethod.GET,  "/actuator/prometheus").permitAll()
                // CORS preflight
                .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                // Everything else requires a valid token
                .anyExchange().authenticated()
            )

            // OAuth2 Resource Server — JWT mode
            // Keycloak JWKS URI is configured in application.yml
            .oauth2ResourceServer(oauth -> oauth
                .jwt(Customizer.withDefaults())
            );

        return http.build();
    }

    /**
     * CORS policy applied at the Gateway level so downstream services
     * do NOT need to configure CORS individually.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With",
                                          "X-Correlation-ID"));
        config.setExposedHeaders(List.of("X-Correlation-ID", "X-RateLimit-Remaining"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L); // 1 hour preflight cache

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
