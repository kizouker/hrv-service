# ── Stage 1: build ────────────────────────────────────────────────────────────
# Maven image already contains mvn (no apk install). pom.xml is copied first so
# dependency downloads are cached in their own layer and re-used across builds.
FROM maven:3.9.9-eclipse-temurin-21 AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn -q dependency:go-offline
COPY src ./src
RUN mvn -q package -DskipTests

# ── Stage 2: run ──────────────────────────────────────────────────────────────
# Slim JRE-only image, running as a non-root user (Azure security best practice).
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S app && adduser -S app -G app
COPY --from=builder /app/target/*.jar app.jar
RUN chown app:app app.jar
USER app
EXPOSE 8080
# Spring Boot reads ${PORT} (see application.yml); Azure Container Apps routes
# to targetPort 8080 and /actuator/health is permitAll in SecurityConfig.
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health | grep -q '"status":"UP"' || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
