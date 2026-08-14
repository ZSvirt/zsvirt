package org.zstack.zwatch.resnotify;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(ResNotifySubscriptionVO.class)
public class ResNotifySubscriptionVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<ResNotifySubscriptionVO, String> name;
    public static volatile SingularAttribute<ResNotifySubscriptionVO, String> description;
    public static volatile SingularAttribute<ResNotifySubscriptionVO, String> resourceTypes;
    public static volatile SingularAttribute<ResNotifySubscriptionVO, String> eventTypes;
    public static volatile SingularAttribute<ResNotifySubscriptionVO, ResNotifyType> type;
    public static volatile SingularAttribute<ResNotifySubscriptionVO, ResNotifySubscriptionState> state;
    public static volatile SingularAttribute<ResNotifySubscriptionVO, Timestamp> createDate;
    public static volatile SingularAttribute<ResNotifySubscriptionVO, Timestamp> lastOpDate;
}
