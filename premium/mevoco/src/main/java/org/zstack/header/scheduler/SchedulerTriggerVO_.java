package org.zstack.header.scheduler;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

/**
 * Created by AlanJager on 2017/6/7.
 */
@StaticMetamodel(SchedulerTriggerVO.class)
public class SchedulerTriggerVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<SchedulerTriggerVO, String> name;
    public static volatile SingularAttribute<SchedulerTriggerVO, String> description;
    public static volatile SingularAttribute<SchedulerTriggerVO, String> schedulerType;
    public static volatile SingularAttribute<SchedulerTriggerVO, Integer> schedulerInterval;
    public static volatile SingularAttribute<SchedulerTriggerVO, Integer> repeatCount;
    public static volatile SingularAttribute<SchedulerTriggerVO, String> managementNodeUuid;
    public static volatile SingularAttribute<SchedulerTriggerVO, Timestamp> createDate;
    public static volatile SingularAttribute<SchedulerTriggerVO, Timestamp> startTime;
    public static volatile SingularAttribute<SchedulerTriggerVO, Timestamp> stopTime;
    public static volatile SingularAttribute<SchedulerTriggerVO, Timestamp> lastOpDate;
    public static volatile SingularAttribute<SchedulerTriggerVO, String> cron;
}
