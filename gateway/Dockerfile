# Build stage
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder

WORKDIR /app

COPY . /app

RUN mvn clean package -DskipTests

# Runtime stage
FROM openjdk:21-jdk

WORKDIR /app

COPY --from=builder /app/target/gateway-0.0.1-SNAPSHOT.jar /app/gateway-0.0.1-SNAPSHOT.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "gateway-0.0.1-SNAPSHOT.jar"]
