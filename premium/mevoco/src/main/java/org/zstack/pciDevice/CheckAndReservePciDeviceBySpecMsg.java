package org.zstack.pciDevice;

import org.zstack.header.message.NeedReplyMessage;

public class CheckAndReservePciDeviceBySpecMsg extends NeedReplyMessage {
    private String hostUuid;
    private String vmUuid;
    private boolean isDryRun;

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public String getVmUuid() {
        return vmUuid;
    }

    public void setVmUuid(String vmUuid) {
        this.vmUuid = vmUuid;
    }

    public boolean isDryRun() {
        return isDryRun;
    }

    public void setDryRun(boolean dryRun) {
        isDryRun = dryRun;
    }
}
