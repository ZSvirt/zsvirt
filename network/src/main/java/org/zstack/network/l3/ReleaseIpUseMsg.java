package org.zstack.network.l3;

import org.zstack.header.message.Message;
import org.zstack.header.network.l3.L3NetworkMessage;

public class ReleaseIpUseMsg extends Message implements L3NetworkMessage {
    private String usedIpUuid;
    private String details;
    private String use;
    private String l3NetworkUuid;

    public String getUsedIpUuid() {
        return usedIpUuid;
    }
    public void setUsedIpUuid(String usedIpUuid) {
        this.usedIpUuid = usedIpUuid;
    }
    public String getDetails() {
        return details;
    }
    public void setDetails(String details) {
        this.details = details;
    }
    public String getUse() {
        return use;
    }
    public void setUse(String use) {
        this.use = use;
    }
    @Override
    public String getL3NetworkUuid() {
        return l3NetworkUuid;
    }
    public void setL3NetworkUuid(String l3NetworkUuid) {
        this.l3NetworkUuid = l3NetworkUuid;
    }
}
