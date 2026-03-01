package com.egxai.gateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * EGX‑AI API Gateway
 *
 * <p>Single entry point for all platform microservices. Responsibilities:
 * <ul>
 *   <li>JWT validation via Keycloak (OAuth2 Resource Server)</li>
 *   <li>Dynamic routing via Eureka Service Discovery</li>
 *   <li>Rate limiting (Redis token bucket per client IP)</li>
 *   <li>CORS policy enforcement</li>
 *   <li>Request/response logging + distributed tracing</li>
 * </ul>
 */
@SpringBootApplication
@EnableDiscoveryClient
public class GatewayApplication {

    private static final Logger log = LoggerFactory.getLogger(GatewayApplication.class);

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(GatewayApplication.class);
        app.run(args);
        log.info("╔═══════════════════════════════════════╗");
        log.info("║   EGX‑AI Gateway started on port 8080  ║");
        log.info("╚═══════════════════════════════════════╝");
    }
}
