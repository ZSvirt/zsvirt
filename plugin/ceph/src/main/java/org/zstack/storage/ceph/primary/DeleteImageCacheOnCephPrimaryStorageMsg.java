package org.zstack.storage.ceph.primary;

import org.zstack.header.storage.primary.DeleteImageCacheOnPrimaryStorageMsg;
import org.zstack.header.storage.primary.PrimaryStorageMessage;

/**
 * Created by xing5 on 2016/7/13.
 */
public class DeleteImageCacheOnCephPrimaryStorageMsg extends DeleteImageCacheOnPrimaryStorageMsg implements PrimaryStorageMessage {
    private String snapshotPath;

    public String getSnapshotPath() {
        return snapshotPath;
    }

    public void setSnapshotPath(String snapshotPath) {
        this.snapshotPath = snapshotPath;
    }
}
