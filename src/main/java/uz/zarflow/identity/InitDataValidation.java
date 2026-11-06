package uz.zarflow.identity;

sealed interface InitDataValidation {

    record Success(VerifiedTelegramUser user) implements InitDataValidation {
    }

    record Failure(FailureReason reason) implements InitDataValidation {
    }

}
