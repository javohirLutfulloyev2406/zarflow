# zarflow

Personal finance Telegram Bot + Mini App. Full spec: [docs/PRD.md](docs/PRD.md). Brand: [docs/brand.md](docs/brand.md).

## Prerequisites

- JDK 21
- Docker and Docker Compose

## Run

```
cp .env.example .env
docker compose up
```

This starts PostgreSQL and the app (`http://localhost:8080`). Check readiness with:

```
curl http://localhost:8080/actuator/health
```

## Test

```
./gradlew test
```

Integration tests use Testcontainers and start their own disposable PostgreSQL instance — no running `docker compose` stack is required to run `./gradlew test`, only a working Docker daemon.

## Build

```
./gradlew build
```
