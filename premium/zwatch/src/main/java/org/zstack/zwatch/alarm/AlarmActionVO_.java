package org.zstack.zwatch.alarm;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(AlarmActionVO.class)
public class AlarmActionVO_ {
    public static volatile SingularAttribute<AlarmActionVO, String> alarmUuid;
    public static volatile SingularAttribute<AlarmActionVO, String> actionType;
    public static volatile SingularAttribute<AlarmActionVO, String> actionUuid;
    public static volatile SingularAttribute<AlarmVO, Timestamp> createDate;
    public static volatile SingularAttribute<AlarmVO, Timestamp> lastOpDate;
}
