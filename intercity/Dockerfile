# Build stage
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder
WORKDIR /app
COPY . /app
RUN mvn clean package -DskipTests

# Runtime stage (Debian-based)
FROM openjdk:21-slim

# Install dependencies and Google Chrome
RUN apt-get update && apt-get install -y \
    wget \
    gnupg \
    ca-certificates \
    fonts-liberation \
    libappindicator3-1 \
    lsb-release \
    libxss1 \
    libasound2 \
    libatk1.0-0 \
    libatk-bridge2.0-0 \
    libnss3 \
    libx11-xcb1 \
    libxcb-dri3-0 && \
    wget -q -O - https://dl.google.com/linux/linux_signing_key.pub | apt-key add - && \
    echo "deb [arch=amd64] http://dl.google.com/linux/chrome/deb/ stable main" > /etc/apt/sources.list.d/google-chrome.list && \
    apt-get update && apt-get install -y google-chrome-stable && \
    apt-get clean && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY --from=builder /app/target/intercity-0.0.1-SNAPSHOT.jar /app/intercity-0.0.1-SNAPSHOT.jar

ENTRYPOINT ["java", "-jar", "intercity-0.0.1-SNAPSHOT.jar"]
