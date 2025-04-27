# Build stage
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder
WORKDIR /app
COPY . /app
RUN mvn clean package -DskipTests

# Run stage
FROM openjdk:21-slim
WORKDIR /app
COPY --from=builder /app/target/flixbus-0.0.1-SNAPSHOT.jar /app/flixbus-0.0.1-SNAPSHOT.jar

ENTRYPOINT ["java", "-jar", "flixbus-0.0.1-SNAPSHOT.jar"]
