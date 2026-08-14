package org.zstack.zops;

import org.zstack.header.message.MessageReply;

public class GetCephMonHealthStatusManagementNodeReply extends MessageReply {
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    private String status;

}
