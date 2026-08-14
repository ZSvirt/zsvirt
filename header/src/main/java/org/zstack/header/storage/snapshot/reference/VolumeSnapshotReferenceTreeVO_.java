package org.zstack.header.storage.snapshot.reference;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;


@StaticMetamodel(VolumeSnapshotReferenceTreeVO.class)
public class VolumeSnapshotReferenceTreeVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<VolumeSnapshotReferenceTreeVO, String> rootImageUuid;
    public static volatile SingularAttribute<VolumeSnapshotReferenceTreeVO, String> rootVolumeSnapshotUuid;
    public static volatile SingularAttribute<VolumeSnapshotReferenceTreeVO, String> rootVolumeUuid;
    public static volatile SingularAttribute<VolumeSnapshotReferenceTreeVO, String> rootVolumeSnapshotTreeUuid;
    public static volatile SingularAttribute<VolumeSnapshotReferenceTreeVO, String> rootVolumeSnapshotInstallUrl;

    public static volatile SingularAttribute<VolumeSnapshotReferenceTreeVO, String> primaryStorageUuid;
    public static volatile SingularAttribute<VolumeSnapshotReferenceTreeVO, String> hostUuid;
}
