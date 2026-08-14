package org.zstack.zwatch.alarm;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(EventSubscriptionActionVO.class)
public class EventSubscriptionActionVO_ {
    public static volatile SingularAttribute<EventSubscriptionActionVO, String> subscriptionUuid;
    public static volatile SingularAttribute<EventSubscriptionActionVO, String> actionType;
    public static volatile SingularAttribute<EventSubscriptionActionVO, String> actionUuid;
    public static volatile SingularAttribute<AlarmVO, Timestamp> createDate;
    public static volatile SingularAttribute<AlarmVO, Timestamp> lastOpDate;
}
