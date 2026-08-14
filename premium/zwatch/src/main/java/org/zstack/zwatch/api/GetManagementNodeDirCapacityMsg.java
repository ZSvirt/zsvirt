package org.zstack.zwatch.api;

import org.zstack.header.message.NeedReplyMessage;

public class GetManagementNodeDirCapacityMsg extends NeedReplyMessage {
    private String managementNodeUuid;

    public String getManagementNodeUuid() {
        return managementNodeUuid;
    }

    public void setManagementNodeUuid(String managementNodeUuid) {
        this.managementNodeUuid = managementNodeUuid;
    }
}
