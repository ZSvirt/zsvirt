package org.zstack.routeProtocol;

import org.zstack.header.message.NeedReplyMessage;

import java.util.List;

public class RefreshRouterProtocolMsg extends NeedReplyMessage {
    private List<String> vRouterUuids;

    public List<String> getvRouterUuids() {
        return vRouterUuids;
    }

    public void setvRouterUuids(List<String> vRouterUuids) {
        this.vRouterUuids = vRouterUuids;
    }
}
