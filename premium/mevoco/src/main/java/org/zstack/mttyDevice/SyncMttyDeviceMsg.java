package org.zstack.mttyDevice;

import org.zstack.header.message.NeedReplyMessage;

import java.util.List;

public class SyncMttyDeviceMsg extends NeedReplyMessage {
    String hostUuid;

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }
}
