FROM gradle:8.10-jdk21 AS builder
WORKDIR /workspace
COPY . .
RUN chmod +x gradlew && ./gradlew clean bootJar

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /workspace/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]
