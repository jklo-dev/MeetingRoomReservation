# ---------- Etapa 1: construccion ----------
FROM eclipse-temurin:21-jdk AS build

WORKDIR /build

COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle gradle

RUN chmod +x gradlew && ./gradlew --no-daemon dependencies

COPY src src

RUN ./gradlew --no-daemon bootJar -x test

# ---------- Etapa 2: ejecucion ----------
FROM eclipse-temurin:21-jre AS runtime

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

RUN useradd --system --no-create-home --shell /usr/sbin/nologin app

WORKDIR /app

COPY --from=build --chown=app:app /build/build/libs/*.jar app.jar

USER app

EXPOSE 8080

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
