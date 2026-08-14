package org.zstack.pciDevice;

import org.zstack.header.message.NeedReplyMessage;

public class CheckAndReservePciDeviceMsg extends NeedReplyMessage {
    private String vmUuid;
    private String pciUuid;

    public String getVmUuid() {
        return vmUuid;
    }

    public void setVmUuid(String vmUuid) {
        this.vmUuid = vmUuid;
    }

    public String getPciUuid() {
        return pciUuid;
    }

    public void setPciUuid(String pciUuid) {
        this.pciUuid = pciUuid;
    }
}
