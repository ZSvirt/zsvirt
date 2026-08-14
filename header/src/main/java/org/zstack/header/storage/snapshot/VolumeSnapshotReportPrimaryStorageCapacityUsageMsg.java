package org.zstack.header.storage.snapshot;

import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by frank on 6/18/2015.
 */
public class VolumeSnapshotReportPrimaryStorageCapacityUsageMsg extends NeedReplyMessage {
    private String primaryStorageUuid;

    public String getPrimaryStorageUuid() {
        return primaryStorageUuid;
    }

    public void setPrimaryStorageUuid(String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
    }
}
