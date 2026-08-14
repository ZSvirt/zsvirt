package org.zstack.header.host;

/**
 * @author Xingwei Yu
 * @date 2025/8/12 14:20
 */
public class HostResizeVolumeStruct {
    private String deviceType;
    String installPath;
    long size;

    public String getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }

    public String getInstallPath() {
        return installPath;
    }

    public void setInstallPath(String installPath) {
        this.installPath = installPath;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }
}
