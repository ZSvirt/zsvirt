package org.zstack.zwatch.migratedb;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(AlarmRecordsVO.class)
public class AlarmRecordsVO_ {
    public static volatile SingularAttribute<AlarmRecordsVO, Long> id;
    public static volatile SingularAttribute<AlarmRecordsVO, Long> createTime;
    public static volatile SingularAttribute<AlarmRecordsVO, String> alarmName;
    public static volatile SingularAttribute<AlarmRecordsVO, String> alarmStatus;
    public static volatile SingularAttribute<AlarmRecordsVO, String> alarmUuid;
    public static volatile SingularAttribute<AlarmRecordsVO, String> comparisonOperator;
    public static volatile SingularAttribute<AlarmRecordsVO, String> context;
    public static volatile SingularAttribute<AlarmRecordsVO, String> dataUuid;
    public static volatile SingularAttribute<AlarmRecordsVO, String> emergencyLevel;
    public static volatile SingularAttribute<AlarmRecordsVO, String> labels;
    public static volatile SingularAttribute<AlarmRecordsVO, String> metricName;
    public static volatile SingularAttribute<AlarmRecordsVO, String> metricValue;
    public static volatile SingularAttribute<AlarmRecordsVO, String> namespace;
    public static volatile SingularAttribute<AlarmRecordsVO, Integer> period;
    public static volatile SingularAttribute<AlarmRecordsVO, Boolean> readStatus;
    public static volatile SingularAttribute<AlarmRecordsVO, String> operatorAccountUuid;
    public static volatile SingularAttribute<AlarmRecordsVO, String> resourceType;
    public static volatile SingularAttribute<AlarmRecordsVO, String> resourceUuid;
    public static volatile SingularAttribute<AlarmRecordsVO, Double> threshold;
    public static volatile SingularAttribute<AlarmRecordsVO, Long> hour;
}
