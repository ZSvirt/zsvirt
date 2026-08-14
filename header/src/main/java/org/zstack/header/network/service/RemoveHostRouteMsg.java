package org.zstack.header.network.service;

import org.zstack.header.message.NeedReplyMessage;

public class RemoveHostRouteMsg extends NeedReplyMessage {
    private String l3NetworkUuid;
    private String prefix;

    public String getL3NetworkUuid() {
        return l3NetworkUuid;
    }

    public void setL3NetworkUuid(String l3NetworkUuid) {
        this.l3NetworkUuid = l3NetworkUuid;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }
}
