package uz.zarflow.shared.exception;

public abstract class ZarFlowException extends RuntimeException {

    private final String errorCode;
    private final transient Object[] args;

    protected ZarFlowException(String errorCode, Object... args) {
        super(errorCode);
        this.errorCode = errorCode;
        this.args = args;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public Object[] getArgs() {
        return args;
    }

}
