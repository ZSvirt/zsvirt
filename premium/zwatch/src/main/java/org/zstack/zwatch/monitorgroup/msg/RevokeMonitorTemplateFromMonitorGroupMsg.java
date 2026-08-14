package org.zstack.zwatch.monitorgroup.msg;

import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by ZStack on 2020/10/27.
 */
public class RevokeMonitorTemplateFromMonitorGroupMsg extends NeedReplyMessage implements MonitorGroupMsg {
    private String groupUuid;

    private String templateUuid;

    public String getGroupUuid() {
        return groupUuid;
    }

    public void setGroupUuid(String groupUuid) {
        this.groupUuid = groupUuid;
    }

    public String getTemplateUuid() {
        return templateUuid;
    }

    public void setTemplateUuid(String templateUuid) {
        this.templateUuid = templateUuid;
    }

    @Override
    public String getMonitorGroupUuid() {
        return groupUuid;
    }
}
