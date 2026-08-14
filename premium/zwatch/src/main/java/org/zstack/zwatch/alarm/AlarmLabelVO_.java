package org.zstack.zwatch.alarm;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(AlarmLabelVO.class)
public class AlarmLabelVO_ extends LabelAO_ {
    public static volatile SingularAttribute<AlarmLabelVO, Long> alarmUuid;
}
