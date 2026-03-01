package com.egxai.gateway.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;

import java.util.UUID;

/**
 * Propagates a {@code X-Correlation-ID} header through the entire request chain.
 *
 * <p>If the inbound request already carries the header (e.g. from a frontend app),
 * the existing value is reused. Otherwise a new UUID is generated. The correlation ID
 * is also written to the response so clients can correlate logs.
 */
@Configuration
public class CorrelationIdFilter {

    private static final Logger log = LoggerFactory.getLogger(CorrelationIdFilter.class);
    private static final String HEADER = "X-Correlation-ID";

    @Bean
    public GlobalFilter correlationIdGlobalFilter() {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();

            // Reuse or generate
            String correlationId = request.getHeaders().getFirst(HEADER);
            if (correlationId == null || correlationId.isBlank()) {
                correlationId = UUID.randomUUID().toString();
            }

            final String cid = correlationId;

            // Inject into downstream request
            ServerHttpRequest mutatedRequest = request.mutate()
                .header(HEADER, cid)
                .build();

            // Write to response for client correlation
            ServerHttpResponse response = exchange.getResponse();
            response.getHeaders().add(HEADER, cid);

            log.debug("Correlation-ID: {} → {} {}", cid,
                      request.getMethod(), request.getPath());

            return chain.filter(exchange.mutate().request(mutatedRequest).build());
        };
    }
}
