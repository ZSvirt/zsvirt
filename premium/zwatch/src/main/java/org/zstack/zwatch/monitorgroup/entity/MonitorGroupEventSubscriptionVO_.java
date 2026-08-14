package org.zstack.zwatch.monitorgroup.entity;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(MonitorGroupEventSubscriptionVO.class)
public class MonitorGroupEventSubscriptionVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<MonitorGroupEventSubscriptionVO, String> groupUuid;
    public static volatile SingularAttribute<MonitorGroupEventSubscriptionVO, String> eventSubscriptionUuid;
    public static volatile SingularAttribute<MonitorGroupEventSubscriptionVO, String> eventRuleTemplateUuid;
    public static volatile SingularAttribute<MonitorGroupEventSubscriptionVO, Timestamp> createDate;
}
