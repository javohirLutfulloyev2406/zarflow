package uz.zarflow.transaction;

public enum TransactionType {
    INCOME,
    EXPENSE,
    TRANSFER,
    /** A loan/credit installment -- reduces balance like EXPENSE, but is never counted as
     * expense in analytics or against the daily safe-to-spend limit (docs/PRD.md M7). */
    DEBT_PAYMENT
}
