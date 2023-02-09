FROM openjdk:17-alpine

COPY build/libs/AnomotBackend-1.0.0.jar /anomot.jar

CMD ["java", "-jar", "/anomot.jar"]