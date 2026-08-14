package org.zstack.header.storage.snapshot;

import org.zstack.header.vo.ResourceVO_;
import org.zstack.header.volume.VolumeType;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

/**
 */
@StaticMetamodel(VolumeSnapshotAO.class)
public class VolumeSnapshotAO_ extends ResourceVO_ {
    public static volatile SingularAttribute<VolumeSnapshotAO, String> name;
    public static volatile SingularAttribute<VolumeSnapshotAO, String> description;
    public static volatile SingularAttribute<VolumeSnapshotAO, String> type;
    public static volatile SingularAttribute<VolumeSnapshotAO, String> format;
    public static volatile SingularAttribute<VolumeSnapshotAO, String> treeUuid;
    public static volatile SingularAttribute<VolumeSnapshotAO, String> parentUuid;
    public static volatile SingularAttribute<VolumeSnapshotAO, String> volumeUuid;
    public static volatile SingularAttribute<VolumeSnapshotAO, String> primaryStorageUuid;
    public static volatile SingularAttribute<VolumeSnapshotAO, String> primaryStorageInstallPath;
    public static volatile SingularAttribute<VolumeSnapshotAO, String> volumeType;
    public static volatile SingularAttribute<VolumeSnapshotAO, Boolean> latest;
    public static volatile SingularAttribute<VolumeSnapshotAO, Boolean> fullSnapshot;
    public static volatile SingularAttribute<VolumeSnapshotAO, Integer> distance;
    public static volatile SingularAttribute<VolumeSnapshotAO, Long> size;
    public static volatile SingularAttribute<VolumeSnapshotAO, VolumeSnapshotState> state;
    public static volatile SingularAttribute<VolumeSnapshotAO, VolumeSnapshotStatus> status;
    public static volatile SingularAttribute<VolumeSnapshotAO, Timestamp> createDate;
    public static volatile SingularAttribute<VolumeSnapshotAO, Timestamp> lastOpDate;
}
