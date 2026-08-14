package org.zstack.monitoring.actions;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

/**
 * Created by xing5 on 2017/6/10.
 */
@StaticMetamodel(MonitorTriggerActionVO.class)
public class MonitorTriggerActionVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<MonitorTriggerActionVO, String> name;
    public static volatile SingularAttribute<MonitorTriggerActionVO, String> description;
    public static volatile SingularAttribute<MonitorTriggerActionVO, String> type;
    public static volatile SingularAttribute<MonitorTriggerActionVO, MonitorTriggerActionState> state;
    public static volatile SingularAttribute<MonitorTriggerActionVO, Timestamp> lastOpDate;
    public static volatile SingularAttribute<MonitorTriggerActionVO, Timestamp> createDate;
}
