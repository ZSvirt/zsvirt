package org.zstack.header.vm;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(VmSchedHistoryVO.class)
public class VmSchedHistoryVO_ {
    public static volatile SingularAttribute<VmSchedHistoryVO, Long> id;
    public static volatile SingularAttribute<VmSchedHistoryVO, String> vmInstanceUuid;
    public static volatile SingularAttribute<VmSchedHistoryVO, String> zoneUuid;
    public static volatile SingularAttribute<VmSchedHistoryVO, String> accountUuid;
    public static volatile SingularAttribute<VmSchedHistoryVO, String> schedType;
    public static volatile SingularAttribute<VmSchedHistoryVO, String> schedReason;
    public static volatile SingularAttribute<VmSchedHistoryVO, String> failReason;
    public static volatile SingularAttribute<VmSchedHistoryVO, Boolean> success;
    public static volatile SingularAttribute<VmSchedHistoryVO, String> lastHostUuid;
    public static volatile SingularAttribute<VmSchedHistoryVO, String> destHostUuid;
    public static volatile SingularAttribute<VmSchedHistoryVO, Timestamp> createDate;
    public static volatile SingularAttribute<VmSchedHistoryVO, Timestamp> lastOpDate;
}
