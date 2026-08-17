package org.zstack.crypto.keyprovider;

import org.zstack.header.message.NeedReplyMessage;

public class TriggerKeyProviderHealthCheckMsg extends NeedReplyMessage {
    private String providerUuid;
    private String providerType;
    private boolean realtime;

    public String getProviderUuid() {
        return providerUuid;
    }

    public void setProviderUuid(String providerUuid) {
        this.providerUuid = providerUuid;
    }

    public String getProviderType() {
        return providerType;
    }

    public void setProviderType(String providerType) {
        this.providerType = providerType;
    }

    public boolean isRealtime() {
        return realtime;
    }

    public void setRealtime(boolean realtime) {
        this.realtime = realtime;
    }
}
