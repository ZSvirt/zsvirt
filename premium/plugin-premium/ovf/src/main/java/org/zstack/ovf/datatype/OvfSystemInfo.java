package org.zstack.ovf.datatype;

/**
 * This indicates the vmware hardware compatible version.
 *
 * Created by Qi Le on 2022/3/4
 */
public class OvfSystemInfo {
    private String virtualSystemType;
    private String firmwareType;

    public String getFirmwareType() {
        return firmwareType;
    }

    public void setFirmwareType(String firmwareType) {
        this.firmwareType = firmwareType;
    }

    public String getVirtualSystemType() {
        return virtualSystemType;
    }

    public void setVirtualSystemType(String virtualSystemType) {
        this.virtualSystemType = virtualSystemType;
    }
}
