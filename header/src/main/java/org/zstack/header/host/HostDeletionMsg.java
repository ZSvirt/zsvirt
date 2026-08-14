package org.zstack.header.host;

import org.zstack.header.message.DeletionMessage;

/**
 */
public class HostDeletionMsg extends DeletionMessage implements HostMessage {
    private String hostUuid;

    @Override
    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }
}
