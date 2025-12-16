FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY build/libs/gateway-service-*.jar app.jar
COPY src/main/resources/application.yml ./application.yml
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
