package org.zstack.log;

import org.zstack.header.message.MessageReply;


public class AddLogConfigurationReply extends MessageReply {
    private String managementNodeUuid;
    private String value;

    public String getManagementNodeUuid() {
        return managementNodeUuid;
    }

    public void setManagementNodeUuid(String managementNodeUuid) {
        this.managementNodeUuid = managementNodeUuid;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
