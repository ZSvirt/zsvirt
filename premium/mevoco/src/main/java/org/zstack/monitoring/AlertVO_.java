package org.zstack.monitoring;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

/**
 * Created by xing5 on 2017/6/15.
 */
@StaticMetamodel(AlertVO.class)
public class AlertVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<AlertVO, String> triggerUuid;
    public static volatile SingularAttribute<AlertVO, String> targetResourceUuid;
    public static volatile SingularAttribute<AlertVO, String> content;
    public static volatile SingularAttribute<AlertVO, MonitorTriggerStatus> triggerStatus;
    public static volatile SingularAttribute<AlertVO, Timestamp> createDate;
    public static volatile SingularAttribute<AlertVO, Timestamp> lastOpDate;
}
