FROM maven:3.8.7-openjdk-18 AS build
COPY src /home/app/src
COPY pom.xml /home/app
RUN mvn -f /home/app/pom.xml clean install

FROM openjdk:latest
EXPOSE 8080
ARG JAR_FILE=target/mawa-bes.jar
ADD ${JAR_FILE} app.jar
ENTRYPOINT ["java","-jar","/app.jar"]