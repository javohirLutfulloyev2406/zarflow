# ADR 0004 — HUMO messages are gated by content format, not sender

## Status
Accepted

## Context
`TelegramUpdateService.handleTextOrHumoBalance` runs `HumoReconciliation` and
`HumoImport` against every non-command message the bot receives, forwarded or
pasted. `resolveForwardDate` reads `message.getForwardOrigin()` only to
extract a timestamp when one is present — it never checks the forwarder's
username against `@HUMOcardbot`. Acceptance is decided entirely by
`HumoBalanceParser`/`HumoTransactionParser` matching the HUMO message shape.

## Decision
Keep the parser as the only gate. Do not add a `forward_origin` sender
allowlist check.

- Pasted text has no `forward_origin` at all, so a sender check could only
  ever cover the forwarded path — it would not make pasted input any safer,
  and pasting is an explicitly supported input method (docs/PRD.md §2/§7).
- Every write this module makes is scoped to the caller's own `user_id` (from
  the verified JWT/Telegram login) — a user feeding it fabricated HUMO-shaped
  text can only corrupt their own ledger, not anyone else's.
- An unlinked card never books silently: `HumoImportService.process` and
  `HumoReconciliationService.process` both stop at a card-linking question
  before anything is written against a real account, giving the user a
  human-in-the-loop checkpoint regardless of where the text came from.

## Risk
Any text matching the HUMO regex shape — forwarded from somewhere else
entirely, or typed by hand — is treated as real bank data. There's no
cryptographic or identity proof that it came from HUMO at all.

## Mitigations already in place
- user_id scoping means the blast radius of fabricated input is self-inflicted
  bad data in the user's own account, never another user's.
- The card-linking and reconciliation-diff questions put a confirmation step
  between parsed text and anything that affects a real linked account.
- Dedupe fingerprinting (`HumoImportFingerprint`) still applies regardless of
  source, so replaying the same fabricated message twice doesn't double-book.

## Revisit
If a shared/multi-user ledger feature ever lets one user's HUMO input affect
another user's data, or if abuse from fabricated pasted/forwarded text is
observed in practice, add a `MessageOriginUser` username check in
`resolveForwardDate` for the forwarded path (pasted input would still need
the parser-only gate).
