package org.zstack.pciDevice;

import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by GuoYi on 2019-07-03.
 */
public class AttachPciDeviceToHostMsg extends NeedReplyMessage {
    String pciDeviceUuid;

    public String getPciDeviceUuid() {
        return pciDeviceUuid;
    }

    public void setPciDeviceUuid(String pciDeviceUuid) {
        this.pciDeviceUuid = pciDeviceUuid;
    }
}
