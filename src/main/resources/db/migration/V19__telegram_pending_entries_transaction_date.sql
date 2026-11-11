-- Backdating support (docs/PRD.md §5 quick-input): carries the resolved transaction_date
-- through to the saved transaction once the user picks a category for a low-confidence draft.
ALTER TABLE telegram_pending_entries ADD COLUMN transaction_date TIMESTAMPTZ;
