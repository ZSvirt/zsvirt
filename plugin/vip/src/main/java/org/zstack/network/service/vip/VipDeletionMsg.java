package org.zstack.network.service.vip;

import org.zstack.header.message.DeletionMessage;

/**
 */
public class VipDeletionMsg extends DeletionMessage implements VipMessage {
    private String vipUuid;
    private boolean returnIp = true;

    @Override
    public String getVipUuid() {
        return vipUuid;
    }

    public void setVipUuid(String vipUuid) {
        this.vipUuid = vipUuid;
    }

    public boolean isReturnIp() {
        return returnIp;
    }

    public void setReturnIp(boolean returnIp) {
        this.returnIp = returnIp;
    }
}
