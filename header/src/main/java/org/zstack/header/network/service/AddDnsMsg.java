package org.zstack.header.network.service;

import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by frank on 8/21/2015.
 */
public class AddDnsMsg extends NeedReplyMessage {
    private String l3NetworkUuid;
    private String dns;

    public String getL3NetworkUuid() {
        return l3NetworkUuid;
    }

    public void setL3NetworkUuid(String l3NetworkUuid) {
        this.l3NetworkUuid = l3NetworkUuid;
    }

    public String getDns() {
        return dns;
    }

    public void setDns(String dns) {
        this.dns = dns;
    }
}
