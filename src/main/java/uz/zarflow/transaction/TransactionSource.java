package uz.zarflow.transaction;

enum TransactionSource {
    BOT,
    MINI_APP,
    RECURRING,
    /** Imported from a bank's own notification message (docs/PRD.md HUMO import) -- never
     * counted as a "manual" transaction when checking for possible duplicate entries. */
    BANK_IMPORT
}
