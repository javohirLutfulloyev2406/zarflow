# ADR 0006 — HUMO path: every message gets a reply, never fails silently

## Status
Accepted

## Context
A production message -- two transaction blocks for the same card, pasted/forwarded as
one message -- got no bot reply at all, while the bot itself kept working (`/today`
still answered). Reproduced in `HumoBalanceParserTest.doesNotMistakeAMultiBlockTransactionMessageForABalanceMessage`:
`HumoBalanceParser`'s `CARD_PATTERN` matches "HUMOCARD *" inside a *transaction*
notification's card line just as readily as inside a real balance block, and its
per-card scan window (from one "HUMOCARD *" match to the next) swept up the *second*
block's transaction amount as if it were the first card's balance. That amount's
comma-decimal format (`13.739,00`, the transaction format -- see
`HumoTransactionParser`'s own doc comment) isn't valid input for
`HumoBalanceParser.parseAmount`'s `new BigDecimal(...)`, so it threw
`NumberFormatException` -- uncaught, all the way up to `TelegramWebhookController`'s
top-level catch-all, which fires the admin alert but never replies to the user. Same
shape of problem as ADR 0004/0005: the two parsers run on every message regardless of
which one it's "really" for, and only content format tells them apart -- here, that
disambiguation was incomplete.

## Decision
1. **Fix the misdetection.** `AMOUNT_PATTERN` now requires the 💵 marker that every real
   balance block uses (`balance.txt` fixture) before the number, instead of matching any
   "number UZS" in the scan window. A transaction's amount line is always marked ➖/➕,
   never 💵, so this fully separates the two formats.
2. **Hard rule: every message that reaches `tryHumo` gets a reply.** Wrapped the
   balance+import parse-and-send step in a catch-all: on any exception, log it, reply
   with `bot.humo-unreadable` ("Xabarni o'qib bo'lmadi, formatni tekshiring."), and
   publish `AdminAlertRequested("humo-import", ex)` -- regardless of whether the
   webhook's own top-level catch would otherwise have swallowed it with no user-facing
   reply. This covers both the live per-message path and the fragment-buffer flush path
   (`TelegramUpdateService.processBufferedHumoFragments`), since both call `tryHumo`.
3. **Unique violation on the fingerprint counts as a duplicate, not a crash.**
   `HumoImportService.claim` already used `ON CONFLICT DO NOTHING`, which shouldn't throw
   on a genuine duplicate -- but if it somehow does, catching the exception in Java alone
   isn't enough: Postgres aborts the *whole* surrounding `@Transactional process()` call
   on any statement failure, poisoning every block still left in that batch. `claim` now
   runs the insert in its own `PROPAGATION_NESTED` (savepoint) transaction, so a
   `DataIntegrityViolationException` there rolls back only that claim and is treated as
   "already imported" -- the rest of the batch keeps going.

## Risk
- The fragment-buffer's timer-based flush (`HumoFragmentScheduler`, `@Scheduled(fixedDelay
  = 300)`, added in ADR 0005) already doesn't depend on a next incoming message to fire --
  confirmed again here since this was explicitly re-checked while investigating. No change
  needed there, but noted because this incident report asked about it directly.
- The savepoint approach for `claim` depends on the JDBC driver/transaction manager
  supporting savepoints, which the Postgres driver does -- untested against a real DB
  locally (project convention: no Testcontainers here, CI covers it).
- The catch-all in `tryHumo` is intentionally broad (`Exception`, not a narrower type) --
  correct for "never fail silently", but it also means a bug that *should* crash loudly
  during development will instead quietly produce a user-facing fallback + admin alert.
  Acceptable trade-off for a bot a non-technical relative might also use.

## Revisit
If another "parser A mistakes parser B's input" bug shows up for a third shape of
message, consider a shared pre-check (e.g., "does this line's marker set belong to
transactions, balance, or neither") instead of patching each parser's regex
independently again.
