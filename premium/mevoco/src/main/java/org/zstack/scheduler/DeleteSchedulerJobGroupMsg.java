package org.zstack.scheduler;

import org.zstack.header.message.NeedReplyMessage;

/**
 * @author qiuyu.zhang
 * @Package org.zstack.scheduler
 * @date 2020/12/18 4:45 PM
 */
public class DeleteSchedulerJobGroupMsg extends NeedReplyMessage {
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
}
