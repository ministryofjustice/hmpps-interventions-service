FROM --platform=${BUILDPLATFORM:-linux/amd64} ghcr.io/ministryofjustice/hmpps-eclipse-temurin:25-jre-jammy AS builder

WORKDIR /app
USER root
ENV GRADLE_USER_HOME=/tmp/gradle
RUN mkdir -p "$GRADLE_USER_HOME" && chmod -R 755 "$GRADLE_USER_HOME"

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
ENV BUILD_NUMBER=${BUILD_NUMBER:-1_0_0}
COPY . .
RUN ./gradlew assemble


# ---
FROM --platform=${BUILDPLATFORM:-linux/amd64} ghcr.io/ministryofjustice/hmpps-eclipse-temurin:25-jre-jammy AS final
LABEL maintainer="HMPPS Digital Studio <info@digital.justice.gov.uk>"
USER root

# ensure apt directories exist and are writable
RUN mkdir -p /var/lib/apt/lists/partial

# force a rebuild of `apk upgrade` below by invalidating the BUILD_NUMBER env variable on every commit
ARG BUILD_NUMBER
ENV BUILD_NUMBER=${BUILD_NUMBER:-1_0_0}

RUN apt-get update && \
    apt-get -y upgrade && \
    apt-get install -y \
      curl \
      tzdata \
    && rm -rf /var/lib/apt/lists/*

ENV TZ=Europe/London
RUN ln -snf "/usr/share/zoneinfo/$TZ" /etc/localtime && echo "$TZ" > /etc/timezone

RUN set -eux; \
    if ! getent group appgroup >/dev/null; then addgroup --gid 2000 --system appgroup; fi; \
    if ! id -u appuser >/dev/null 2>&1; then adduser --uid 2000 --system --ingroup appgroup appuser; fi; \
    usermod -a -G appgroup appuser

WORKDIR /app
COPY --from=builder --chown=appuser:appgroup /app/build/libs/hmpps-interventions-service*.jar /app/app.jar
COPY --from=builder --chown=appuser:appgroup /app/build/libs/applicationinsights-agent*.jar /app/agent.jar
COPY --chown=appuser:appgroup applicationinsights.json /app

USER 2000

ENTRYPOINT ["java", "-XX:MaxHeapSize=786432000", "-XX:+AlwaysActAsServerClassMachine", "-XX:+HeapDumpOnOutOfMemoryError", "-XX:HeapDumpPath=/dumps", "-javaagent:/app/agent.jar", "-jar", "/app/app.jar"]
