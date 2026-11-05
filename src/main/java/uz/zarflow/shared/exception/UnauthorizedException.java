package uz.zarflow.shared.exception;

public class UnauthorizedException extends ZarFlowException {

    public UnauthorizedException(String errorCode, Object... args) {
        super(errorCode, args);
    }

}
