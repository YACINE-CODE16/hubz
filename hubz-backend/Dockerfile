# =============================================================================
# Hubz Backend - Production Dockerfile
# Multi-stage build for optimized production image
# =============================================================================

# --- Stage 1: Build ---
FROM maven:3.9-eclipse-temurin-21-alpine AS build

WORKDIR /app

# Copy only pom.xml first to leverage Docker cache for dependencies
COPY pom.xml .

# Download dependencies (cached unless pom.xml changes)
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application (skip tests as they should run in CI)
RUN mvn clean package -DskipTests -B -Dspring-boot.build-info.skip=false

# --- Stage 2: Runtime ---
FROM eclipse-temurin:21-jre-alpine

# Add non-root user for security
RUN addgroup -g 1001 -S hubz && \
    adduser -u 1001 -S hubz -G hubz

WORKDIR /app

# Copy the JAR from build stage
COPY --from=build /app/target/*.jar app.jar

# Create directories for uploads and logs with proper permissions
RUN mkdir -p /app/uploads /app/logs && \
    chown -R hubz:hubz /app

# Switch to non-root user
USER hubz

# Expose the application port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Set Spring profile to production
ENV SPRING_PROFILES_ACTIVE=prod

# JVM options for production
ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -Djava.security.egd=file:/dev/./urandom"

# Entry point with JVM options
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
