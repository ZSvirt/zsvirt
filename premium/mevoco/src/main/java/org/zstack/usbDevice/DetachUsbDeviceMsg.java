package org.zstack.usbDevice;

import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by GuoYi on 10/21/17.
 */
public class DetachUsbDeviceMsg extends NeedReplyMessage {
    String usbDeviceUuid;

    public String getUsbDeviceUuid() {
        return usbDeviceUuid;
    }

    public void setUsbDeviceUuid(String usbDeviceUuid) {
        this.usbDeviceUuid = usbDeviceUuid;
    }
}
