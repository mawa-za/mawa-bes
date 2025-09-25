# Stage 1: Build with Maven
FROM maven:3.8.7-openjdk-18 AS build

# Copy source code
COPY src /home/app/src
COPY pom.xml /home/app

# Clone Flyway scripts into the expected location
RUN git clone https://github.com/mawa-za/mawa-flyway-scripts.git /home/app/src/main/resources/db/migration

# Build the application
RUN mvn -f /home/app/pom.xml clean install -Pprod

# Stage 2: Runtime
FROM openjdk:18-jdk-slim
EXPOSE 8080

ARG JAR_FILE=/home/app/target/mawa-bes.jar
COPY --from=build ${JAR_FILE} app.jar

ENTRYPOINT ["java", "-jar", "/app.jar"]
