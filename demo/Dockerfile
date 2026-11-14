# Multi-stage Dockerfile for Spring Boot 3.3.4 (Java 17) Backend
# Stage 1: Build the JAR with Gradle
FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /app

# Copy gradle wrapper and build files
COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./

# Convert Windows line endings (CRLF -> LF) and make gradlew executable
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew

# Pre-fetch dependencies to speed up subsequent builds
RUN ./gradlew dependencies --no-daemon || true

# Copy source code
COPY src src

# Build bootJar skipping tests
RUN ./gradlew bootJar -x test --no-daemon

# Stage 2: Minimal JRE runtime
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Run as non-privileged user for security
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy built JAR from builder stage
COPY --from=builder /app/build/libs/*.jar app.jar

# Render assigns PORT dynamically; default to 8080 locally
ENV PORT=8080
EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java -Djava.security.egd=file:/dev/./urandom -jar app.jar"]

