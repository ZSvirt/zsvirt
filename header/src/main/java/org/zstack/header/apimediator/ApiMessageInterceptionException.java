package org.zstack.header.apimediator;

import org.zstack.header.errorcode.ErrorCode;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 11:38 PM
 * To change this template use File | Settings | File Templates.
 */
public class ApiMessageInterceptionException extends RuntimeException {
    private ErrorCode error;

    public ApiMessageInterceptionException(ErrorCode err) {
        this.error = err;
    }

    @Override
    public String getMessage() {
        return error.toString();
    }

    public ErrorCode getError() {
        return error;
    }

    public void setError(ErrorCode error) {
        this.error = error;
    }
}
