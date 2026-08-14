package org.zstack.header.network.service;

import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by shixin on 04/04/2018.
 */
public class L3NetworkUpdateDhcpMsg extends NeedReplyMessage {
    private String l3NetworkUuid;

    public String getL3NetworkUuid() {
        return l3NetworkUuid;
    }

    public void setL3NetworkUuid(String l3NetworkUuid) {
        this.l3NetworkUuid = l3NetworkUuid;
    }
}
