package org.zstack.zwatch.monitorgroup.msg;

import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by ZStack on 2020/10/27.
 */
public class DeleteMonitorGroupAlarmMsg extends NeedReplyMessage implements MonitorGroupMsg {
    private String groupUuid;

    public String getGroupUuid() {
        return groupUuid;
    }

    public void setGroupUuid(String groupUuid) {
        this.groupUuid = groupUuid;
    }

    @Override
    public String getMonitorGroupUuid() {
        return groupUuid;
    }
}
