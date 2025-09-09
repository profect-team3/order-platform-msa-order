FROM gradle:8.8-jdk17 AS builder

WORKDIR /workspace

COPY gradlew .
COPY gradlew.bat .
COPY gradle ./gradle

COPY build.cloud.gradle build.gradle
COPY settings.gradle .

COPY src ./src
COPY libs ./libs

RUN ./gradlew bootJar

FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

COPY --from=builder /workspace/build/libs/*.jar /app/application.jar
RUN apt-get update && apt-get install -y wget \
    && wget https://truststore.pki.rds.amazonaws.com/global/global-bundle.pem -O /app/global-bundle.pem \
    && apt-get remove -y wget \
    && rm -rf /var/lib/apt/lists/*

EXPOSE 8084

ENTRYPOINT ["java", "-Dspring.profiles.active=prod", "-jar", "/app/application.jar"]