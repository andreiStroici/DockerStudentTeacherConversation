FROM openjdk:8-jdk-alpine
RUN apk add --no-cache docker
ADD target/TeacherMicroservice-1.0-SNAPSHOT-jar-with-dependencies.jar TeacherMicroservice.jar
ADD questions_database.txt questions_database.txt
ADD grade.txt grade.txt
ENTRYPOINT ["java","-jar", "TeacherMicroservice.jar"]