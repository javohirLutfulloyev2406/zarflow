package uz.zarflow.identity;

enum FailureReason {
    MISSING_HASH,
    BAD_SIGNATURE,
    EXPIRED,
    MALFORMED
}
