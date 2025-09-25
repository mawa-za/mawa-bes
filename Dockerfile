# -------- Build Stage --------
FROM maven:3.8.7-openjdk-18 AS build

WORKDIR /home/app

# Copy app source
COPY src ./src
COPY pom.xml .

# Build with Maven
RUN mvn clean install -Pprod

# -------- Runtime Stage --------
FROM openjdk:latest

WORKDIR /app

EXPOSE 8080

# JAR from build stage
ARG JAR_FILE=/home/app/target/mawa-bes.jar
COPY --from=build ${JAR_FILE} app.jar

# Copy flyway scripts from Cloud Build cloned repo into runtime image
# (Cloud Build clones into ./flyway-scripts at build time)
COPY flyway-scripts /app/flyway/migrations

# Run app
ENTRYPOINT ["java", "-jar", "app.jar"]
