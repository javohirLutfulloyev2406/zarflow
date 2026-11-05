package uz.zarflow.shared.exception;

public class NotFoundException extends ZarFlowException {

    public NotFoundException(String errorCode, Object... args) {
        super(errorCode, args);
    }

}
