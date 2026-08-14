package org.zstack.header.cluster;

import org.zstack.header.message.NeedReplyMessage;

public class ChangeClusterStateMsg extends NeedReplyMessage implements ClusterMessage {
    private String uuid;
    private String stateEvent;

    public ChangeClusterStateMsg() {
    }

    public String getStateEvent() {
        return stateEvent;
    }

    public void setStateEvent(String stateEvent) {
        this.stateEvent = stateEvent;
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
