package org.zstack.header.storage.snapshot.reference;


import org.zstack.header.storage.snapshot.VolumeSnapshotBackupStorageRefVO;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(VolumeSnapshotReferenceVO.class)
public class VolumeSnapshotReferenceVO_ {
    public static volatile SingularAttribute<VolumeSnapshotBackupStorageRefVO, Long> id;
    public static volatile SingularAttribute<VolumeSnapshotBackupStorageRefVO, Long> parentId;
    public static volatile SingularAttribute<VolumeSnapshotBackupStorageRefVO, String> volumeUuid;
    public static volatile SingularAttribute<VolumeSnapshotBackupStorageRefVO, String> volumeSnapshotUuid;
    public static volatile SingularAttribute<VolumeSnapshotBackupStorageRefVO, String> volumeSnapshotInstallUrl;
    public static volatile SingularAttribute<VolumeSnapshotBackupStorageRefVO, String> treeUuid;
    public static volatile SingularAttribute<VolumeSnapshotBackupStorageRefVO, String> referenceUuid;
    public static volatile SingularAttribute<VolumeSnapshotBackupStorageRefVO, String> referenceType;
    public static volatile SingularAttribute<VolumeSnapshotBackupStorageRefVO, String> referenceInstallUrl;
    public static volatile SingularAttribute<VolumeSnapshotBackupStorageRefVO, String> referenceVolumeUuid;

}
