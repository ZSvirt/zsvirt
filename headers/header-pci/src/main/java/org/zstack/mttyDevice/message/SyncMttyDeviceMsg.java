package org.zstack.mttyDevice.message;

import org.zstack.header.message.NeedReplyMessage;

public class SyncMttyDeviceMsg extends NeedReplyMessage {
    String hostUuid;

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }
}
