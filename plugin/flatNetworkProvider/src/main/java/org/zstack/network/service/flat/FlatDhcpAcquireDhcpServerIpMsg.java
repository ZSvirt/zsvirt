package org.zstack.network.service.flat;

import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by frank on 10/11/2015.
 */
public class FlatDhcpAcquireDhcpServerIpMsg extends NeedReplyMessage {
    private String l3NetworkUuid;

    public String getL3NetworkUuid() {
        return l3NetworkUuid;
    }

    public void setL3NetworkUuid(String l3NetworkUuid) {
        this.l3NetworkUuid = l3NetworkUuid;
    }
}
