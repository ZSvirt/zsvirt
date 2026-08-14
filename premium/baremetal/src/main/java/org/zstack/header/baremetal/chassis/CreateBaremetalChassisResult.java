package org.zstack.header.baremetal.chassis;

import org.zstack.header.errorcode.ErrorCode;

/**
 * Created by GuoYi on 2018-10-08.
 */
public class CreateBaremetalChassisResult {
    private String ipmiAddress;
    private Integer ipmiPort;
    private boolean success = true;
    private ErrorCode error;

    public CreateBaremetalChassisResult(String ipmiAddress, Integer ipmiPort, ErrorCode error) {
        this.ipmiAddress = ipmiAddress;
        this.ipmiPort = ipmiPort;
        this.success = error == null;
        this.error = error;
    }

    public String getIpmiAddress() {
        return ipmiAddress;
    }

    public void setIpmiAddress(String ipmiAddress) {
        this.ipmiAddress = ipmiAddress;
    }

    public Integer getIpmiPort() {
        return ipmiPort;
    }

    public void setIpmiPort(Integer ipmiPort) {
        this.ipmiPort = ipmiPort;
    }

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
    }
}
