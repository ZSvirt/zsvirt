package org.zstack.drs.entity;

import org.zstack.drs.data.DRSVmMigrationStatus;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

/**
 * Created by lining on 2019/12/12.
 */

@StaticMetamodel(DRSVmMigrationActivityVO.class)
public class DRSVmMigrationActivityVO_ {
    public static volatile SingularAttribute<DRSVmMigrationActivityVO, String> drsUuid;
    public static volatile SingularAttribute<DRSVmMigrationActivityVO, String> uuid;
    public static volatile SingularAttribute<DRSVmMigrationActivityVO, String> vmUuid;
    public static volatile SingularAttribute<DRSVmMigrationActivityVO, String> vmSourceHostUuid;
    public static volatile SingularAttribute<DRSVmMigrationActivityVO, String> vmTargetHostUuid;
    public static volatile SingularAttribute<DRSVmMigrationActivityVO, DRSVmMigrationStatus> status;
    public static volatile SingularAttribute<DRSVmMigrationActivityVO, String> result;
    public static volatile SingularAttribute<DRSVmMigrationActivityVO, String> reason;
    public static volatile SingularAttribute<DRSVmMigrationActivityVO, String> adviceUuid;
    public static volatile SingularAttribute<DRSVmMigrationActivityVO, Timestamp> endDate;
    public static volatile SingularAttribute<DRSVmMigrationActivityVO, Timestamp> createDate;
    public static volatile SingularAttribute<DRSVmMigrationActivityVO, Timestamp> lastOpDate;
}
