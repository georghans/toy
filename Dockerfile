# Multi-stage build for the Spring Boot application
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

# Copy Gradle wrapper and metadata first to leverage layer caching
COPY gradlew gradlew.bat settings.gradle.kts build.gradle.kts /workspace/
COPY gradle /workspace/gradle

# Ensure the wrapper is executable
RUN chmod +x gradlew

# Copy the actual source
COPY src /workspace/src

# Build a Bootable jar
RUN ./gradlew bootJar --no-daemon

# Runtime image
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create non-root user
RUN addgroup -S app && adduser -S app -G app

# Copy the built jar
COPY --from=build /workspace/build/libs/*-SNAPSHOT.jar /app/app.jar

EXPOSE 8080
USER app
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
