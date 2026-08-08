FROM eclipse-temurin:8-jre
WORKDIR /app
COPY target/pong-service-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8033
ENTRYPOINT ["java", "-jar", "app.jar"]
