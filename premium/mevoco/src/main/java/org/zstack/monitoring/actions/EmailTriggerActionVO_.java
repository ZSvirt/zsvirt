package org.zstack.monitoring.actions;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

/**
 * Created by xing5 on 2017/7/8.
 */
@StaticMetamodel(EmailTriggerActionVO.class)
public class EmailTriggerActionVO_ extends MonitorTriggerActionVO_ {
    public static volatile SingularAttribute<MonitorTriggerActionVO, String> email;
    public static volatile SingularAttribute<MonitorTriggerActionVO, String> mediaUuid;
}
