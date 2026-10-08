# ==========================================
# Build stage
# ==========================================

FROM maven:3.9-eclipse-temurin-23 AS build

WORKDIR /app

COPY pom.xml .

COPY src ./src

RUN mvn clean package -DskipTests


# ==========================================
# Runtime stage
# ==========================================

FROM eclipse-temurin:23-jre

WORKDIR /app

COPY --from=build /app/target/fintrack-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-Xms64m", "-Xmx192m", "-XX:+UseSerialGC", "-jar", "app.jar"]