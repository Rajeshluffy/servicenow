# ServiceNow API Test Automation

BDD-style API test suite for ServiceNow's Incident Management **Table API**, built on top of [autoFrameX](https://github.com/Rajeshluffy/autoFrameX) — a shared, modular Selenium/REST test framework — and packaged for CI/CD with Docker, Jenkins, and Kubernetes.

## What it tests

Full CRUD lifecycle for ServiceNow incidents, driven entirely through the REST Table API (no UI):

- **Create** — single incident and a data-driven `Scenario Outline` creating multiple incidents from an examples table
- **Fetch** — create an incident, then retrieve it by its generated incident number (e.g. `INC0001234`), verifying end-to-end persistence
- **Update** — PUT a new short description onto an existing incident
- **Delete** — remove an incident and confirm a `204 No Content` response

Every scenario also validates the response against a JSON schema (`src/test/resources/schemas`), and an `@After` hook cleans up any incident left behind so the suite is repeatable.

See [`IncidentService.feature`](src/test/resources/features/IncidentService.feature) for the full Gherkin spec.

## Tech stack

| Layer | Tool |
|---|---|
| Language | Java 16 |
| BDD | Cucumber 7 (Gherkin, `cucumber-picocontainer` for DI into step defs) |
| Test runner | TestNG (via Cucumber's `AbstractTestNGCucumberTests`) |
| HTTP client | REST Assured (via `autoframex-api`) |
| Build | Maven |
| Shared framework | [autoFrameX](https://github.com/Rajeshluffy/autoFrameX) — `autoframex-api` + `autoframex-cucumber` modules |
| CI/CD | Jenkins declarative pipeline |
| Runtime | Docker image deployed as a one-shot Kubernetes `Job` on Minikube |

## Architecture

```
src/test/java/step/defs/som/     Cucumber step definitions (Incident*Steps.java)
src/test/java/runner/            TestNG + Cucumber runner
src/test/resources/features/     Gherkin feature files
src/test/resources/schemas/      JSON schemas for response validation
k8s/                             Namespace + Job manifests for the test run
Dockerfile                       Builds autoFrameX + this project into one test image
Jenkinsfile                      Full CI/CD pipeline (see below)
```

`IncidentScenarioContext` carries state (created sys_id, incident number) between steps within a scenario via constructor-injected DI, rather than static fields — keeping scenarios safe to run in parallel.

## CI/CD pipeline

The `Jenkinsfile` runs a 6-stage pipeline entirely self-hosted, no external registry required:

1. **Checkout** this repo and a sibling checkout of `autoFrameX` (parameterized branch)
2. **Build** a Docker image containing the built autoFrameX reactor + this project
3. **Load** the image directly into a Minikube node (`docker save` → `docker load` inside the node)
4. **Sync credentials** — ServiceNow instance username/password/OAuth client id & secret are pulled from Jenkins Credentials and synced into a Kubernetes `Secret`, never written to disk or logs
5. **Deploy** the test image as a one-shot Kubernetes `Job`
6. **Collect results** — wait for the Job to complete, stream `surefire-reports` back out of the node, publish JUnit results and archive them as build artifacts

Required Jenkins credentials (Secret text, exact IDs): `SERVICE_NOW_USERNAME`, `SERVICE_NOW_PASSWORD`, `SERVICE_NOW_CLIENT_ID`, `SERVICE_NOW_CLIENT_SECRET`.

## Running locally

```bash
# 1. Build and install the autoFrameX reactor once
git clone https://github.com/Rajeshluffy/autoFrameX.git
cd autoFrameX && mvn install -DskipTests -Djacoco.skip=true

# 2. Run this suite
cd ../serivcenow
mvn test
```

Override the suite file at runtime: `mvn test -Dtestng.suite.file=testng-smoke.xml`
