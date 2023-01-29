FROM openjdk:8-jdk-alpine
EXPOSE 8080
RUN ./mvnw clean install
ARG JAR_FILE=target/mawa-bes.jar
ADD ${JAR_FILE} app.jar
ENTRYPOINT ["java","-jar","/app.jar"]