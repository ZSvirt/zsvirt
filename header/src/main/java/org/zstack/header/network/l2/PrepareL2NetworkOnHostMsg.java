package org.zstack.header.network.l2;

import org.zstack.header.host.HostInventory;
import org.zstack.header.message.NeedReplyMessage;

/**
 */
public class PrepareL2NetworkOnHostMsg extends NeedReplyMessage implements L2NetworkMessage {
    private String l2NetworkUuid;
    private HostInventory host;

    @Override
    public String getL2NetworkUuid() {
        return l2NetworkUuid;
    }

    public void setL2NetworkUuid(String l2NetworkUuid) {
        this.l2NetworkUuid = l2NetworkUuid;
    }

    public HostInventory getHost() {
        return host;
    }

    public void setHost(HostInventory host) {
        this.host = host;
    }
}
