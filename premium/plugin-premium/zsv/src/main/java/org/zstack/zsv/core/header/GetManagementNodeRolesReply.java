package org.zstack.zsv.core.header;

import org.zstack.header.message.MessageReply;

public class GetManagementNodeRolesReply extends MessageReply {
    private String managementNodeUuid;
    private boolean host;
    private String hostUuid;

    public String getManagementNodeUuid() {
        return managementNodeUuid;
    }

    public void setManagementNodeUuid(String managementNodeUuid) {
        this.managementNodeUuid = managementNodeUuid;
    }

    public boolean isHost() {
        return host;
    }

    public void setHost(boolean host) {
        this.host = host;
    }

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }
}
