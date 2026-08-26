package org.zstack.header.core.external.service;

import org.zstack.header.message.MessageReply;

/**
 * @Author: ya.wang
 * @Date: 1/15/26 3:26 AM
 */
public class ApplyExternalServiceConfigurationReply extends MessageReply {
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
