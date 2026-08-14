package org.zstack.header.network.l2;

import org.zstack.header.message.NeedReplyMessage;

/**
 */
public class DetachL2NetworkFromClusterMsg extends NeedReplyMessage implements L2NetworkMessage {
    private String l2NetworkUuid;
    private String clusterUuid;

    public String getL2NetworkUuid() {
        return l2NetworkUuid;
    }

    public void setL2NetworkUuid(String l2NetworkUuid) {
        this.l2NetworkUuid = l2NetworkUuid;
    }

    public String getClusterUuid() {
        return clusterUuid;
    }

    public void setClusterUuid(String clusterUuid) {
        this.clusterUuid = clusterUuid;
    }
}
