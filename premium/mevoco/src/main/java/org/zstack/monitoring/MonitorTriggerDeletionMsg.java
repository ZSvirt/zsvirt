package org.zstack.monitoring;

import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by xing5 on 2017/8/8.
 */
public class MonitorTriggerDeletionMsg extends NeedReplyMessage implements MonitorTriggerMessage {
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getMonitorTriggerUuid() {
        return uuid;
    }
}
