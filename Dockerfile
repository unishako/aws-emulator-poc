FROM gradle:9-jdk25 AS build
WORKDIR /src
COPY build.gradle.kts settings.gradle.kts ./
COPY src src
RUN gradle bootJar --no-daemon

FROM eclipse-temurin:25-jre
COPY --from=build /src/build/libs/app.jar /app/app.jar
COPY docker/entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh
USER 1000
ENTRYPOINT ["/entrypoint.sh"]
