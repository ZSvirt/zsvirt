package org.zstack.billing.spendingcalculator.snapshot;

import org.zstack.billing.UsageAO_;
import org.zstack.billing.spendingcalculator.vm.VmUsageVO;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

/**
 * Created by camile on 2017/5/19.
 */
@StaticMetamodel(SnapShotUsageVO.class)
public class SnapShotUsageVO_ extends UsageAO_ {
    public static volatile SingularAttribute<SnapShotUsageVO, Long> id;
    public static volatile SingularAttribute<SnapShotUsageVO, String> volumeUuid;
    public static volatile SingularAttribute<SnapShotUsageVO, String> SnapshotUuid;
    public static volatile SingularAttribute<SnapShotUsageVO, String> SnapshotStatus;
    public static volatile SingularAttribute<SnapShotUsageVO, String> SnapshotName;
    public static volatile SingularAttribute<SnapShotUsageVO, Long> SnapshotSize;
    public static volatile SingularAttribute<VmUsageVO, String> inventory;
    public static volatile SingularAttribute<SnapShotUsageVO, Timestamp> createDate;
    public static volatile SingularAttribute<SnapShotUsageVO, Timestamp> lastOpDate;
}
