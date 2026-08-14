package org.zstack.header.vrouterRoute;

import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by mingjian.deng on 2017/9/6.
 */
public class DeleteVRouterEntryInnerMsg extends NeedReplyMessage {
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
}
