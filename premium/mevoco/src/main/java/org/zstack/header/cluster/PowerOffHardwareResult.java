package org.zstack.header.cluster;

import org.zstack.header.errorcode.ErrorCode;

public class PowerOffHardwareResult {
    private String uuid;
    private boolean success = true;
    private ErrorCode error;

    public String getUuid() {
        return uuid;
    }

    public boolean isSuccess() {
        return success;
    }

    public ErrorCode getError() {
        return error;
    }

    public void setError(ErrorCode error) {
        success = false;
        this.error = error;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public static PowerOffHardwareResult valueOf(String uuid, ErrorCode error) {
        PowerOffHardwareResult result = new PowerOffHardwareResult();
        result.uuid = uuid;
        result.error = error;
        result.success = error == null;
        return result;
    }
}
