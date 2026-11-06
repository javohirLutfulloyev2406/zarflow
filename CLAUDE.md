# ZarFlow
Personal finance Telegram Bot + Mini App. Full spec: docs/PRD.md. Brand: docs/brand.md.

## Commands
- Build: ./gradlew build | Test: ./gradlew test | Run: docker compose up

## Hard rules
- Money: BigDecimal / NUMERIC(19,2) only. Never double/float.
- Transfers are never income or expense. Loan principal is not an expense.
- user_id comes only from the verified JWT, never from the request body.
- Every repository query filters by user_id. No hard deletes on transactions.
- Timezone Asia/Tashkent. Modules talk via public services or events, not repositories.
- Do not add features outside the current phase (docs/PRD.md §8).


## Workflow (fast mode)
- Implement directly. Write a short plan (max 5 lines) and wait for approval ONLY for:
  new DB tables/migrations that change existing data, auth/security changes, deleting code.
- Do NOT run the full test suite or Testcontainers tests locally. CI runs them on the PR.
- Write unit tests (no Spring context, no Docker) only for: money math, date/limit formulas,
  parsers, security checks. Run only those test classes: ./gradlew test --tests "<Class>".
- Definition of done for a task:
    1. ./gradlew bootRun starts without errors (and npm run dev for miniapp changes),
    2. a quick curl / manual smoke of the new endpoint returns the expected JSON,
    3. commit + push to a feature branch, open PR. Stop there — the owner tests on the phone.
- No code-reviewer / security-auditor subagents per task; only when asked or at phase end.
- One prompt = one complete feature. Keep the final report under 15 lines:
  what was built, how to try it on the phone, known gaps.
