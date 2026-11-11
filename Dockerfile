# Pinned to the builder's native arch: the jar is architecture-independent bytecode,
# so there's no reason to run the Gradle build itself under QEMU emulation for arm64.
FROM --platform=$BUILDPLATFORM eclipse-temurin:21-jdk AS build
WORKDIR /workspace
COPY gradlew gradlew.bat ./
COPY gradle gradle
COPY build.gradle.kts settings.gradle.kts ./
COPY src src
RUN chmod +x gradlew && ./gradlew bootJar --no-daemon

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S zarflow && adduser -S zarflow -G zarflow
WORKDIR /app
COPY --from=build /workspace/build/libs/*.jar app.jar
USER zarflow
EXPOSE 8080
ENV JAVA_OPTS="-Xmx384m -Xss512k -XX:MaxMetaspaceSize=128m -XX:+UseSerialGC -XX:TieredStopAtLevel=1"
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
