FROM gradle:8.8-jdk17 AS builder
WORKDIR /workspace

COPY gradlew gradlew.bat settings.gradle ./
COPY gradle ./gradle
COPY order-platform-msa-order ./order-platform-msa-order
COPY order-platform-msa-order/build.cloud.gradle ./order-platform-msa-order/build.gradle

RUN ./gradlew :order-platform-msa-order:bootJar -x test

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

COPY --from=builder /workspace/order-platform-msa-order/build/libs/*-boot.jar /app/application.jar

EXPOSE 8084
ENTRYPOINT ["java", "-Dspring.profiles.active=prod", "-jar", "/app/application.jar"]
