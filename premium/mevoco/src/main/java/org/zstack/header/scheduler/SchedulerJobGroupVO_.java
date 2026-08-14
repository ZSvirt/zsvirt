package org.zstack.header.scheduler;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(SchedulerJobGroupVO.class)
public class SchedulerJobGroupVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<SchedulerJobGroupVO, String> name;
    public static volatile SingularAttribute<SchedulerJobGroupVO, String> description;
    public static volatile SingularAttribute<SchedulerJobGroupVO, String> jobData;
    public static volatile SingularAttribute<SchedulerJobGroupVO, String> jobType;
    public static volatile SingularAttribute<SchedulerJobGroupVO, String> jobClassName;
    public static volatile SingularAttribute<SchedulerJobGroupVO, String> state;
    public static volatile SingularAttribute<SchedulerJobGroupVO, String> zoneUuid;
    public static volatile SingularAttribute<SchedulerJobGroupVO, String> managementNodeUuid;
    public static volatile SingularAttribute<SchedulerJobGroupVO, Timestamp> createDate;
    public static volatile SingularAttribute<SchedulerJobGroupVO, Timestamp> lastOpDate;
}
