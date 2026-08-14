package org.zstack.ha;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(VmHaVO.class)
public class VmHaVO_ {
    public static volatile SingularAttribute<VmHaVO, String> uuid;
    public static volatile SingularAttribute<VmHaVO, VmHaLevel> haLevel;
    public static volatile SingularAttribute<VmHaVO, Timestamp> haLevelUpdateTime;
    public static volatile SingularAttribute<VmHaVO, String> inhibitionReason;
    public static volatile SingularAttribute<VmHaVO, Timestamp> inhibitionTime;
}
