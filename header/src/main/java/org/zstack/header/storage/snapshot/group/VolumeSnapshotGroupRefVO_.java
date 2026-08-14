package org.zstack.header.storage.snapshot.group;


import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

/**
 * Created by MaJin on 2019/7/10.
 */
@StaticMetamodel(VolumeSnapshotGroupRefVO.class)
public class VolumeSnapshotGroupRefVO_ {
    public static volatile SingularAttribute<VolumeSnapshotGroupRefVO, String> volumeSnapshotUuid;
    public static volatile SingularAttribute<VolumeSnapshotGroupRefVO, String> volumeSnapshotGroupUuid;
    public static volatile SingularAttribute<VolumeSnapshotGroupRefVO, Boolean> snapshotDeleted;
    public static volatile SingularAttribute<VolumeSnapshotGroupRefVO, Integer> deviceId;
    public static volatile SingularAttribute<VolumeSnapshotGroupRefVO, String> volumeName;
    public static volatile SingularAttribute<VolumeSnapshotGroupRefVO, String> volumeUuid;
    public static volatile SingularAttribute<VolumeSnapshotGroupRefVO, String> volumeType;
    public static volatile SingularAttribute<VolumeSnapshotGroupRefVO, String> volumeInstallPath;
    public static volatile SingularAttribute<VolumeSnapshotGroupRefVO, String> volumeSnapshotName;
    public static volatile SingularAttribute<VolumeSnapshotGroupRefVO, String> volumeSnapshotInstallPath;
    public static volatile SingularAttribute<VolumeSnapshotGroupRefVO, Timestamp> createDate;
    public static volatile SingularAttribute<VolumeSnapshotGroupRefVO, Timestamp> lastOpDate;
    public static volatile SingularAttribute<VolumeSnapshotGroupRefVO, Timestamp> volumeLastAttachDate;
}
