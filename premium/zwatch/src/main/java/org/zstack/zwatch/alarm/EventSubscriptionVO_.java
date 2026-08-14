package org.zstack.zwatch.alarm;

import org.zstack.header.vo.ResourceVO_;
import org.zstack.zwatch.datatype.EmergencyLevel;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(EventSubscriptionVO.class)
public class EventSubscriptionVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<EventSubscriptionVO, String> name;
    public static volatile SingularAttribute<EventSubscriptionVO, String> namespace;
    public static volatile SingularAttribute<EventSubscriptionVO, String> eventName;
    public static volatile SingularAttribute<EventSubscriptionVO, EventSubscriptionState> state;
    public static volatile SingularAttribute<EventSubscriptionVO, Timestamp> createDate;
    public static volatile SingularAttribute<EventSubscriptionVO, Timestamp> lastOpDate;
    public static volatile SingularAttribute<EventSubscriptionVO, EmergencyLevel> emergencyLevel;

}
