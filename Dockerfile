# Root Dockerfile for Render / Docker deployments
FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /app

# Copy backend gradle wrapper and build files from demo directory
COPY demo/gradlew .
COPY demo/gradle gradle
COPY demo/build.gradle demo/settings.gradle ./

# Convert Windows line endings (CRLF -> LF) and make gradlew executable
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew

# Pre-fetch dependencies
RUN ./gradlew dependencies --no-daemon || true

# Copy backend source code
COPY demo/src src

# Build bootJar skipping tests
RUN ./gradlew bootJar -x test --no-daemon

# Stage 2: Minimal JRE runtime
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Run as non-privileged user
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy built JAR from builder stage
COPY --from=builder /app/build/libs/*.jar app.jar

ENV PORT=8080
EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java -Djava.security.egd=file:/dev/./urandom -jar app.jar"]
