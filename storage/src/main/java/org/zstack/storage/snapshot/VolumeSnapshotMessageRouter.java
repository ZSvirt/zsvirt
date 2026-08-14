package org.zstack.storage.snapshot;

import org.zstack.header.storage.snapshot.VolumeSnapshotInventory;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO;

/**
 * Created by MaJin on 2019/7/10.
 */
public class VolumeSnapshotMessageRouter {
    public static String getResourceIdToRouteMsg(VolumeSnapshotVO snapshot) {
        return snapshot.getVolumeUuid() != null ? snapshot.getVolumeUuid() : snapshot.getTreeUuid();
    }

    public static String getResourceIdToRouteMsg(VolumeSnapshotInventory snapshot) {
        return snapshot.getVolumeUuid() != null ? snapshot.getVolumeUuid() : snapshot.getTreeUuid();
    }
}
