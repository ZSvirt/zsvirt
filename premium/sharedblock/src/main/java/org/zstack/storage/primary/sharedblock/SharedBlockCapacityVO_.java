package org.zstack.storage.primary.sharedblock;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(SharedBlockCapacityVO.class)
public class SharedBlockCapacityVO_ {
    public static volatile SingularAttribute<SharedBlockCapacityVO, String> uuid;
    public static volatile SingularAttribute<SharedBlockCapacityVO, Long> totalCapacity;
    public static volatile SingularAttribute<SharedBlockCapacityVO, Long> availableCapacity;
    public static volatile SingularAttribute<SharedBlockCapacityVO, Timestamp> createDate;
    public static volatile SingularAttribute<SharedBlockCapacityVO, Timestamp> lastOpDate;
}
