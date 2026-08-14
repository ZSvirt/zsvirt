package org.zstack.header.cluster;

import org.zstack.header.message.NeedReplyMessage;

public class DeleteClusterMsg extends NeedReplyMessage implements ClusterMessage {
    private String uuid;

    public DeleteClusterMsg() {
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getClusterUuid() {
        return getUuid();
    }
}
