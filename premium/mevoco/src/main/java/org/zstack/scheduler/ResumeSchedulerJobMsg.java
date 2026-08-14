package org.zstack.scheduler;

import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by kayo on 2018/9/10.
 */
public class ResumeSchedulerJobMsg extends NeedReplyMessage implements SchedulerMessage {
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getSchedulerUuid() {
        return uuid;
    }
}
