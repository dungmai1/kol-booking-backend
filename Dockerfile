# Build stage
FROM eclipse-temurin:21-jdk-jammy AS builder

WORKDIR /workspace

# Copy Gradle files first (layer cache optimization)
COPY gradlew .
COPY gradlew.bat .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

# Download dependencies (this layer will be cached if build.gradle doesn't change)
RUN chmod +x gradlew && ./gradlew --version && ./gradlew dependencies --no-daemon 2>/dev/null || true

# Now copy source code (separate layer)
COPY src src

# Build the application (with dev profile to avoid missing properties in prod config)
RUN ./gradlew clean build -x test --no-daemon -Dspring.profiles.active=dev

# Runtime stage
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# Copy the built JAR from builder
COPY --from=builder /workspace/build/libs/*.jar app.jar

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
