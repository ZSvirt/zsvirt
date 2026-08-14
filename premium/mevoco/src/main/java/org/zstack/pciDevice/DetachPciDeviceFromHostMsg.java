package org.zstack.pciDevice;

import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by GuoYi on 2019-07-04.
 */
public class DetachPciDeviceFromHostMsg extends NeedReplyMessage {
    String pciDeviceUuid;

    public String getPciDeviceUuid() {
        return pciDeviceUuid;
    }

    public void setPciDeviceUuid(String pciDeviceUuid) {
        this.pciDeviceUuid = pciDeviceUuid;
    }
}
