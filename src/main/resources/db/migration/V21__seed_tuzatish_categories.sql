-- System categories for Humo balance-reconciliation corrections (docs/PRD.md §7). Kept
-- distinct from the user-facing categories seeded in V5 so they can be excluded from
-- analytics/insights/the daily limit by code (see TransactionRepository, TransactionsService).
INSERT INTO categories (code, name_uz, name_ru, type, necessity, icon, color, keywords, sort_order)
VALUES
    ('tuzatish_exp', 'Tuzatish', 'Коррекция', 'EXPENSE', 'DISCRETIONARY', 'scale', '#9AA7B8', ARRAY[]::TEXT[], 100),
    ('tuzatish_inc', 'Tuzatish', 'Коррекция', 'INCOME', NULL, 'scale', '#9AA7B8', ARRAY[]::TEXT[], 100);