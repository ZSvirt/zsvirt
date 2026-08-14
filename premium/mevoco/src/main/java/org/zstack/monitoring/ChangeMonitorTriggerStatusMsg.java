package org.zstack.monitoring;

import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by xing5 on 2017/6/10.
 */
public class ChangeMonitorTriggerStatusMsg extends NeedReplyMessage implements MonitorTriggerMessage {
    private String uuid;
    private MonitorTriggerStatus status;
    private MonitorTriggerContext context;

    @Override
    public String getMonitorTriggerUuid() {
        return uuid;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public MonitorTriggerStatus getStatus() {
        return status;
    }

    public void setStatus(MonitorTriggerStatus status) {
        this.status = status;
    }

    public MonitorTriggerContext getContext() {
        return context;
    }

    public void setContext(MonitorTriggerContext context) {
        this.context = context;
    }
}
