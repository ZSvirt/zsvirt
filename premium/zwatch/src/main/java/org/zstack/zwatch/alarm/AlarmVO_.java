package org.zstack.zwatch.alarm;

import org.zstack.header.vo.ResourceVO_;
import org.zstack.monitoring.media.MediaConstants;
import org.zstack.zwatch.datatype.EmergencyLevel;
import org.zstack.zwatch.ruleengine.ComparisonOperator;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(AlarmVO.class)
public class AlarmVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<AlarmVO, String> name;
    public static volatile SingularAttribute<AlarmVO, String> description;
    public static volatile SingularAttribute<AlarmVO, ComparisonOperator> comparisonOperator;
    public static volatile SingularAttribute<AlarmVO, Integer> period;
    public static volatile SingularAttribute<AlarmVO, Integer> repeatInterval;
    public static volatile SingularAttribute<AlarmVO, Integer> repeatCount;
    public static volatile SingularAttribute<AlarmVO, Integer> namespace;
    public static volatile SingularAttribute<AlarmVO, Integer> metricName;
    public static volatile SingularAttribute<AlarmVO, Double> threshold;
    public static volatile SingularAttribute<AlarmVO, Boolean> enableRecovery;
    public static volatile SingularAttribute<AlarmVO, AlarmStatus> status;
    public static volatile SingularAttribute<AlarmVO, AlarmState> state;
    public static volatile SingularAttribute<AlarmVO, Timestamp> createDate;
    public static volatile SingularAttribute<AlarmVO, Timestamp> lastOpDate;
    public static volatile SingularAttribute<AlarmVO, EmergencyLevel> emergencyLevel;
}
