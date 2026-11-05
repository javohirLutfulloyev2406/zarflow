package uz.zarflow.shared.exception;

public class BusinessRuleException extends ZarFlowException {

    public BusinessRuleException(String errorCode, Object... args) {
        super(errorCode, args);
    }

}
