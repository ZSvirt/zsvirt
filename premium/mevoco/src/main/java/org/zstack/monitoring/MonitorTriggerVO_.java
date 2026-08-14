package org.zstack.monitoring;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

/**
 * Created by xing5 on 2017/6/5.
 */
@StaticMetamodel(MonitorTriggerVO.class)
public class MonitorTriggerVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<MonitorTriggerVO, String> name;
    public static volatile SingularAttribute<MonitorTriggerVO, String> expression;
    public static volatile SingularAttribute<MonitorTriggerVO, String> recoveryExpression;
    public static volatile SingularAttribute<MonitorTriggerVO, String> contextData;
    public static volatile SingularAttribute<MonitorTriggerVO, String> description;
    public static volatile SingularAttribute<MonitorTriggerVO, Integer> duration;
    public static volatile SingularAttribute<MonitorTriggerVO, MonitorTriggerStatus> status;
    public static volatile SingularAttribute<MonitorTriggerVO, MonitorTriggerState> state;
    public static volatile SingularAttribute<MonitorTriggerVO, String> targetResourceUuid;
    public static volatile SingularAttribute<MonitorTriggerVO, Timestamp> lastStatusChangeTime;
    public static volatile SingularAttribute<MonitorTriggerVO, Timestamp> createDate;
    public static volatile SingularAttribute<MonitorTriggerVO, Timestamp> lastOpDate;
}
