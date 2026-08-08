FROM eclipse-temurin:8-jre
WORKDIR /app
COPY target/ping-service-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8011 8022
ENTRYPOINT ["java", "-jar", "app.jar"]
