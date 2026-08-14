package org.zstack.header.storage.snapshot.group;

import org.zstack.header.storage.snapshot.SnapshotBackendOperation;

/**
 * Created by MaJin on 2019/7/9.
 */
public interface VolumeSnapshotGroupMessage {
    String getGroupUuid();
    SnapshotBackendOperation getBackendOperation();
}
