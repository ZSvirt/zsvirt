package org.zstack.scheduler;

import org.zstack.header.message.NeedReplyMessage;

/**
 * @author qiuyu.zhang
 * @Package org.zstack.scheduler
 * @date 2020/12/14 3:20 PM
 */
public class DeleteSchedulerJobMsg extends NeedReplyMessage implements SchedulerMessage {

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
