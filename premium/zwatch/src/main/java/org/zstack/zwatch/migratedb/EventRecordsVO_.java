package org.zstack.zwatch.migratedb;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(EventRecordsVO.class)
public class EventRecordsVO_ {
    public static volatile SingularAttribute<EventRecordsVO, Long> id;
    public static volatile SingularAttribute<EventRecordsVO, Long> createTime;
    public static volatile SingularAttribute<EventRecordsVO, String> namespace;
    public static volatile SingularAttribute<EventRecordsVO, String> name;
    public static volatile SingularAttribute<EventRecordsVO, String> emergencyLevel;
    public static volatile SingularAttribute<EventRecordsVO, String> resourceId;
    public static volatile SingularAttribute<EventRecordsVO, String> resourceName;
    public static volatile SingularAttribute<EventRecordsVO, String> error;
    public static volatile SingularAttribute<EventRecordsVO, String> dataUuid;
    public static volatile SingularAttribute<EventRecordsVO, String> subscriptionUuid;
    public static volatile SingularAttribute<EventRecordsVO, Boolean> readStatus;
    public static volatile SingularAttribute<EventRecordsVO, String> labels;
    public static volatile SingularAttribute<AlarmRecordsVO, Long> hour;
}
