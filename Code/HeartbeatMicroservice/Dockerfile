FROM openjdk:8-jdk-alpine
RUN apk add --no-cache docker
ADD target/HeartbeatMicroservice-1.0-SNAPSHOT-jar-with-dependencies.jar HeartbeatMicroservice.jar
ENTRYPOINT ["java","-jar", "HeartbeatMicroservice.jar"]