package org.zstack.zwatch.driver;

import org.zstack.zwatch.datatype.*;

import java.util.List;
import java.util.function.Consumer;

public class SimulatorEventDatabaseDriver implements EventDatabaseDriver {
    public interface PersistFunction {
        void persist(List<EventData> data);
    }

    public interface QueryFunction {
        List<EventData> query(EventQueryObject obj);
    }

    private PersistFunction persistFunction;
    private QueryFunction queryFunction;

    public void setQueryFunction(QueryFunction queryFunction) {
        this.queryFunction = queryFunction;
    }

    public void setPersistFunction(PersistFunction persistFunction) {
        this.persistFunction = persistFunction;
    }

    @Override
    public void persist(List<EventData> data) {
        persistFunction.persist(data);
    }

    @Override
    public void audit(List<AuditDataV2> data) {

    }

    @Override
    public void alarm(AlarmDataV2 data) {

    }

    @Override
    public List<EventData> query(EventQueryObject obj) {
        return queryFunction.query(obj);
    }

    @Override
    public List<AuditData> query(AuditQueryObject obj) {
        return null;
    }

    @Override
    public List<AlarmData> query(AlarmQueryObject obj) {
        return null;
    }

    @Override
    public void subscribeEvent(EventSubscriber subscriber, Consumer<EventData> consumer) {
    }

    @Override
    public void unsubscribeEvent(String subscriberUuid) {

    }

    @Override
    public Long getQueryCount(EventQueryObject obj) {
        return null;
    }

    @Override
    public void update(EventData data) {
        return;
    }

    @Override
    public void query(EventQueryObject obj, PagedQueryResultHandler<PagedQueryResult<EventData>> handler) {

    }

    @Override
    public Long getQueryCount(AlarmQueryObject obj) {
        return null;
    }

    @Override
    public void query(AlarmQueryObject obj, PagedQueryResultHandler<PagedQueryResult<AlarmData>> handler) {

    }

    @Override
    public void update(AlarmData data) {

    }

    @Override
    public void consumeEvents(List<EventData> data) {

    }

    @Override
    public List<String> getAllSubscriberUuids() {
        return null;
    }

    @Override
    public void markAsRead(EventQueryObject obj, String accountUuid) {

    }

    @Override
    public void markAsRead(AlarmQueryObject obj, String accountUuid) {

    }
}
