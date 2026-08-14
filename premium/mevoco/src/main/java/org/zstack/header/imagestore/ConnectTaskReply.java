package org.zstack.header.imagestore;

import org.zstack.header.errorcode.ErrorCode;

/**
 * Created by mingjian.deng on 2017/9/22.
 */
public class ConnectTaskReply {
    private String value;
    private boolean success = true;
    private ErrorCode error;

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public ErrorCode getError() {
        return error;
    }

    public void setError(ErrorCode error) {
        this.error = error;
        this.setSuccess(false);
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
