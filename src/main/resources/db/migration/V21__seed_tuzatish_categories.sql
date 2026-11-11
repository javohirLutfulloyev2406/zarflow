-- System categories for Humo balance-reconciliation corrections (docs/PRD.md §7). Kept
-- distinct from the user-facing categories seeded in V5 so they can be excluded from
-- analytics/insights/the daily limit by code (see TransactionRepository, TransactionsService).
--
-- "hidden" marks a system category as usable-by-id only (CategoryRepository.findVisibleByIdForUser,
-- via Categories#requireUsable / #systemCategoryIdByCode) but never offered in the user-facing
-- picker (CategoryRepository.findVisibleForUser*, via Categories#listForUser) -- these two
-- corrections are bookkeeping, not something a user should ever pick as a transaction's category.
ALTER TABLE categories ADD COLUMN hidden BOOLEAN NOT NULL DEFAULT false;

INSERT INTO categories (code, name_uz, name_ru, type, necessity, icon, color, keywords, sort_order, hidden)
VALUES
    ('tuzatish_exp', 'Tuzatish', 'Коррекция', 'EXPENSE', 'DISCRETIONARY', 'scale', '#9AA7B8', ARRAY[]::TEXT[], 100, true),
    ('tuzatish_inc', 'Tuzatish', 'Коррекция', 'INCOME', NULL, 'scale', '#9AA7B8', ARRAY[]::TEXT[], 100, true);