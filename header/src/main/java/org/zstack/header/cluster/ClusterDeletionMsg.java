package org.zstack.header.cluster;

import org.zstack.header.message.DeletionMessage;

/**
 */
public class ClusterDeletionMsg extends DeletionMessage implements ClusterMessage {
    private String clusterUuid;

    @Override
    public String getClusterUuid() {
        return clusterUuid;
    }

    public void setClusterUuid(String clusterUuid) {
        this.clusterUuid = clusterUuid;
    }
}
