# ─────────────────────────────────────────────────────────────────────────
# Build context NOTE: this Dockerfile COPYs from a sibling project directory
# (../autoFrameX) because serivcenow depends on autoFrameX's autoframex-api +
# autoframex-cucumber modules — unpublished Maven artifacts with no shared
# Nexus/Artifactory in this environment (same v1 simplification as
# GPN/Dockerfile — see its header comment). Build from the workspace/ PARENT
# directory, not from serivcenow/ itself:
#
#   cd "D:\E Drive\Engineering\testleaf\workspace"
#   docker build -f serivcenow/Dockerfile -t servicenow-test .
#
# The real, longer-term fix is a shared internal Maven repository
# (Nexus/Artifactory/GitHub Packages) — this build-context workaround is a
# deliberate v1 simplification, not the end state.
#
# No Chrome/Selenium here: serivcenow is a pure REST API suite (Cucumber +
# RestAssured via autoframex-api), unlike GPN/AlfaDOCK which drive a browser.
# ─────────────────────────────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-17

LABEL org.opencontainers.image.title="serivcenow"
LABEL org.opencontainers.image.description="ServiceNow REST API test suite — Cucumber/TestNG, built on autoFrameX"

WORKDIR /app

# ── Layer 1: dependency cache — every pom, so go-offline sees the full graph ──
COPY autoFrameX/pom.xml autoFrameX/
COPY autoFrameX/autoframex-core/pom.xml autoFrameX/autoframex-core/
COPY autoFrameX/autoframex-selenium/pom.xml autoFrameX/autoframex-selenium/
COPY autoFrameX/autoframex-api/pom.xml autoFrameX/autoframex-api/
COPY autoFrameX/autoframex-database/pom.xml autoFrameX/autoframex-database/
COPY autoFrameX/autoframex-cucumber/pom.xml autoFrameX/autoframex-cucumber/
COPY autoFrameX/autoframex-performance/pom.xml autoFrameX/autoframex-performance/
COPY autoFrameX/autoframex-security/pom.xml autoFrameX/autoframex-security/
COPY autoFrameX/autoframex-testkit/pom.xml autoFrameX/autoframex-testkit/
COPY serivcenow/pom.xml serivcenow/
RUN mvn -f autoFrameX/pom.xml dependency:go-offline -q

# ── Layer 2: source — invalidated on any source change ──
COPY autoFrameX/autoframex-core/ autoFrameX/autoframex-core/
COPY autoFrameX/autoframex-selenium/ autoFrameX/autoframex-selenium/
COPY autoFrameX/autoframex-api/ autoFrameX/autoframex-api/
COPY autoFrameX/autoframex-database/ autoFrameX/autoframex-database/
COPY autoFrameX/autoframex-cucumber/ autoFrameX/autoframex-cucumber/
COPY autoFrameX/autoframex-performance/ autoFrameX/autoframex-performance/
COPY autoFrameX/autoframex-security/ autoFrameX/autoframex-security/
COPY autoFrameX/autoframex-testkit/ autoFrameX/autoframex-testkit/
COPY serivcenow/src/ serivcenow/src/
COPY serivcenow/testng.xml serivcenow/

# service.now.app.config.properties holds live credentials and is git-ignored
# (never committed — see serivcenow/.gitignore), so a fresh checkout never
# has it. Materialize it from the checked-in .example template so the
# non-secret fields (base.uri/base.path/oauth.base.path/grant.type, which
# have no env var override) are present; the 4 credential fields in the
# template are placeholders, overridden at container runtime by the
# SERVICE_NOW_* env vars below (env vars always win — see
# ConfigurationManager).
RUN cp serivcenow/src/main/resources/service.now.app.config.properties.example \
       serivcenow/src/main/resources/service.now.app.config.properties

# Install in dependency order: autoFrameX reactor -> serivcenow.
RUN mvn -f autoFrameX/pom.xml clean install -DskipTests -Djacoco.skip=true -q && \
    mvn -f serivcenow/pom.xml clean install -DskipTests -q

WORKDIR /app/serivcenow

# Credentials — never baked into the image. Supply real values via
# `docker run --env-file` or a mounted K8s Secret. ConfigurationManager reads
# these exact env var names at runtime and they take precedence over the
# .properties file.
ENV SERVICE_NOW_USERNAME=""
ENV SERVICE_NOW_PASSWORD=""
ENV SERVICE_NOW_CLIENT_ID=""
ENV SERVICE_NOW_CLIENT_SECRET=""

# Mount these volumes to retrieve test artifacts from the host after the container exits
VOLUME ["/app/serivcenow/target/surefire-reports", "/app/serivcenow/reports", "/app/serivcenow/logs"]

CMD ["mvn", "test", "-B"]
