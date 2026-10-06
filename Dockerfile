FROM eclipse-temurin:23-jre

WORKDIR /app

COPY target/fintrack-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]