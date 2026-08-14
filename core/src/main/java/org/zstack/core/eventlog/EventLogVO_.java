package org.zstack.core.eventlog;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(EventLogVO.class)
public class EventLogVO_ {
    public static volatile SingularAttribute<EventLogVO, Long> id;
    public static volatile SingularAttribute<EventLogVO, Long> time;
    public static volatile SingularAttribute<EventLogVO, String> content;
    public static volatile SingularAttribute<EventLogVO, String> resourceUuid;
    public static volatile SingularAttribute<EventLogVO, String> resourceType;
    public static volatile SingularAttribute<EventLogVO, String> category;
    public static volatile SingularAttribute<EventLogVO, String> trackingId;
    public static volatile SingularAttribute<EventLogVO, EventLogType> type;
    public static volatile SingularAttribute<EventLogVO, Timestamp> createDate;
}
