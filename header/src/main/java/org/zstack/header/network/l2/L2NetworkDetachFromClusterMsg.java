package org.zstack.header.network.l2;

import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by weiwang on 21/03/2017.
 */
public class L2NetworkDetachFromClusterMsg extends NeedReplyMessage implements L2NetworkMessage {
    private String l2NetworkUuid;
    private String clusterUuid;

    public String getClusterUuid() {
        return clusterUuid;
    }

    public void setClusterUuid(String clusterUuid) {
        this.clusterUuid = clusterUuid;
    }

    public void setL2NetworkUuid(String l2NetworkUuid) {
        this.l2NetworkUuid = l2NetworkUuid;
    }

    @Override
    public String getL2NetworkUuid() {
        return l2NetworkUuid;
    }

}
