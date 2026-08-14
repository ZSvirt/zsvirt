package org.zstack.zwatch.alarm.activealarm.entity;

import org.zstack.zwatch.datatype.EmergencyLevel;
import org.zstack.zwatch.ruleengine.ComparisonOperator;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(ActiveAlarmTemplateVO.class)
public class ActiveAlarmTemplateVO_ {
    public static volatile SingularAttribute<ActiveAlarmTemplateVO, String> uuid;
    public static volatile SingularAttribute<ActiveAlarmTemplateVO, String> alarmName;
    public static volatile SingularAttribute<ActiveAlarmTemplateVO, ComparisonOperator> comparisonOperator;
    public static volatile SingularAttribute<ActiveAlarmTemplateVO, Integer> period;
    public static volatile SingularAttribute<ActiveAlarmTemplateVO, Integer> repeatInterval;
    public static volatile SingularAttribute<ActiveAlarmTemplateVO, Integer> repeatCount;
    public static volatile SingularAttribute<ActiveAlarmTemplateVO, String> namespace;
    public static volatile SingularAttribute<ActiveAlarmTemplateVO, String> metricName;
    public static volatile SingularAttribute<ActiveAlarmTemplateVO, Double> threshold;
    public static volatile SingularAttribute<ActiveAlarmTemplateVO, EmergencyLevel> emergencyLevel;
    public static volatile SingularAttribute<ActiveAlarmTemplateVO, String> labels;
    public static volatile SingularAttribute<ActiveAlarmTemplateVO, Timestamp> createDate;
    public static volatile SingularAttribute<ActiveAlarmTemplateVO, Timestamp> lastOpDate;
}
