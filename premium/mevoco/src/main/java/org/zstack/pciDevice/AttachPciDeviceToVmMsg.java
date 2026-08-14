package org.zstack.pciDevice;

import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by GuoYi on 2019-06-24.
 */
public class AttachPciDeviceToVmMsg extends NeedReplyMessage {
    String pciDeviceUuid;
    String vmInstanceUuid;

    public String getPciDeviceUuid() {
        return pciDeviceUuid;
    }

    public void setPciDeviceUuid(String pciDeviceUuid) {
        this.pciDeviceUuid = pciDeviceUuid;
    }

    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }
}
