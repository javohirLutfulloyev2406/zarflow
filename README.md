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

## Mini App (`miniapp/`)

Run the backend first (`docker compose up`), then:

```
cd miniapp
cp .env.example .env   # fill in VITE_BOT_USERNAME at least
npm install
npm run dev
```

Telegram only loads Mini Apps over HTTPS, so testing against a real Telegram client needs a
tunnel to your local dev server:

```
cloudflared tunnel --url http://localhost:5173
```

Then:
1. Set `VITE_DEV_ALLOWED_HOST` in `miniapp/.env` to the tunnel's hostname and restart `npm run dev`
   (Vite refuses unlisted hosts by design — never set this to `*`/`all`).
2. Set `ZARFLOW_MINI_APP_URL` in the backend's `.env` to the tunnel's HTTPS URL.
3. Restart the backend with `ZARFLOW_TELEGRAM_REGISTER_WEBHOOK=true` so the bot's `/start` button
   and menu button point at the tunnel.
4. Open the bot in Telegram and tap "Ilovani ochish".

### Mini App quality gates

```
npm run lint
npm run typecheck
npm test
npm run build
npm run bundle-size   # after a build — reports gzip size against the 200KB budget
```

### Capturing a real initData sample

With `VITE_DEBUG=true` set (dev server only — this panel is tree-shaken out of production
builds), a "Copy raw initData" button appears. Use it to grab a real sample for the backend's
`InitDataValidatorGoldenTest` (set it as `ZARFLOW_TEST_INIT_DATA`, alongside the real
`TELEGRAM_BOT_TOKEN`, when running `./gradlew test`).
