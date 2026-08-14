package org.zstack.header.storage.snapshot;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

/**
 */
@StaticMetamodel(VolumeSnapshotEO.class)
public class VolumeSnapshotEO_ extends VolumeSnapshotAO_ {
    public static volatile SingularAttribute<VolumeSnapshotEO, String> deleted;
}
