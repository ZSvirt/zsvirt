package org.zstack.drs.message;

import org.zstack.drs.DRSMessage;
import org.zstack.header.message.NeedReplyMessage;

public class DeleteClusterDRSMsg extends NeedReplyMessage implements DRSMessage {
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getDRSUuid() {
        return uuid;
    }
}
