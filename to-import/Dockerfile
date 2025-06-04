# Build stage
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder

WORKDIR /app

COPY . /app

RUN mvn clean package -DskipTests

# Runtime stage
FROM openjdk:21-jdk

WORKDIR /app

COPY --from=builder /app/target/to-import-0.0.1-SNAPSHOT.jar /app/to-import-0.0.1-SNAPSHOT.jar

# Expose application and debug ports
EXPOSE 8087

# Enable remote debugging on port 5005
ENTRYPOINT ["java", "-jar", "to-import-0.0.1-SNAPSHOT.jar"]
