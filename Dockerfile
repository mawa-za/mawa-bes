# Stage 1: Build with Maven
FROM maven:3.9.9-eclipse-temurin-17 AS build

# Set working directory
WORKDIR /home/app

# Copy source code and pom
COPY src /home/app/src
COPY pom.xml /home/app/

# Build the application
RUN mvn clean package -DskipTests -Pprod

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-jammy

# Expose application port
EXPOSE 8080

# Copy JAR from build stage
ARG JAR_FILE=/home/app/target/mawa-bes.jar
COPY --from=build ${JAR_FILE} app.jar
# Run the application
ENTRYPOINT ["java", "-jar", "/app.jar"]
