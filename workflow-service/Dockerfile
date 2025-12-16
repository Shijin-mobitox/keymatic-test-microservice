FROM eclipse-temurin:21-jre

WORKDIR /app

ARG JAR_FILE=workflow-service/build/libs/workflow-service-1.0.0-SNAPSHOT.jar
COPY ${JAR_FILE} app.jar

EXPOSE 8090

ENTRYPOINT ["java","-jar","/app/app.jar"]

