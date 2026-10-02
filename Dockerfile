FROM eclipse-temurin:21-jdk-jammy AS builder

WORKDIR /workspace

COPY gradlew gradlew
COPY gradle gradle
COPY gradle.properties settings.gradle build.gradle ./
COPY common/build.gradle common/build.gradle
COPY server/build.gradle server/build.gradle
COPY client/build.gradle client/build.gradle
COPY client-fx/build.gradle client-fx/build.gradle

RUN sed -i 's/\r$//' gradlew \
    && chmod +x gradlew \
    && ./gradlew :server:dependencies --configuration runtimeClasspath --no-daemon

COPY common/src common/src
COPY server/src server/src

RUN ./gradlew :server:bootJar --no-daemon \
    && jar_file="$(find server/build/libs -maxdepth 1 -type f -name '*.jar' ! -name '*-plain.jar' -print -quit)" \
    && test -n "$jar_file" \
    && cp "$jar_file" /workspace/bomberman-server.jar

FROM eclipse-temurin:21-jre-jammy

RUN useradd --system --uid 10001 --create-home --home-dir /app bomberman

WORKDIR /app
COPY --from=builder --chown=bomberman:bomberman /workspace/bomberman-server.jar /app/bomberman-server.jar

USER bomberman
EXPOSE 8081

ENTRYPOINT ["java", "-jar", "/app/bomberman-server.jar"]
