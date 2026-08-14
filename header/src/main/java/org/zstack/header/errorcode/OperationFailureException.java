package org.zstack.header.errorcode;

/**
 */
public class OperationFailureException extends RuntimeException {
    private ErrorCode errorCode;

    @Override
    public String getMessage() {
        return String.format("%s: %s", OperationFailureException.class.getName(), errorCode);
    }

    public OperationFailureException(ErrorCode errorCode) {
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(ErrorCode errorCode) {
        this.errorCode = errorCode;
    }
}
