package org.zstack.zwatch.alarm;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(EventDataAckVO.class)
public class EventDataAckVO_ extends AlertDataAckVO_ {
    public static volatile SingularAttribute<EventDataAckVO, String> eventSubscriptionUuid;
}
