FROM openjdk:8-jdk-alpine
RUN apk add --no-cache docker
ADD target/AssistantMicroservice-1.0-SNAPSHOT-jar-with-dependencies.jar AssistantMicroservice.jar
ENTRYPOINT ["java","-jar", "AssistantMicroservice.jar"]