FROM openjdk:8-jdk-alpine
EXPOSE 8080
RUN mvn clean install -DskipTests=true
ARG JAR_FILE=target/mawa-bes.jar
ADD ${JAR_FILE} app.jar
ENTRYPOINT ["java","-jar","/app.jar"]