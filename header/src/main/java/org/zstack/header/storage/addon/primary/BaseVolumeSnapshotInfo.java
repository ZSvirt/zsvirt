package org.zstack.header.storage.addon.primary;

import org.zstack.header.storage.snapshot.VolumeSnapshotStats;

public class BaseVolumeSnapshotInfo extends VolumeSnapshotStats {
    protected BaseVolumeInfo volume;
    // optional
    protected String id;
    // optional
    protected String name;
}
