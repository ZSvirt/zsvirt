package org.zstack.header.host;

import org.zstack.header.message.NeedReplyMessage;

/**
 * @Author: DaoDao
 * @Date: 2023/7/4
 */
public class ChangeHostStatusMsg extends NeedReplyMessage implements HostMessage{
    private String uuid;
    private String statusEvent;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getStatusEvent() {
        return statusEvent;
    }

    public void setStatusEvent(String statusEvent) {
        this.statusEvent = statusEvent;
    }

    @Override
    public String getHostUuid() {
        return uuid;
    }
}
