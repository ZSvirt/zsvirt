package org.zstack.header.storage.snapshot;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

/**
 */
@StaticMetamodel(VolumeSnapshotBackupStorageRefVO.class)
public class VolumeSnapshotBackupStorageRefVO_ {
    public static volatile SingularAttribute<VolumeSnapshotBackupStorageRefVO, Long> id;
    public static volatile SingularAttribute<VolumeSnapshotBackupStorageRefVO, String> volumeSnapshotUuid;
    public static volatile SingularAttribute<VolumeSnapshotBackupStorageRefVO, String> backupStorageUuid;
    public static volatile SingularAttribute<VolumeSnapshotBackupStorageRefVO, String> installPath;
}
