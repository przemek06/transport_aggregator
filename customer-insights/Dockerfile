# Build stage
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder

WORKDIR /app

COPY . /app

RUN mvn clean package -DskipTests

# Runtime stage
FROM openjdk:21-jdk

WORKDIR /app

COPY --from=builder /app/target/customer-insights-0.0.1-SNAPSHOT.jar /app/customer-insights-0.0.1-SNAPSHOT.jar

# Expose application and debug ports
EXPOSE 8095

# Enable remote debugging on port 5005
ENTRYPOINT ["java", "-jar", "customer-insights-0.0.1-SNAPSHOT.jar"]
