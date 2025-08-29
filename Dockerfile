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

RUN apt-get update && apt-get install -y wget && \
    wget https://truststore.pki.rds.amazonaws.com/global/global-bundle.pem -O /tmp/global-bundle.pem
RUN mkdir -p /app/certs
RUN keytool -import -alias docdb-cert -file /tmp/global-bundle.pem -keystore /app/certs/truststore.jks -storepass Goorm3project -noprompt
RUN rm /tmp/global-bundle.pem

COPY --from=builder /workspace/build/libs/*.jar /app/application.jar

EXPOSE 8084

ENTRYPOINT ["java", "-Dspring.profiles.active=prod", "-jar", "/app/application.jar"]
