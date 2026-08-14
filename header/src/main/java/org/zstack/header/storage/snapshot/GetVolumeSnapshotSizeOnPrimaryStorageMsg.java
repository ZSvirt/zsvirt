package org.zstack.header.storage.snapshot;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.storage.primary.PrimaryStorageMessage;

/**
 * Created by lining on 2019/5/14.
 */
public class GetVolumeSnapshotSizeOnPrimaryStorageMsg extends NeedReplyMessage implements PrimaryStorageMessage {
    private String snapshotUuid;

    private String primaryStorageUuid;

    public String getSnapshotUuid() {
        return snapshotUuid;
    }

    public void setSnapshotUuid(String snapshotUuid) {
        this.snapshotUuid = snapshotUuid;
    }

    public void setPrimaryStorageUuid(String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
    }

    @Override
    public String getPrimaryStorageUuid() {
        return primaryStorageUuid;
    }
}
