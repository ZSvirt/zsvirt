package org.zstack.header.scheduler;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

/**
 * Created by AlanJager on 2017/6/7.
 */

@StaticMetamodel(SchedulerJobVO.class)
public class SchedulerJobVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<SchedulerJobVO, String> targetResourceUuid;
    public static volatile SingularAttribute<SchedulerJobVO, String> name;
    public static volatile SingularAttribute<SchedulerJobVO, String> description;
    public static volatile SingularAttribute<SchedulerJobVO, String> jobClassName;
    public static volatile SingularAttribute<SchedulerJobVO, String> jobData;
    public static volatile SingularAttribute<SchedulerJobVO, String> state;
    public static volatile SingularAttribute<SchedulerJobVO, String> managementNodeUuid;
    public static volatile SingularAttribute<SchedulerJobVO, Timestamp> createDate;
    public static volatile SingularAttribute<SchedulerJobVO, Timestamp> lastOpDate;
}
