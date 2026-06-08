package org.zstack.sdk.external.service;

import org.zstack.sdk.ErrorCode;

public class ApplyExternalConfigurationResult  {

    public java.lang.String managementNodeUuid;
    public void setManagementNodeUuid(java.lang.String managementNodeUuid) {
        this.managementNodeUuid = managementNodeUuid;
    }
    public java.lang.String getManagementNodeUuid() {
        return this.managementNodeUuid;
    }

    public ErrorCode errorCode;
    public void setErrorCode(ErrorCode errorCode) {
        this.errorCode = errorCode;
    }
    public ErrorCode getErrorCode() {
        return this.errorCode;
    }

    public boolean success;
    public void setSuccess(boolean success) {
        this.success = success;
    }
    public boolean getSuccess() {
        return this.success;
    }

}
