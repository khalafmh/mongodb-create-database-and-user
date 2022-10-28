FROM springci/graalvm-ce:java17-0.12.x as builder
WORKDIR /workspace
COPY gradlew ./
COPY gradle ./gradle
RUN ./gradlew --console=plain
COPY build.gradle.kts settings.gradle.kts gradle.properties ./
RUN ./gradlew --console=plain dependencies
COPY . .
RUN ./gradlew --console=plain build -Dquarkus.package.type=native \
        -Dquarkus.native.additional-build-args="-H:+StaticExecutableWithDynamicLibC"


FROM debian:11-slim
WORKDIR /app/
RUN chown 1001 /app \
    && chmod "g+rwX" /app \
    && chown 1001:root /app
COPY --from=builder --chown=1001:root /workspace/build/*-runner /app/application

USER 1001

CMD ["./application"]
