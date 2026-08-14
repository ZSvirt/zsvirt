package org.zstack.storage.ceph;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

/**
 * Created by Administrator on 2017-03-21.
 */

@StaticMetamodel(CephCapacityVO.class)
public class CephCapacityVO_ {
    public static volatile SingularAttribute<CephCapacityVO, String> fsid;
    public static volatile SingularAttribute<CephCapacityVO, Long> totalCapacity;
    public static volatile SingularAttribute<CephCapacityVO, Long> availableCapacity;
    public static volatile SingularAttribute<CephCapacityVO, Timestamp> createDate;
    public static volatile SingularAttribute<CephCapacityVO, Timestamp> lastOpDate;
}
