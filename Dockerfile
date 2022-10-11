FROM openjdk:17-alpine

COPY build/libs/AnomotBackend-0.0.1-SNAPSHOT.jar /anomot.jar

CMD ["java", "-jar", "/anomot.jar"]