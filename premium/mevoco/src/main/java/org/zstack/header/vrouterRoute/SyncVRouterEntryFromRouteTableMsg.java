package org.zstack.header.vrouterRoute;

import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by mingjian.deng on 17/6/28.
 */
public class SyncVRouterEntryFromRouteTableMsg extends NeedReplyMessage {
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
}
