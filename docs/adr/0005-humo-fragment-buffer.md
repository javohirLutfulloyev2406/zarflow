# ADR 0005 — HUMO multi-message fragments are merged via a short in-memory buffer

## Status
Accepted

## Context
`HumoTransactionParser`/`HumoBalanceParser` assume one HUMO notification arrives as a
single message with all its lines together. In practice, forwarding a @HUMOcardbot
notification delivers it as several consecutive Telegram messages -- one per line (💸,
➖, 📍, 💳, 🕓 each separately) -- confirmed from a real forward that produced exactly
this symptom: the bot asked "which category?" three times (one per line that contained
a bare number: the amount, the card's last 4 digits, and the time's hour) and booked 12
so'm (misread from "🕓 12:00") instead of the real 47 000 so'm expense, which was never
recorded at all.

## Decision
Add `HumoFragmentDetector.looksLikeFragment` (pure, `uz.zarflow.humo`) to recognize a
line that carries a HUMO marker but didn't parse as a complete block on its own. At the
bot entry point (`TelegramUpdateService.handleTextOrHumoBalance`): try the full parse
first (unchanged, zero latency for already-complete messages); if that fails and the
line looks like a fragment, hold it in `HumoFragmentBuffer` (in-memory, per chat) instead
of handing it to quick-input. `HumoFragmentScheduler` polls every 300ms and, once a
chat's buffer has been quiet for `HumoFragmentBuffer.DEBOUNCE` (1.2s), merges its lines
back into one re-parse attempt; if the merge still doesn't parse, it falls through to
quick-input exactly as before (one entry per line), so genuinely unrelated input is never
silently dropped.

## Risk
- **In-memory, single-instance.** A restart (or, per `SchedulingConfig`'s own comment
  about ShedLock existing for multi-instance deploys, a brief overlap window during a
  rolling redeploy) mid-debounce silently drops whatever was buffered for that chat. No
  user-visible error either way -- the sender would just see nothing happen for that
  particular fragment.
- **Same-instance assumption.** If HUMO fragments for one notification land on different
  app instances (only possible during that redeploy overlap, given the webhook is a
  single Cloudflare tunnel URL), they can't be merged. Degrades to pre-fix behavior for
  that one message, not worse.
- **1.2s debounce is a guess**, not measured from real @HUMOcardbot delivery timing.

## Mitigations already in place
- The "try full parse first" step means this only engages for text that already failed
  to parse whole -- no latency or behavior change for normal quick-input or for any
  message that already arrives intact (the existing pasted-batch fixtures, for example).
- Falling through to today's per-line quick-input behavior when the merge still doesn't
  parse means worst case is unchanged from before this fix, not newly broken.

## Revisit
If @HUMOcardbot's fragment timing turns out to need a longer or shorter window than
1.2s in practice, or if horizontal scaling becomes real (not just the redeploy-overlap
case ShedLock already guards elsewhere), move the buffer into a small DB-backed table
(`humo_message_fragments`) so it survives a restart and works across instances -- same
shape as `humo_reconciliation_drafts`/`humo_import_drafts`, just keyed by chat instead of
by draft.
