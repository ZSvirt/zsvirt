package org.zstack.header.volume;

import org.zstack.header.message.NeedReplyMessage;

public class BatchSyncManagedActiveVolumeSizeMsg extends NeedReplyMessage {
    private String clusterUuid;

    public void setClusterUuid(String clusterUuid) {
        this.clusterUuid = clusterUuid;
    }

    public String getClusterUuid() {
        return clusterUuid;
    }
}
