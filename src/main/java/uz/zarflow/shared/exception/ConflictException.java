package uz.zarflow.shared.exception;

public class ConflictException extends ZarFlowException {

    public ConflictException(String errorCode, Object... args) {
        super(errorCode, args);
    }

}
