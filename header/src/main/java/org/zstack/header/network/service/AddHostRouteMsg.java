package org.zstack.header.network.service;

import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by shixin on 04/04/2018.
 */
public class AddHostRouteMsg extends NeedReplyMessage {
    private String l3NetworkUuid;
    private String prefix;
    private String nexthop;

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

    public String getNexthop() {
        return nexthop;
    }

    public void setNexthop(String nexthop) {
        this.nexthop = nexthop;
    }
}
