package org.zstack.usbDevice;

import org.zstack.header.message.MessageReply;

import java.util.List;

/**
 * @Author: DaoDao
 * @Date: 2023/9/6
 */
public class CheckAndReserveUsbDeviceReply extends MessageReply {
    private List<String> revertUsbUuids;

    public List<String> getRevertUsbUuids() {
        return revertUsbUuids;
    }

    public void setRevertUsbUuids(List<String> revertUsbUuids) {
        this.revertUsbUuids = revertUsbUuids;
    }
}
