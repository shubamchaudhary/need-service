# Multi-stage Dockerfile for Spring Boot application
# Stage 1: Build stage
FROM gradle:8.10-jdk17-alpine AS build

# Set working directory
WORKDIR /app

# Copy gradle files first for better caching
COPY gradle gradle
COPY gradlew .
COPY build.gradle .
COPY settings.gradle .

# Download dependencies (this layer will be cached if dependencies don't change)
RUN ./gradlew dependencies --no-daemon

# Copy source code
COPY src src

# Build the application
RUN ./gradlew build --no-daemon -x test

# Stage 2: Runtime stage
FROM eclipse-temurin:17-jre-alpine

# Add a non-root user
RUN addgroup -g 1000 spring && \
    adduser -u 1000 -G spring -s /bin/sh -D spring

# Set working directory
WORKDIR /app

# Copy the JAR file from build stage
COPY --from=build /app/build/libs/need-calculation-service-*.jar app.jar

# Copy the CSV file (if needed in production)
COPY src/main/resources/forecast_data.csv /app/resources/forecast_data.csv

# Create directory for logs
RUN mkdir -p /app/logs && chown -R spring:spring /app

# Switch to non-root user
USER spring:spring

# Expose port
EXPOSE 8081

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8081/api/v1/need-calculation/health || exit 1

# Set JVM options for container environment
ENV JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]