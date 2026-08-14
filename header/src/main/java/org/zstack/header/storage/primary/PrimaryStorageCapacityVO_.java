package org.zstack.header.storage.primary;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

/**
 */
@StaticMetamodel(PrimaryStorageCapacityVO.class)
public class PrimaryStorageCapacityVO_ extends StorageCapacityAO_ {
    public static volatile SingularAttribute<PrimaryStorageCapacityVO, String> uuid;
    public static volatile SingularAttribute<PrimaryStorageCapacityVO, Long> totalCapacity;
    public static volatile SingularAttribute<PrimaryStorageCapacityVO, Long> availableCapacity;
    public static volatile SingularAttribute<PrimaryStorageCapacityVO, Timestamp> createDate;
    public static volatile SingularAttribute<PrimaryStorageCapacityVO, Timestamp> lastOpDate;
}
