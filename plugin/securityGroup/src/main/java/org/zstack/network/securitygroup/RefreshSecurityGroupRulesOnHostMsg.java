package org.zstack.network.securitygroup;

import org.zstack.header.message.NeedReplyMessage;

public class RefreshSecurityGroupRulesOnHostMsg extends NeedReplyMessage {
    private String hostUuid;

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }
}
