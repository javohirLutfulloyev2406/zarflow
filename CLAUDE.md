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

## Workflow
- Plan first, wait for approval, then implement. Small commits, one issue per branch.
- When you make a mistake I correct, add a rule here.
