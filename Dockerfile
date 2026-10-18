FROM gradle:8.10.2-jdk17 AS build
WORKDIR /app
COPY --chown=gradle:gradle . .
RUN gradle clean bootJar --no-daemon

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/build/libs/jobtracker-api-0.0.1.jar app.jar
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=65.0"
USER 10001:10001
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
