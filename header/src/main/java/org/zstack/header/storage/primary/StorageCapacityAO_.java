package org.zstack.header.storage.primary;

import org.zstack.header.storage.snapshot.VolumeSnapshotTreeAO_;
import org.zstack.header.storage.snapshot.VolumeSnapshotTreeEO;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(StorageCapacityAO.class)
public class StorageCapacityAO_ {
    public static volatile SingularAttribute<StorageCapacityAO, String> totalPhysicalCapacity;
    public static volatile SingularAttribute<StorageCapacityAO, String> availablePhysicalCapacity;
}
