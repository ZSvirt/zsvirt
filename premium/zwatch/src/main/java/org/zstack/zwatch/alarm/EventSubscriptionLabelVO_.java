package org.zstack.zwatch.alarm;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(EventSubscriptionLabelVO.class)
public class EventSubscriptionLabelVO_ extends LabelAO_ {
    public static volatile SingularAttribute<EventSubscriptionLabelVO, String> subscriptionUuid;
}
