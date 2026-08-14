package org.zstack.header.vm;

import org.zstack.header.errorcode.ErrorCode;

public class VmInstantiateResourceException extends Exception {
    private ErrorCode errorCode;

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(ErrorCode errorCode) {
        this.errorCode = errorCode;
    }
}
