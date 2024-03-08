FROM --platform=${BUILDPLATFORM:-linux/amd64} eclipse-temurin:21-jre-jammy AS builder

WORKDIR /app

ENV JAVA_OPTS="-Djdk.lang.Process.launchMechanism=vfork"

# download most dependencies
# exclude generateGitProperties -- .git folder is not copied to allow caching
COPY build.gradle.kts settings.gradle.kts gradlew ./
COPY gradle/ gradle/
RUN ./gradlew classes --exclude-task=generateGitProperties

# compile main app
# exclude generateGitProperties -- .git folder is not copied to allow caching
COPY src/main/ src/main/
RUN ./gradlew classes --exclude-task=generateGitProperties

# assemble extracts information from .git and BUILD_NUMBER env var, these layers change for all commits
ARG BUILD_NUMBER
ENV BUILD_NUMBER ${BUILD_NUMBER:-1_0_0}
COPY . .
RUN ./gradlew --no-daemon assemble


# ---
FROM --platform=${BUILDPLATFORM:-linux/amd64} eclipse-temurin:17.0.10_7-jre-focal AS final
LABEL maintainer="HMPPS Digital Studio <info@digital.justice.gov.uk>"

# force a rebuild of `apk upgrade` below by invalidating the BUILD_NUMBER env variable on every commit
ARG BUILD_NUMBER
ENV BUILD_NUMBER ${BUILD_NUMBER:-1_0_0}

RUN apt-get update && \
    apt-get -y upgrade && \
    apt-get install -y \
      curl \
      tzdata \
    && rm -rf /var/lib/apt/lists/*

ENV TZ=Europe/London
RUN ln -snf "/usr/share/zoneinfo/$TZ" /etc/localtime && echo "$TZ" > /etc/timezone

RUN addgroup --gid 2000 --system appgroup && \
    adduser --uid 2000 --system appuser && \
    adduser appuser appgroup

WORKDIR /app
COPY --from=builder --chown=appuser:appgroup /app/build/libs/hmpps-interventions-service*.jar /app/app.jar
COPY --from=builder --chown=appuser:appgroup /app/build/libs/applicationinsights-agent*.jar /app/agent.jar
COPY --chown=appuser:appgroup applicationinsights.json /app

USER 2000

ENTRYPOINT ["java", "-XX:MaxHeapSize=786432000", "-XX:+AlwaysActAsServerClassMachine", "-XX:+HeapDumpOnOutOfMemoryError", "-XX:HeapDumpPath=/dumps", "-javaagent:/app/agent.jar", "-jar", "/app/app.jar"]
