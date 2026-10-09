# I am not using the maven wrapper since task specifies docker image with maven pre-isntalled
FROM maven:3.9.16-eclipse-temurin-25-alpine AS build

WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline

COPY src src
RUN mvn clean package -Dmaven.test.skip=true

FROM eclipse-temurin:25-jre-alpine-3.24 AS runtime

WORKDIR /app
COPY --from=build /app/target/*.jar resource-processor.jar

EXPOSE 8084

CMD ["java", "-jar", "resource-processor.jar"]