package org.zstack.header.volume;

/**
 * Create by weiwang at 2018/5/23
 */
public class ResizeVolumeStruct {
    private boolean vmRunning = false;
    private String vmHostUuid;
    private String vmInstanceUuid;

    public boolean isVmRunning() {
        return vmRunning;
    }

    public void setVmRunning(boolean vmRunning) {
        this.vmRunning = vmRunning;
    }

    public String getVmHostUuid() {
        return vmHostUuid;
    }

    public void setVmHostUuid(String vmHostUuid) {
        this.vmHostUuid = vmHostUuid;
    }

    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }
}
