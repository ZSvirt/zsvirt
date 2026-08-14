package org.zstack.header.scheduler;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

/**
 * Created by AlanJager on 2017/6/7.
 */

@StaticMetamodel(SchedulerJobSchedulerTriggerRefVO.class)
public class SchedulerJobSchedulerTriggerRefVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<SchedulerJobSchedulerTriggerRefVO, String> schedulerJobUuid;
    public static volatile SingularAttribute<SchedulerJobSchedulerTriggerRefVO, String> schedulerTriggerUuid;
    public static volatile SingularAttribute<SchedulerJobSchedulerTriggerRefVO, Timestamp> createDate;
    public static volatile SingularAttribute<SchedulerJobSchedulerTriggerRefVO, Timestamp> lastOpDate;
}
