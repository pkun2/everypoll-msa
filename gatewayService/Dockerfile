FROM gradle:8.5.0-jdk17-jammy AS builder

WORKDIR /app

COPY build.gradle settings.gradle gradlew ./
COPY gradle ./gradle

COPY common ./common
COPY gatewayService ./gatewayService

RUN ./gradlew :gatewayService:build --build-cache -x test

FROM openjdk:17.0.1-jdk-slim
WORKDIR /app

COPY --from=builder /app/gatewayService/build/libs/*.jar app.jar

ENV JAVA_TOOL_OPTIONS="-XX:-UseContainerSupport"

ENTRYPOINT ["java", "-jar", "app.jar"]
