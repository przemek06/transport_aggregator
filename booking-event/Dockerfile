# Build stage
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder

WORKDIR /app

COPY . /app

RUN mvn clean package -DskipTests

# Runtime stage
FROM openjdk:21-jdk

WORKDIR /app

COPY --from=builder /app/target/booking-event-0.0.1-SNAPSHOT.jar /app/booking-event-0.0.1-SNAPSHOT.jar

# Expose application and debug ports
EXPOSE 8091

# Enable remote debugging on port 5005
ENTRYPOINT ["java", "-jar", "booking-event-0.0.1-SNAPSHOT.jar"]
