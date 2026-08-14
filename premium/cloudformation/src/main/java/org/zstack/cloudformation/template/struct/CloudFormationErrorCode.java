package org.zstack.cloudformation.template.struct;

import org.zstack.header.errorcode.ErrorCode;

/**
 * Created by mingjian.deng on 2018/6/13.
 */
public class CloudFormationErrorCode {
    private boolean success = true;
    private ErrorCode errorCode;

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(ErrorCode errorCode) {
        this.success = false;
        this.errorCode = errorCode;
    }
}
