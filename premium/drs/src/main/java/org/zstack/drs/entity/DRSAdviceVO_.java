package org.zstack.drs.entity;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

/**
 * Created by lining on 2019/12/12.
 */
@StaticMetamodel(DRSAdviceVO.class)
public class DRSAdviceVO_ {
    public static volatile SingularAttribute<DRSAdviceVO, String> uuid;
    public static volatile SingularAttribute<DRSAdviceVO, String> drsUuid;
    public static volatile SingularAttribute<DRSAdviceVO, String> adviceGroupUuid;
    public static volatile SingularAttribute<DRSAdviceVO, String> vmUuid;
    public static volatile SingularAttribute<DRSAdviceVO, String> vmSourceHostUuid;
    public static volatile SingularAttribute<DRSAdviceVO, String> vmTargetHostUuid;
    public static volatile SingularAttribute<DRSAdviceVO, String> reason;
    public static volatile SingularAttribute<DRSAdviceVO, Timestamp> createDate;
    public static volatile SingularAttribute<DRSAdviceVO, Timestamp> lastOpDate;
}
