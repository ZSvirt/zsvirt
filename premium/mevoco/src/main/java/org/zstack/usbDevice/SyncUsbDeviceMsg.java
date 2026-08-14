package org.zstack.usbDevice;

import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by GuoYi on 10/21/17.
 */
public class SyncUsbDeviceMsg extends NeedReplyMessage {
    String hostUuid;

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }
}
