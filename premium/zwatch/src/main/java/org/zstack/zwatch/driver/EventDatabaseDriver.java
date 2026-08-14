package org.zstack.zwatch.driver;

import org.zstack.zwatch.datatype.*;

import java.util.List;
import java.util.function.Consumer;

public interface EventDatabaseDriver {
    class EventSubscriber {
        public String uuid;
        public String namespace;
        public String eventName;
        public List<Label> labels;
    }

    void persist(List<EventData> data);

    void audit(List<AuditDataV2> data);

    void alarm(AlarmDataV2 data);

    List<EventData> query(EventQueryObject obj);

    void query(EventQueryObject obj, PagedQueryResultHandler<PagedQueryResult<EventData>> handler);

    void query(AlarmQueryObject obj, PagedQueryResultHandler<PagedQueryResult<AlarmData>> handler);

    Long getQueryCount(EventQueryObject obj);

    void update(EventData data);

    void update(AlarmData data);

    List<AuditData> query(AuditQueryObject obj);

    List<AlarmData> query(AlarmQueryObject obj);

    Long getQueryCount(AlarmQueryObject obj);

    void subscribeEvent(EventSubscriber subscriber, Consumer<EventData> consumer);

    void unsubscribeEvent(String subscriberUuid);

    void consumeEvents(List<EventData> data);

    List<String> getAllSubscriberUuids();

    void markAsRead(EventQueryObject obj, String accountUuid);

    void markAsRead(AlarmQueryObject obj, String accountUuid);
}
