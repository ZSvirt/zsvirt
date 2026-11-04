package org.zstack.guesttools;

import org.zstack.header.message.MessageReply;

public class GetVmZWatchStatusReply extends MessageReply {
    private String status;
    private String version;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
}
