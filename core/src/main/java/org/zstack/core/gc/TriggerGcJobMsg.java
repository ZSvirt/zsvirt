package org.zstack.core.gc;

import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by mingjian.deng on 2018/12/7.
 */
public class TriggerGcJobMsg extends NeedReplyMessage {
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
}
