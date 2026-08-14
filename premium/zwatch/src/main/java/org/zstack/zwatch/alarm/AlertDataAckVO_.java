package org.zstack.zwatch.alarm;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(AlertDataAckVO.class)
public class AlertDataAckVO_ {
    public static volatile SingularAttribute<AlertDataAckVO, String> alertDataUuid;
    public static volatile SingularAttribute<AlertDataAckVO, String> alertType;
    public static volatile SingularAttribute<AlertDataAckVO, Long> ackPeriod;
    public static volatile SingularAttribute<AlertDataAckVO, String> resourceUuid;
    public static volatile SingularAttribute<AlertDataAckVO, Timestamp> ackDate;
    public static volatile SingularAttribute<AlertDataAckVO, Boolean> resumeAlert;
}
