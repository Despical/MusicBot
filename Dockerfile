FROM eclipse-temurin:25-jdk-noble AS build

WORKDIR /workspace

COPY gradle gradle
COPY gradlew settings.gradle build.gradle ./
COPY src src

RUN chmod +x gradlew && ./gradlew --no-daemon installDist

FROM eclipse-temurin:25-jre-noble

WORKDIR /opt/musicbot

RUN useradd --create-home --home-dir /opt/musicbot --shell /usr/sbin/nologin musicbot

COPY --from=build /workspace/build/install/MusicBot/ /opt/musicbot/

RUN mkdir -p /opt/musicbot/data && chown -R musicbot:musicbot /opt/musicbot

USER musicbot

VOLUME ["/opt/musicbot/data"]

ENTRYPOINT ["/opt/musicbot/bin/MusicBot"]
