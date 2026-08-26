package org.zstack.header.core.external.service;

import org.zstack.header.errorcode.ErrorCode;

/**
 * @Author: ya.wang
 * @Date: 1/15/26 2:50 AM
 */
public class ApplyExternalConfigurationResult {

    private String managementNodeUuid;
    private ErrorCode errorCode;
    private boolean success = true;

    public String getManagementNodeUuid() {
        return managementNodeUuid;
    }

    public void setManagementNodeUuid(String managementNodeUuid) {
        this.managementNodeUuid = managementNodeUuid;
    }

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
