# ADR 0002 — initData max age (24h)

## Status
Accepted

## Context
Telegram Mini App `initData` carries an `auth_date` field. Telegram's own
"Validating data received via the Mini App" documentation recommends treating
`initData` older than 24 hours as untrusted. `InitDataValidator` enforces this
via `zarflow.auth.init-data-max-age`, defaulting to `PT24H`.

## Decision
Use Telegram's own recommended 24-hour window as the default, rather than a
shorter custom one. This is the officially documented validation period, not
an invention of this codebase.

## Risk
Within that 24-hour window, a leaked `initData` string (captured from, say, a
compromised intermediary or shared logs somewhere outside our control) remains
a fully valid, replayable login credential. There is no server-side single-use
or nonce tracking of `initData` — the signature and `auth_date` are the only
checks performed.

## Mitigations already in place
- `initData` only ever travels over HTTPS.
- `initData` (and its hash) is never logged — `TelegramLoginService` logs only
  the `FailureReason` enum on a failed validation, never the raw string.
- JWT TTL is short (`zarflow.auth.jwt-ttl`, default `PT1H`), so even a
  successful replay only buys a short-lived session, not standing access.
- No refresh tokens: every session re-proves itself with fresh `initData` once
  the JWT expires, rather than extending access indefinitely.

## Revisit
Phase 5 (Public Product), together with rate limiting on
`POST /api/v1/auth/telegram`. At that point, reconsider shortening the window
and/or adding single-use tracking (e.g. a short-TTL cache of consumed hashes)
if real-world usage patterns justify the added complexity.
