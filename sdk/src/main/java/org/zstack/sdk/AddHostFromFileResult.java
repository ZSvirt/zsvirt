package org.zstack.sdk;

import org.zstack.sdk.ErrorCode;

public class AddHostFromFileResult  {

    public java.lang.String ip;
    public void setIp(java.lang.String ip) {
        this.ip = ip;
    }
    public java.lang.String getIp() {
        return this.ip;
    }

    public boolean success;
    public void setSuccess(boolean success) {
        this.success = success;
    }
    public boolean getSuccess() {
        return this.success;
    }

    public ErrorCode error;
    public void setError(ErrorCode error) {
        this.error = error;
    }
    public ErrorCode getError() {
        return this.error;
    }

}
