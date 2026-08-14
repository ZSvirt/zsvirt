package org.zstack.zwatch.monitorgroup.entity;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(MonitorGroupAlarmVO.class)
public class MonitorGroupAlarmVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<MonitorGroupAlarmVO, String> groupUuid;
    public static volatile SingularAttribute<MonitorGroupAlarmVO, String> alarmUuid;
    public static volatile SingularAttribute<MonitorGroupAlarmVO, String> metricRuleTemplateUuid;
    public static volatile SingularAttribute<MonitorGroupAlarmVO, Timestamp> createDate;
}
