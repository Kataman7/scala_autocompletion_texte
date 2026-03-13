FROM eclipse-temurin:17-jdk

# ─── sbt via sbt-launch.jar (no network required at build time) ───────────────
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# Download sbt-launch.jar (only needed once at image build time)
RUN curl -fL "https://repo1.maven.org/maven2/org/scala-sbt/sbt-launch/1.9.7/sbt-launch-1.9.7.jar" \
    -o /usr/local/lib/sbt-launch.jar

# Wrapper script so `sbt` works as a normal command
RUN printf '#!/bin/sh\nexec java -jar /usr/local/lib/sbt-launch.jar "$@"\n' \
    > /usr/local/bin/sbt && chmod +x /usr/local/bin/sbt

WORKDIR /project

# Pre-warm: resolve dependencies before copying full source
COPY project/build.properties project/build.properties
COPY build.sbt build.sbt
RUN sbt update

# Copy source
COPY . .

CMD ["sbt"]
