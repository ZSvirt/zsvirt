package org.zstack.header.core.external.service;

import org.zstack.header.message.NeedReplyMessage;

/**
 * @Author: ya.wang
 * @Date: 1/15/26 2:59 AM
 */
public class ApplyExternalServiceConfigurationMsg extends NeedReplyMessage {
    private String serviceType;

    public String getServiceType() {
        return serviceType;
    }

    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }
}
