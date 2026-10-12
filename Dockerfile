# ---- Build stage ----
FROM gradle:8.10-jdk17 AS build
WORKDIR /home/gradle/src
COPY . .
RUN gradle :api:buildFatJar --no-daemon

# ---- Runtime stage ----
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /home/gradle/src/api/build/libs/app.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
