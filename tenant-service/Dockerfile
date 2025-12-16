# Multi-stage build: First stage builds the JAR
FROM gradle:8.5-jdk21-alpine AS build
WORKDIR /workspace
# Copy root build files
COPY settings.gradle build.gradle ./
# Copy shared module
COPY shared ./shared
# Copy tenant-service module
COPY tenant-service/build.gradle ./tenant-service/
COPY tenant-service/src ./tenant-service/src
# Build the project
RUN gradle :tenant-service:clean :tenant-service:build -x test --no-daemon

# Second stage: Runtime image
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /workspace/tenant-service/build/libs/tenant-service-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
