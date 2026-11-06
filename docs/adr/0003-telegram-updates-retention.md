# ADR 0003 — telegram_updates has no retention policy yet

## Status
Accepted

## Context
`telegram_updates` (V7) is a pure idempotency ledger: `update_id` primary key,
`received_at` timestamp, nothing else. `TelegramUpdateService.claimUpdate`
inserts a row per inbound webhook update (`ON CONFLICT DO NOTHING`) inside the
same transaction as the user registration/update it guards, so a duplicate
delivery from Telegram is a no-op. Nothing ever deletes a row.

## Decision
Ship without pruning. At current and expected near-term Phase 0/1 message
volume, the table's growth is negligible compared to the rest of the schema,
and a bot this size doesn't need a scheduled job's operational overhead yet.

## Risk
Unbounded growth. Years of `update_id` rows accumulate with no automatic
cleanup; eventually this is wasted storage and (much later) a slower primary
key index.

## Revisit
Add a scheduled delete of rows older than N days (e.g. 30) once there's
either (a) real production message volume to justify it, or (b) a general
job-scheduling mechanism already exists in the app for other reasons — don't
introduce one solely for this.
