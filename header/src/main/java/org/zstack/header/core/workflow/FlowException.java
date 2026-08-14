package org.zstack.header.core.workflow;

import org.zstack.header.errorcode.ErrorCode;

/**
 */
public class FlowException extends RuntimeException {
    private ErrorCode errorCode;

    public FlowException(ErrorCode errorCode) {
        this.errorCode = errorCode;
    }

    public FlowException(String message, ErrorCode errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public FlowException(String message, Throwable cause, ErrorCode errorCode) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public FlowException(Throwable cause, ErrorCode errorCode) {
        super(cause);
        this.errorCode = errorCode;
    }

    public FlowException() {
    }

    public FlowException(String message) {
        super(message);
    }

    public FlowException(String message, Throwable cause) {
        super(message, cause);
    }

    public FlowException(Throwable cause) {
        super(cause);
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(ErrorCode errorCode) {
        this.errorCode = errorCode;
    }
}
