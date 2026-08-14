package org.zstack.storage.device.localRaid;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(PhysicalDriveSmartSelfTestHistoryVO.class)
public class PhysicalDriveSmartSelfTestHistoryVO_ {
    public static volatile SingularAttribute<PhysicalDriveSmartSelfTestHistoryVO, Long> id;
    public static volatile SingularAttribute<PhysicalDriveSmartSelfTestHistoryVO, String> raidPhysicalDriveUuid;
    public static volatile SingularAttribute<PhysicalDriveSmartSelfTestHistoryVO, RunningState> runningState;
    public static volatile SingularAttribute<PhysicalDriveSmartSelfTestHistoryVO, String> testResult;
    public static volatile SingularAttribute<PhysicalDriveSmartSelfTestHistoryVO, Timestamp> createDate;
    public static volatile SingularAttribute<PhysicalDriveSmartSelfTestHistoryVO, Timestamp> lastOpDate;
}
