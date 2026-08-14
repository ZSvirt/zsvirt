package org.zstack.usbDevice;

import org.zstack.header.message.NeedReplyMessage;

/**
 * @Author: DaoDao
 * @Date: 2023/9/6
 */
public class CheckAndReserveUsbDeviceMsg extends NeedReplyMessage {
    private String vmUuid;

    public String getVmUuid() {
        return vmUuid;
    }

    public void setVmUuid(String vmUuid) {
        this.vmUuid = vmUuid;
    }
}
