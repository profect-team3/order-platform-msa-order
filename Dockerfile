FROM gradle:8.8-jdk17 AS builder
WORKDIR /workspace

COPY gradlew gradlew.bat settings.gradle build.cloud.gradle ./
COPY gradle ./gradle

COPY order-platform-msa-order ./order-platform-msa-order

RUN ./gradlew :order-platform-msa-order:build -x test

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

COPY --from=builder /workspace/order-platform-msa-order/build/libs/*.jar /app/application.jar

EXPOSE 8084
ENTRYPOINT ["java", "-jar", "/app/application.jar"]
