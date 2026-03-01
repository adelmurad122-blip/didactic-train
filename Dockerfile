# ═══════════════════════════════════════════════════════════════════
#  EGX‑AI Gateway — Production Dockerfile
#  Multi-stage build: compile → minimal runtime image
# ═══════════════════════════════════════════════════════════════════

# ── Stage 1: Build ───────────────────────────────────────────────
FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /build

# Cache Maven dependencies before copying source
COPY pom.xml .
# Download deps (offline-friendly: fails gracefully if repo is unavailable)
RUN mvn dependency:go-offline -q --no-transfer-progress 2>/dev/null || true

COPY src ./src

# Package — skip tests (run tests in CI, not in Docker build)
RUN mvn package -DskipTests --no-transfer-progress

# ── Stage 2: Layer extraction (Spring Boot layered JARs) ─────────
FROM eclipse-temurin:17-jdk-alpine AS extractor

WORKDIR /extract

COPY --from=builder /build/target/gateway.jar app.jar
# Extract layers so Docker can cache unchanged layers (deps rarely change)
RUN java -Djarmode=layertools -jar app.jar extract

# ── Stage 3: Minimal runtime image ───────────────────────────────
FROM eclipse-temurin:17-jre-alpine AS runtime

LABEL maintainer="EGX‑AI Platform Team"
LABEL version="1.0.0"
LABEL description="EGX‑AI API Gateway — Spring Cloud Gateway"

# Non-root user for security
RUN addgroup -S egxai && adduser -S egxai -G egxai

WORKDIR /app

# Copy layered JAR content in optimal cache order
COPY --from=extractor /extract/dependencies/          ./
COPY --from=extractor /extract/spring-boot-loader/    ./
COPY --from=extractor /extract/snapshot-dependencies/ ./
COPY --from=extractor /extract/application/           ./

RUN chown -R egxai:egxai /app

USER egxai

EXPOSE 8080

# Health-check so Docker/Compose knows when the service is ready
HEALTHCHECK --interval=15s --timeout=5s --start-period=40s --retries=3 \
    CMD wget -qO- http://localhost:8080/actuator/health || exit 1

# JVM tuning for containers:
#   -XX:MaxRAMPercentage   — use up to 75% of container memory for heap
#   -XX:+UseContainerSupport — respect cgroup CPU/memory limits (default in JDK 11+, explicit for clarity)
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "org.springframework.boot.loader.launch.JarLauncher"]
