# Build stage
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app

# Copy Gradle wrapper and build config
COPY gradlew .
COPY gradle gradle
COPY build.gradle.kts .
COPY settings.gradle.kts .
RUN chmod +x gradlew

# Download dependencies (cached unless build files change)
RUN ./gradlew dependencies --no-daemon || true

# Copy source and build fat JAR
COPY src src
RUN ./gradlew buildFatJar --no-daemon

# Run stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Create non-root user for security
RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup
USER appuser

# Copy the fat JAR from build stage
COPY --from=build /app/build/libs/*-all.jar app.jar

# App reads PORT from env (Railway); default 8080
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
