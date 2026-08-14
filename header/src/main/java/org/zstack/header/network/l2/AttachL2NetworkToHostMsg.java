package org.zstack.header.network.l2;

import org.zstack.header.message.NeedReplyMessage;

public class AttachL2NetworkToHostMsg extends NeedReplyMessage implements L2NetworkMessage {
    private String l2NetworkUuid;
    private String hostUuid;
    private String l2ProviderType;
    private String hostParam;

    public String getL2NetworkUuid() {
        return l2NetworkUuid;
    }

    public void setL2NetworkUuid(String l2NetworkUuid) {
        this.l2NetworkUuid = l2NetworkUuid;
    }

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public String getL2ProviderType() {
        return l2ProviderType;
    }

    public void setL2ProviderType(String l2ProviderType) {
        this.l2ProviderType = l2ProviderType;
    }

    public String getHostParam() {
        return hostParam;
    }

    public void setHostParam(String hostParam) {
        this.hostParam = hostParam;
    }
}
