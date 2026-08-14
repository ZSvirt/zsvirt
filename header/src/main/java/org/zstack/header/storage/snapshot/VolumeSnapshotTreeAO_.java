package org.zstack.header.storage.snapshot;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

/**
 */
@StaticMetamodel(VolumeSnapshotTreeAO.class)
public class VolumeSnapshotTreeAO_ extends ResourceVO_ {
    public static volatile SingularAttribute<VolumeSnapshotTreeAO, Boolean> current;
    public static volatile SingularAttribute<VolumeSnapshotTreeAO, String> volumeUuid;
    public static volatile SingularAttribute<VolumeSnapshotTreeAO, VolumeSnapshotTreeStatus> status;
    public static volatile SingularAttribute<VolumeSnapshotTreeAO, Timestamp> createDate;
    public static volatile SingularAttribute<VolumeSnapshotTreeAO, Timestamp> lastOpDate;
}
