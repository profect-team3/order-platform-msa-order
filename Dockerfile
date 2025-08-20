FROM gradle:8.8-jdk17 AS builder
WORKDIR /workspace

COPY gradlew gradlew.bat settings.gradle build.gradle ./
COPY gradle ./gradle

COPY order-platform-msa-order ./order-platform-msa-order

# 3. 전체 프로젝트 컨텍스트에서 특정 모듈을 빌드합니다.
RUN ./gradlew :order-platform-msa-order:build -x test

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

COPY --from=builder /workspace/order-platform-msa-order/build/libs/*.jar /app/application.jar

EXPOSE 8084
ENTRYPOINT ["java", "-jar", "/app/application.jar"]
