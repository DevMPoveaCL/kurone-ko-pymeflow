FROM eclipse-temurin:21-jdk-jammy AS build

WORKDIR /workspace

COPY gradle gradle
COPY gradlew build.gradle settings.gradle ./
RUN chmod +x gradlew

COPY src src
RUN ./gradlew --no-daemon bootJar \
    && cp "$(find build/libs -maxdepth 1 -name '*.jar' ! -name '*-plain.jar' -print -quit)" /tmp/app.jar

FROM eclipse-temurin:21-jre-jammy

RUN groupadd --system spring && useradd --system --gid spring --create-home spring
WORKDIR /app

COPY --from=build /tmp/app.jar app.jar
USER spring

EXPOSE 8080

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
