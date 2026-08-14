package org.zstack.header.storage.snapshot;

import org.zstack.header.message.MessageReply;

/**
 * Created by frank on 6/18/2015.
 */
public class VolumeSnapshotReportPrimaryStorageCapacityUsageReply extends MessageReply {
    private long usedSize;

    public long getUsedSize() {
        return usedSize;
    }

    public void setUsedSize(long usedSize) {
        this.usedSize = usedSize;
    }
}
