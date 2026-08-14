package org.zstack.header.exception;

public class CloudOperationError extends Error {
    private final String errorCode;
    private final String details;

    public CloudOperationError(String errorCode, String details) {
        super();
        this.errorCode = errorCode;
        this.details = details;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getDetails() {
        return details;
    }
}
