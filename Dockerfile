FROM maven:3.8.7-openjdk-18 AS build
COPY src /home/app/src
COPY pom.xml /home/app
RUN mvn -f /home/app/pom.xml clean install -P dev

FROM openjdk:latest
EXPOSE 8080
ARG JAR_FILE=/home/app/target/mawa-bes.jar
COPY --from=build ${JAR_FILE} app.jar
ENTRYPOINT ["java","-jar","/app.jar"]