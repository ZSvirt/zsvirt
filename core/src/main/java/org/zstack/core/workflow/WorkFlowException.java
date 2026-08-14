package org.zstack.core.workflow;

import org.zstack.header.errorcode.ErrorCode;

public class WorkFlowException extends Exception {
    private ErrorCode errorCode;

    public WorkFlowException(ErrorCode err, Throwable t) {
        super(err.toString(), t);
        this.errorCode = err;
    }
    
    public WorkFlowException(ErrorCode err) {
        super(err.toString());
        this.errorCode = err;
    }
    
    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(ErrorCode errorCode) {
        this.errorCode = errorCode;
    }
}
