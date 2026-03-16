ARG BASE_IMAGE=ghcr.io/ministryofjustice/hmpps-eclipse-temurin:25-jre-jammy
FROM gradle:9-jdk25 AS builder

FROM ${BASE_IMAGE} AS runtime

FROM builder AS build
ARG BUILD_NUMBER
ENV BUILD_NUMBER=${BUILD_NUMBER:-1_0_0}
WORKDIR /app
ADD . .
RUN gradle --no-daemon assemble

FROM builder AS development
WORKDIR /app

FROM runtime AS production
LABEL maintainer="HMPPS Digital Studio <info@digital.justice.gov.uk>"

ARG BUILD_NUMBER
ENV BUILD_NUMBER=${BUILD_NUMBER:-1_0_0}

USER root
RUN apt-get update && \
    apt-get install -y --no-install-recommends curl && \
    rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY --from=build --chown=appuser:appgroup /app/build/libs/hmpps-interventions-services*.jar /app/app.jar
COPY --from=build --chown=appuser:appgroup /app/build/libs/applicationinsights-agent*.jar /app/agent.jar
COPY --from=build --chown=appuser:appgroup /app/applicationinsights.json /app

USER 2000

ENTRYPOINT ["java", "-XX:MaxHeapSize=786432000", "-XX:+AlwaysActAsServerClassMachine", "-XX:+HeapDumpOnOutOfMemoryError", "-XX:HeapDumpPath=/dumps", "-javaagent:/app/agent.jar", "-jar", "/app/app.jar"]
