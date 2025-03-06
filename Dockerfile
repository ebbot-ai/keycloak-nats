# Build
FROM openjdk:21-slim

USER root

RUN apt-get update && apt-get install -y --no-install-recommends \
	git \
	curl \
	tar \
	maven

VOLUME /keycloak-nats-main
COPY . /keycloak-nats-main
RUN cd /keycloak-nats-main && ./gradlew shadowJar

RUN ls -la /keycloak-nats-main/build/libs

