# DockerStudentTeacherConversation

## Capition
1. [Introduction](#1-introduction)
2. [Techincal Aspects](#2-techincal-apsects)  
    1. [Micorservices archiecture](#1-micorservices-archiecture)  
    2. [ Docker Containers - Technical Overview](#2-docker-containers---technical-overview) 
3. [Usecase Diagram](#3-usecase-diagram)
4. [Microservices](#4-microservices)
    1. [HeartbeatMicroservice](#1-heartbeatmicroservice)
    2. [MessageManager Microservice](#2-messagemanager-microservice)
    3. [StudentMicroservice](#3-studentmicroservice)
    4. [AssistantMicroservice](#4-assistantmicroservice)
    5. [TeacherMicroservice](#5-teachermicroservice)
5. [Activity Diagram](#5-activity-diagram)
6. [Class Diagram](#6-class-diagram)
---

## 1. Introduction

This project illustrates a simple communication model between a teacher and multiple students using a microservices architecture. Each component of the system is implemented as an independent microservice and containerized using Docker.

The primary goal of this project is to demonstrate how Docker can be used to coordinate and manage microservices effectively, showcasing the principles of containerization, isolation, and service orchestration. 

---

## 2. Techincal Apsects

This section explains the most important technical aspects of the project.

### 1. Micorservices archiecture

**Microservices Architecture (MSA)** is an architectural style that structures an application as a collection of loosely coupled, independently deployable services. Each service is responsible for a distinct business capability and communicates with others using lightweight protocols, typically HTTP or messaging systems.

---

### Key Characteristics (Fowler & Lewis, 2014):

- **Componentization via Services**  
  Each part of the system is a separate service, which can be deployed independently and developed in isolation.

- **Business-Oriented Organization**  
  Teams are organized around business capabilities rather than technical layers (e.g., “Order Management” instead of “Database Layer”).

- **Independent Deployment**  
  Each service can be deployed without affecting the others, enabling continuous delivery and rapid iterations.

- **Decentralized Data Management**  
  Each service manages its own database or data store, promoting autonomy and avoiding shared database bottlenecks.

- **Built for Failure**  
  Microservices embrace the reality of failure. They use patterns like circuit breakers and retries to build fault-tolerant systems.

- **Infrastructure Automation**  
  Continuous integration, automated testing, and deployment pipelines are essential to manage the complexity of microservices.

---

### 2. Docker Containers - Technical Overview

Docker containers are lightweight, standalone, and executable software packages that include everything needed to run an application: code, runtime, system tools, libraries, and configuration. Containers are isolated from one another and from the host system, ensuring consistency and reproducibility across environments.

### Key Characteristics

- **Isolation**: Each container runs in its own isolated environment, providing security and stability.
- **Portability**: Containers encapsulate both the application and its dependencies, allowing them to run consistently across different platforms and environments.
- **Efficiency**: Containers share the host operating system kernel, making them more lightweight and faster to start compared to virtual machines.
- **Ephemerality**: Containers can be quickly created, replicated, stopped, and removed as needed.

### Container vs. Image

- A **Docker image** is a read-only blueprint that defines the contents and configuration of the container, including the application code and runtime environment.
- A **Docker container** is a runtime instance of an image. It can be started, modified, and stopped independently of the original image.

### Container Lifecycle

1. **Build** – Define the image using a `Dockerfile`.
2. **Run** – Start a container from the image using the `docker run` command.
3. **Stop/Restart** – Manage the container's state with commands like `docker stop` and `docker start`.
4. **Remove** – Delete unused containers using `docker rm` to clean up resources.

---

## 3. Usecase Diagram

In the image below, the use case diagram is presented:

![Use case diagram](/Images/Usecase.png)

The diagram includes three actors: **student**, **teacher**, and **assistant**.

The **student** and **teacher** share the same main functionalities: asking questions and providing responses.

The **assistant** intermediates communication between the student and the teacher and verifies the identity of the student to ensure that questions and responses come from a legitimate source.

---

## 4. Microservices

This section presents the microservices used in the system.

### 1. HeartbeatMicroservice

This microservice implements a **heartbeat mechanism** that periodically checks whether subscribed containers are alive. If a container becomes unresponsive, the microservice attempts to restart it.

#### Responsibilities

- **`add subscriber`**  

  Accepts a connection from a new subscriber and adds it to the internal subscriber list.

- **`remove subscriber`**  

  Removes a subscriber when it gracefully closes its connection with this microservice.

- **`evaluate`**  

  Periodically (every 5 seconds) iterates through the list of subscribers to check their availability. If a subscriber is unresponsive, the microservice tries to restart it.

#### Technologies Used

- Docker (with custom network for inter-container communication)
- Kotlin  

#### SOLID Principles Applied

- **`Single Responsibility Principle (SRP)`**  

  The microservice has a single well-defined responsibility: managing the heartbeat mechanism and monitoring container availability.

- **`Interface Segregation Principle`**  

  The microservice exposes only the minimal and necessary operations required to manage subscribers and monitor their health.

- **`Inversion of Control (IoC)`**  

  Communication with external components is handled via TCP connections, decoupling the microservice logic from direct service dependencies.

### 2. MessageManager Microservice

This microservice is responsible for redirecting messages between the `AssistantMicroservice`, the `StudentMicroservice`, and between multiple instances of the `StudentMicroservice`.

#### Responsibilities

- **`add subscriber`**  

  Accepts a connection from a new subscriber and adds it to the internal subscriber list.

- **`remove subscriber`**  

  Removes a subscriber when it gracefully closes its connection with this microservice.

- **`broadcast`**  

  Sends a message to all connected subscribers except the one that originally sent the message.

- **`send to`**  

  Sends a message directly to a specific destination in the network.

#### Technologies Used

- Docker (with custom network for inter-container communication)  
- Kotlin  

#### SOLID Principles Applied

- **`Single Responsibility Principle (SRP)`**  

  The microservice has a single well-defined responsibility: redirecting messages within the custom network.

- **`Interface Segregation Principle`**  

  The microservice exposes only the minimal and necessary operations required for message redirection.

- **`Inversion of Control (IoC)`**  

  Communication with external components is handled via TCP connections, decoupling the microservice logic from direct service dependencies.

### 3. StudentMicroservice

This microservice models the student's actions: asking and responding to questions.

#### Responsibilities

- **`subscribe`**  
  Allows the `StudentMicroservice` to subscribe to both the `MessageManagerMicroservice`, `AssistantMicroservice`, and the `HeartbeatMicroservice`.

- **`ask`**  
  Enables the student to send a question to either the `TeacherMicroservice` or another student.

- **`respond`**  
  Sends a response to a received question by forwarding a message through the `MessageManagerMicroservice`.

#### Technologies Used

- Docker (with custom network for inter-container communication)  
- Kotlin  

#### SOLID Principles Applied

- **`Single Responsibility Principle (SRP)`**  
  The microservice has a single well-defined responsibility: managing question-and-answer interactions for the student.

- **`Interface Segregation Principle`**  
  The microservice exposes only the essential operations required for communication and interaction.

- **`Inversion of Control (IoC)`**  
  Communication with external components is handled via TCP connections, decoupling internal logic from direct dependencies.

Additionally, a graphical interface was implemented to allow the student to send questions or requests to the others students or to  the teacher.

### 4. AssistantMicroservice

This microservice represents the teacher assistant and ensures that questions and responses sent to the teacher come from valid students. It intermediates communication between the `TeacherMicroservice` and the `MessageManagerMicroservice`.

#### Responsibilities

- **`add subscriber`**  

  Allows the assistant to monitor student connections and identify which student is sending a message.

- **`remove subscriber`**  

  Removes a subscriber when it gracefully closes its connection with this microservice.

- **`send message`** 

  Sends a message to either the `TeacherMicroservice` or the `MessageManagerMicroservice`, depending on the context.

- **`check idenity`**

  Checks the identity of the student that sends a message to make sure that only if the student id is in a list of students.

#### Technologies Used

- Docker (with custom network for inter-container communication)  
- Kotlin  

#### SOLID Principles Applied

- **`Single Responsibility Principle (SRP)`**  

  The microservice has a single well-defined responsibility: authenticating and mediating communication between students and the `TeacherMicroservice`.

- **`Interface Segregation Principle`**  

  The microservice exposes only the essential operations needed for communication handling.

- **`Inversion of Control (IoC)`**  

  Communication with external components is handled via TCP connections, decoupling the microservice's internal logic from external service dependencies.

### 5. TeacherMicroservice

This microservice represents the teacher that can ask questions and response to questions and evaluates students. 

#### Responsibilities

- **`subscribe`**

  This responsibility is used to make a connection with the AssitantMicroservice.

- **`ask question`**

  This responsibility is used to put a question to the students.

- **`response question`**

  This responsibility is used to offer a response to a question, if the teacher knows (based on a text file with responses).

- **`evaluate`**

  This responsibility lets the teacher to grade the students.

#### Technologies Used

- Docker (with custom network for inter-container communication)  
- Kotlin  

#### SOLID Principles Applied

- **`Single Responsibility Principle (SRP)`**  

  The microservice has a single well-defined responsibility: it implements teacher attributes: evaluate students, ask questions, and responde to the student questions.

- **`Interface Segregation Principle`**  

  The microservice exposes only the essential operations required for communication and interaction.

- **`Inversion of Control (IoC)`**  

  Communication with external components is handled via TCP connections, decoupling internal logic from direct dependencies.

---

## 5. Activity Diagram

To improve clarity, a separate activity diagram is provided for each microservice.

#### 1. StudentMicroservice

![Student activity diagram](/Images/Student.png)

#### 2. TeacherMicroservice

![Teacher activity diagram](/Images/Teacher.png)

#### 3. AssistantMicroservice

![Assistant activity diagram](/Images/Assistantpng.png)

#### 4. MessageManager Microservice

![MessageManager activity diagram](/Images/MessageManager.png)

#### 5. HeartbeatMicroservice

![Heartbeat activity diagram](/Images/Heartbeat.png)

---

## 6. Class Diagram

For the implementation of the microservices, an object-oriented programming (OOP) approach was chosen, where each microservice is structured around classes and objects.

In the following, the class diagram for each microservice is presented.

#### 1. StudentMicroservice

![StudentMicroservice class diagram](/Images/StudentClass.png)

#### 2. TeacherMicroservice

![TeacherMicroservice class diagram](/Images/TeacherClass.png)

#### 3. AssistantMicroservice

![AssistantMicroservice](/Images/AssistantMicroserviceClass.png)

#### 4. MessageManager Microservice

![MessageManager Microservice class diagram](/Images/MessageManagerClass.png)

#### 5. HeartbeatMicroservice

![HeartbeatMicroservice](/Images/HeartbeatClass.png)

---

## Bibiliography

- Martin Fowler & James Lewis. ["Microservices"](https://martinfowler.com/articles/microservices.html), March 2014.

- Docker Documentation. *What is a Container?*. Available at: [https://docs.docker.com/get-started/overview/](https://docs.docker.com/get-started/overview/)

- Docker Docs. *Docker CLI Reference*. Available at: [https://docs.docker.com/engine/reference/commandline/cli/](https://docs.docker.com/engine/reference/commandline/cli/)

- Docker Documentation. *Get Started with Docker*. Available at: [https://docs.docker.com/get-started/](https://docs.docker.com/get-started/)

- Merkel, Dirk. “Docker: Lightweight Linux Containers for Consistent Development and Deployment.” *Linux Journal*, vol. 2014, no. 239, 2014.

- Pahl, Claus. “Containerization and the PaaS Cloud.” *IEEE Cloud Computing*, vol. 2, no. 3, 2015, pp. 24–31. doi:10.1109/MCC.2015.51
