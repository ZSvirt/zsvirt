package org.zstack.header.host;

import org.zstack.header.errorcode.ErrorCode;

public class AddHostFromFileResult {
    private String ip;
    private boolean success = true;
    private ErrorCode error;

    public AddHostFromFileResult(String ip, ErrorCode error) {
        this.ip = ip;
        this.error = error;
        this.success = error == null;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public void setError(ErrorCode error) {
        this.error = error;
        this.success = false;
    }

    public String getIp() {
        return ip;
    }

    public ErrorCode getError() {
        return error;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }
}
