package org.zstack.storage.device.localRaid;

import org.zstack.header.message.NeedReplyMessage;

public class RefreshLocalRaidMsg extends NeedReplyMessage {
    private String hostUuid;

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }
}
