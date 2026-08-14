package org.zstack.header.network.l3;

import org.zstack.header.message.NeedReplyMessage;

public class ReturnIpMsg extends NeedReplyMessage implements L3NetworkMessage {
    private String usedIpUuid;
    private String l3NetworkUuid;
    private String nicUuid;

    public String getUsedIpUuid() {
        return usedIpUuid;
    }

    public void setUsedIpUuid(String usedIpUuid) {
        this.usedIpUuid = usedIpUuid;
    }

    public void setL3NetworkUuid(String l3NetworkUuid) {
        this.l3NetworkUuid = l3NetworkUuid;
    }

    @Override
    public String getL3NetworkUuid() {
        return l3NetworkUuid;
    }

    public String getNicUuid() {
        return nicUuid;
    }

    public void setNicUuid(String nicUuid) {
        this.nicUuid = nicUuid;
    }
}
