package org.zstack.zwatch.alarm;


import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(AlarmDataAckVO.class)
public class AlarmDataAckVO_ extends AlertDataAckVO_ {
    public static volatile SingularAttribute<AlarmDataAckVO, String> alarmUuid;
}
