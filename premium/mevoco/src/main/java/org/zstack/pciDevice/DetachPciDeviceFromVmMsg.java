package org.zstack.pciDevice;

import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by weiwang on 13/07/2017.
 */
public class DetachPciDeviceFromVmMsg extends NeedReplyMessage {
    String pciDeviceUuid;

    String vmInstanceUuid;

    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    public String getPciDeviceUuid() {
        return pciDeviceUuid;
    }

    public void setPciDeviceUuid(String pciDeviceUuid) {
        this.pciDeviceUuid = pciDeviceUuid;
    }
}
