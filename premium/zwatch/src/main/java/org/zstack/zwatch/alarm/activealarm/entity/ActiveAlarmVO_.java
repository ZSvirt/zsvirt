package org.zstack.zwatch.alarm.activealarm.entity;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(ActiveAlarmVO.class)
public class ActiveAlarmVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<ActiveAlarmVO, String> templateUuid;
    public static volatile SingularAttribute<ActiveAlarmVO, String> alarmUuid;
    public static volatile SingularAttribute<ActiveAlarmVO, String> namespace;
    public static volatile SingularAttribute<ActiveAlarmVO, Timestamp> createDate;
}
