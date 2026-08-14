package org.zstack.zwatch.monitorgroup.entity;

import org.zstack.header.vo.ResourceVO_;
import org.zstack.zwatch.alarm.AlarmStatus;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(MonitorGroupInstanceVO.class)
public class MonitorGroupInstanceVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<MonitorGroupInstanceVO, String> groupUuid;
    public static volatile SingularAttribute<MonitorGroupInstanceVO, String> instanceResourceType;
    public static volatile SingularAttribute<MonitorGroupInstanceVO, String> instanceUuid;
    public static volatile SingularAttribute<MonitorGroupInstanceVO, AlarmStatus> status;
    public static volatile SingularAttribute<MonitorGroupInstanceVO, Timestamp> createDate;
    public static volatile SingularAttribute<MonitorGroupInstanceVO, Timestamp> lastOpDate;
}
