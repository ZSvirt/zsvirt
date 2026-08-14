package org.zstack.header.volume;

import org.zstack.header.message.NeedReplyMessage;

public class BatchSyncActiveVolumeSizeOnHostMsg extends NeedReplyMessage {
    private String hostUuid;

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public String getHostUuid() {
        return hostUuid;
    }
}
