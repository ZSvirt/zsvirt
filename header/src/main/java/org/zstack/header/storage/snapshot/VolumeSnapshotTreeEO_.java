package org.zstack.header.storage.snapshot;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

/**
 */
@StaticMetamodel(VolumeSnapshotTreeEO.class)
public class VolumeSnapshotTreeEO_ extends VolumeSnapshotTreeAO_ {
    public static volatile SingularAttribute<VolumeSnapshotTreeEO, String> deleted;
}
