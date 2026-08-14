package org.zstack.zwatch.migratedb;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.ResourceDestinationMaker;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.thread.PeriodicTask;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.Component;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.identity.AccountConstant;
import org.zstack.header.managementnode.ManagementNodeReadyExtensionPoint;
import org.zstack.identity.Account;
import org.zstack.identity.AccountManager;
import org.zstack.utils.FieldUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;
import org.zstack.zwatch.ZWatchConstants;
import org.zstack.zwatch.ZWatchGlobalConfig;
import org.zstack.zwatch.ZWatchGlobalProperty;
import org.zstack.zwatch.datatype.AlarmData;
import org.zstack.zwatch.datatype.AlarmDataV2;
import org.zstack.zwatch.datatype.AlarmQueryObject;
import org.zstack.zwatch.datatype.AuditData;
import org.zstack.zwatch.datatype.AuditDataV2;
import org.zstack.zwatch.datatype.AuditQueryObject;
import org.zstack.zwatch.datatype.EventData;
import org.zstack.zwatch.datatype.EventFamily;
import org.zstack.zwatch.datatype.EventQueryObject;
import org.zstack.zwatch.datatype.Label;
import org.zstack.zwatch.datatype.Namespace;
import org.zstack.zwatch.driver.AfterSaveAuditsExtensionPoint;
import org.zstack.zwatch.driver.EventDatabaseDriver;
import org.zstack.zwatch.driver.PagedQueryResult;
import org.zstack.zwatch.driver.PagedQueryResultHandler;
import javax.persistence.Query;
import javax.transaction.Transactional;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.argerr;


public class MigrateDBEventDatabaseDriver implements EventDatabaseDriver, Component, ManagementNodeReadyExtensionPoint {
    private static final CLogger logger = Utils.getLogger(MigrateDBEventDatabaseDriver.class);


 /*   public void MigrateDBEventDatabaseDriver() {
        return new MigrateDBEventDatabaseDriver();
    }*/

    @Autowired
    private ResourceDestinationMaker destinationMaker;

    @Autowired
    private AccountManager acmgr;

    @Autowired
    private ThreadFacade thdf;

    @Autowired
    protected PluginRegistry pluginRgty;

    private Future retentionAlarmEventAuditTask;
    private boolean retaining = false;

    private static DatabaseFacade dbf;

    private static Gson gson = new Gson();

    // grouped by event name which may be duplicated across different namespace
    private Map<String, List<EventFamily>> eventFamilies = new HashMap<>();

    private interface EventConsumer extends Consumer<EventData> {
        EventSubscriber getEventSubscriber();
    }

    private Map<String, ConcurrentLinkedDeque<EventConsumer>> eventSubscribers = new ConcurrentHashMap<>();

    @Override
    public boolean start() {
        Namespace.namespaces.forEach((k, namespaces) -> {
            namespaces.forEach(ns -> {
                if (ns.getEvents() != null) {
                    ns.getEvents().forEach(ef -> {
                        List<EventFamily> efs = eventFamilies.computeIfAbsent(ef.getName(), x->new ArrayList<>());
                        efs.add(ef);
                    });
                }
            });
        });

        return true;
    }

    @Override
    public void managementNodeReady() {
        if (retentionAlarmEventAuditTask != null) {
            return;
        }

        retentionAlarmEventAuditTask = thdf.submitPeriodicTask(new PeriodicTask() {
            @Override
            public TimeUnit getTimeUnit() {
                if (CoreGlobalProperty.UNIT_TEST_ON) {
                    return TimeUnit.SECONDS;
                }

                return TimeUnit.HOURS;
            }

            @Override
            public long getInterval() {
                return 1;
            }

            @Override
            public String getName() {
                return "zwatch-alarm-event-audit-retention";
            }

            @Override
            public void run() {
                if (!destinationMaker.isManagedByUs(AccountConstant.INITIAL_SYSTEM_ADMIN_UUID)) {
                    return;
                }

                if (retaining) {
                    return;
                }
                retaining = true;

                int durationOfDay = ZWatchGlobalConfig.ALARM_RETENTION_DURATION.value(Integer.class);
                doRetentionDuration(AlarmRecordsVO.class, durationOfDay);
                int retentionOfQuantity = ZWatchGlobalConfig.ALARM_RETENTION_THRESHOLD.value(Integer.class);
                doDataRetentionThreshold(AlarmRecordsVO.class, retentionOfQuantity);

                durationOfDay = ZWatchGlobalConfig.EVENT_RETENTION_DURATION.value(Integer.class);
                doRetentionDuration(EventRecordsVO.class, durationOfDay);
                retentionOfQuantity = ZWatchGlobalConfig.EVENT_RETENTION_THRESHOLD.value(Integer.class);
                doDataRetentionThreshold(EventRecordsVO.class, retentionOfQuantity);

                durationOfDay = ZWatchGlobalConfig.AUDIT_RETENTION_DURATION.value(Integer.class);
                doRetentionDuration(AuditsVO.class, durationOfDay);
                retentionOfQuantity = ZWatchGlobalConfig.AUDIT_RETENTION_THRESHOLD.value(Integer.class);
                doDataRetentionThreshold(AuditsVO.class, retentionOfQuantity);

                retaining = false;
            }

            private void doDataRetentionThreshold(Class targetTableClass, int threshold) {
                String tableName = targetTableClass.getSimpleName();
                if (threshold <= 0) {
                    return;
                }

                long count = Q.New(targetTableClass).count();
                if (count <= threshold) {
                    return;
                }

                long needDeleteRow = count - threshold;
                if (needDeleteRow < ZWatchGlobalProperty.RETENTION_THRESHOLD_BUFFER) {
                    return;
                }

                int batchRow = 10000;
                for (long deletedRow = 0; deletedRow < needDeleteRow; deletedRow += batchRow) {
                    long limit = needDeleteRow - deletedRow > batchRow ? batchRow : needDeleteRow - deletedRow;
                    List<Long> ids = SQL.New(String.format("select id from %s order by createTime asc", tableName), Long.class)
                            .limit((int)limit)
                            .list();
                    SQL.New(String.format("delete from %s where id in :ids", tableName))
                            .param("ids", ids)
                            .execute();
                }
            }

            private void doRetentionDuration(Class targetTableClass, int durationOfDay) {
                if (durationOfDay <= 0) return;
                String tableName = targetTableClass.getSimpleName();
                long time = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(durationOfDay);
                String sql = String.format("select min(createTime) from %s", tableName);
                Long minCreateTime = SQL.New(sql, Long.class)
                        .find();
                if (minCreateTime == null || minCreateTime > time) {
                    return;
                }

                if ((time - minCreateTime) < TimeUnit.DAYS.toMillis(1)) {
                    return;
                }

                SQL.New(String.format("delete from %s where createTime < :createTime", tableName))
                        .param("createTime", time)
                        .execute();
            }
        });
    }

    @Override
    public boolean stop() {
        if (retentionAlarmEventAuditTask != null) {
            retentionAlarmEventAuditTask.cancel(true);
        }

        return true;
    }

    @Override
    public void persist(List<EventData> data) {
        data.forEach(d -> {
            try {
                EventRecordsVO vo = new EventRecordsVO();
                vo.setAccountUuid(d.getAccountUuid());
                vo.setDataUuid(d.getDataUuid());
                vo.setEmergencyLevel(d.getEmergencyLevel().name());
                vo.setError(d.getError());
                vo.setName(d.getName());
                vo.setNamespace(d.getNamespace());
                vo.setReadStatus(StringUtils.equals(d.getReadStatus(), ZWatchConstants.DATA_READ_STATUS_READ));
                vo.setResourceId(d.getResourceId());
                vo.setResourceName(d.getResourceName());
                vo.setSubscriptionUuid(d.getSubscriptionUuid());
                vo.setCreateTime(System.currentTimeMillis());
                long createTimeWithoutMinSec = TimeUnit.HOURS.toSeconds(TimeUnit.MILLISECONDS.toHours(vo.getCreateTime()));
                vo.setHour(createTimeWithoutMinSec);

                if (d.getLabels().size() > 0){
                    Map<String, Object> labels = new HashMap<>();
                    d.getLabels().keySet().forEach(k -> {
                        labels.put(k, d.getLabels().get(k));
                    });
                    vo.setLabels(JSONObjectUtil.toJsonString(labels));
                }

                if (dbf == null) {
                    dbf = Platform.getComponentLoader().getComponent(DatabaseFacade.class);
                }
                dbf.persist(vo);
            } catch (Throwable t) {
                logger.warn(String.format("unable to persist event data:\n%s", JSONObjectUtil.toJsonString(d)), t);
            }
        });
    }

    private static String getTruncatedResponseIfExceedLimit(JsonObject responseData) {
        final int textMaxLength = 65535;
        String responseDump = gson.toJson(responseData);
        responseDump = StringUtils.length(responseDump) > textMaxLength ?
                StringUtils.left(responseDump, textMaxLength) : responseDump;
        return responseDump;
    }

    @Transactional
    @Override
    public void audit(List<AuditDataV2> data) {

        data.forEach(d -> {
            try {
                AuditsVO vo = new AuditsVO();
                JsonObject reqData = JsonParser.parseString(d.getRequestDump()).getAsJsonObject();
                if (reqData.has("headers")) {
                    reqData.remove("headers");
                }
                JsonObject resData = JsonParser.parseString(d.getResponseDump()).getAsJsonObject();
                if (resData.has("headers")) {
                    resData.remove("headers");
                }
                vo.setApiName(d.getApiName());
                vo.setClientBrowser(d.getClientBrowser());
                vo.setClientIp(d.getClientIp());
                vo.setDuration(d.getDuration());
                vo.setError(d.getError());
                vo.setOperator(d.getOperator());
                vo.setRequestDump(gson.toJson(reqData));
                vo.setResponseDump(getTruncatedResponseIfExceedLimit(resData));
                vo.setResourceType(d.getResourceType());
                vo.setResourceUuid(d.getResourceUuid());
                vo.setResourceName(d.getResourceName());
                vo.setRequestUuid(d.getRequestUuid());
                vo.setOperatorAccountUuid(d.getOperatorAccountUuid());
                vo.setSuccess(d.getError() == null);
                vo.setCreateTime(System.currentTimeMillis());
                if (dbf == null) {
                    dbf = Platform.getComponentLoader().getComponent(DatabaseFacade.class);
                }
                dbf.persist(vo);
                pluginRgty.getExtensionList(AfterSaveAuditsExtensionPoint.class).forEach(point -> point.saveEncryptAfterSaveAudits(vo));
            } catch (Throwable t) {
                logger.warn(String.format("unable to persist audit data:\n%s", JSONObjectUtil.toJsonString(d)), t);
            }
        });
    }

    @Override
    public void alarm(AlarmDataV2 data) {
        try {
            AlarmRecordsVO vo = new AlarmRecordsVO();
            vo.setAccountUuid(data.getAccountUuid());
            vo.setAlarmName(data.getAlarmName());
            vo.setAlarmStatus(data.getAlarmStatus());
            vo.setComparisonOperator(data.getComparisonOperator());
            vo.setAlarmUuid(data.getAlarmUuid());
            vo.setContext(data.getContext());
            vo.setDataUuid(data.getDataUuid());
            vo.setEmergencyLevel(data.getEmergencyLevel());
            vo.setLabels(data.getLabels());
            vo.setMetricName(data.getMetricName());
            vo.setMetricValue(data.getMetricValue());
            vo.setNamespace(data.getNamespace());
            vo.setPeriod(data.getPeriod());
            vo.setReadStatus(StringUtils.equals(data.getReadStatus(), ZWatchConstants.DATA_READ_STATUS_READ));
            vo.setResourceType(data.getResourceType());
            vo.setResourceUuid(data.getResourceUuid());
            vo.setThreshold(data.getThreshold());
            vo.setDataUuid(data.getDataUuid());
            vo.setCreateTime(System.currentTimeMillis());
            long createTimeWithoutMinSec = TimeUnit.HOURS.toSeconds(TimeUnit.MILLISECONDS.toHours(vo.getCreateTime()));
            vo.setHour(createTimeWithoutMinSec);

            if (dbf == null) {
                dbf = Platform.getComponentLoader().getComponent(DatabaseFacade.class);
            }
            dbf.persist(vo);

        } catch (Throwable t) {
            logger.warn(String.format("unable to persist alarm data:\n%s", JSONObjectUtil.toJsonString(data)), t);
        }
    }

    @Override
    public List<EventData> query(EventQueryObject obj) {
        return new MysqlEventQueryObject(obj).query();
    }

    @Override
    public void query(EventQueryObject obj, PagedQueryResultHandler<PagedQueryResult<EventData>> handler) {

    }

    @Override
    public void query(AlarmQueryObject obj, PagedQueryResultHandler<PagedQueryResult<AlarmData>> handler) {

    }

    @Override
    public Long getQueryCount(EventQueryObject obj) {
        return new MysqlEventQueryObject(obj).count();
    }

    @Override
    public void update(EventData data) {
    }

    @Override
    public void update(AlarmData data) {
    }

    @Override
    public List<AuditData> query(AuditQueryObject obj) {
        return new MysqlAuditQueryObject(obj).query();
    }

    @Override
    public List<AlarmData> query(AlarmQueryObject obj) {
        return new MysqlAlarmQueryObject(obj).query();
    }

    @Override
    public Long getQueryCount(AlarmQueryObject obj) {
        return new MysqlAlarmQueryObject(obj).count();
    }

    @Override
    public void subscribeEvent(EventSubscriber subscriber, Consumer<EventData> consumer) {
        String key = String.format("%s.%s", subscriber.namespace, subscriber.eventName);
        ConcurrentLinkedDeque<MigrateDBEventDatabaseDriver.EventConsumer> consumers = eventSubscribers.computeIfAbsent(key, k->new ConcurrentLinkedDeque<>());
        consumers.add(new MigrateDBEventDatabaseDriver.EventConsumer() {
            @Override
            public void accept(EventData data) {
                consumer.accept(data);
            }

            @Override
            public EventSubscriber getEventSubscriber() {
                return subscriber;
            }
        });
    }

    @Override
    public void unsubscribeEvent(String subscriberUuid) {
        for (ConcurrentLinkedDeque<EventConsumer> queue : eventSubscribers.values()) {
            Iterator<EventConsumer> it = queue.iterator();
            while (it.hasNext()) {
                EventConsumer c = it.next();
                if (c.getEventSubscriber().uuid.equals(subscriberUuid)) {
                    it.remove();
                    return;
                }
            }
        }
    }

    @Override
    public void consumeEvents(List<EventData> data) {
        List<EventData> subscribeEvents = new ArrayList<>();

        data.forEach(event -> {
            Queue<EventConsumer> queue = eventSubscribers.get(String.format("%s.%s", event.getNamespace(), event.getName()));
            if (queue == null) {
                return;
            }

            Namespace ns = Namespace.getEventNameSpace(event.getNamespace(), event.getName());

            queue.forEach(consumer-> {
                EventSubscriber subscriber = consumer.getEventSubscriber();

                if (!ns.resourceEventIsMatch(event.getResourceId(), subscriber.uuid)) {
                    return;
                }

                if (event.getAccountUuid() != null) {
                    String accountUuid = acmgr.getOwnerAccountUuidOfResource(subscriber.uuid);
                    if (!Account.isAdminPermission(accountUuid)) {
                        if (!accountUuid.equals(event.getAccountUuid())) {
                            return;
                        }
                    }
                }

                if (subscriber.labels == null || subscriber.labels.isEmpty()) {
                    subscribeEvents.add(event);
                    consumer.accept(new EventData(event));
                    return;
                }

                boolean isMatch = true;
                Map<String, String> eventLabels = event.asQueryLabels();
                for (Label label : subscriber.labels) {
                    String labelValue = eventLabels.get(label.getKey());
                    if (!label.match(labelValue)) {
                        isMatch = false;
                        break;
                    }
                }

                if (isMatch) {
                    subscribeEvents.add(event);
                    consumer.accept(new EventData(event));
                }
            });
        });

        List<EventData> noSubscribeEvents = new ArrayList<>(data);
        noSubscribeEvents.removeAll(subscribeEvents);
        if (noSubscribeEvents.size() == 0) {
            return;
        }
        noSubscribeEvents = noSubscribeEvents.stream()
                .filter(eventData -> eventData.getDataUuid() != null && destinationMaker.isManagedByUs(eventData.getResourceId())) //判断资源是不是当前节点管的
                .collect(Collectors.toList());
        // no necessary to write normal events 第三方的数据只保存Normal级别以上的 并且修改emergent为important级别给系统外对接资源告警
        if (!MigrateDBGlobalProperty.MIGRATEDB_WRITE_NORMAL_EVENTS) {
            noSubscribeEvents = noSubscribeEvents.stream().filter(eventData -> eventData.getEmergencyLevel() != EventFamily.EmergencyLevel.Normal)
                    .collect(Collectors.toList());
        }
        for (EventData eventData : noSubscribeEvents) {
            if (eventData.getEmergencyLevel() == EventFamily.EmergencyLevel.Emergent) {
                eventData.setEmergencyLevel(EventFamily.EmergencyLevel.Important.name());
            }
        }
    }

    @Override
    public List<String> getAllSubscriberUuids() {
        List<String> uuids = new ArrayList<>();
        for (ConcurrentLinkedDeque<EventConsumer> queue : eventSubscribers.values()) {
            Iterator<EventConsumer> it = queue.iterator();
            while (it.hasNext()) {
                EventConsumer c = it.next();
                uuids.add(c.getEventSubscriber().uuid);
            }
        }

        return uuids;
    }

    @Override
    public void markAsRead(EventQueryObject obj, String accountUuid) {
        new MysqlEventQueryObject(obj).markAsRead(accountUuid);
    }

    @Override
    public void markAsRead(AlarmQueryObject obj, String accountUuid) {
        new MysqlAlarmQueryObject(obj).markAsRead(accountUuid);
    }

    class MysqlEventQueryObject extends EventQueryObject {
        public MysqlEventQueryObject(EventQueryObject other) {
            super(other);
        }

        Long count() {
            String qstr = toQueryCountString();
            if (logger.isTraceEnabled()) {
                logger.trace(qstr);
            }
            return SQL.New(qstr, Long.class).find();
        }



        List<EventData> query() {
            String qstr = toQueryString();
            List<EventRecordsVO> eventdata;
            if (getLimit() != -1) {
                eventdata = SQL.New(qstr, EventRecordsVO.class).limit(getLimit()).offset(getOffset()).list();
            } else {
                eventdata = SQL.New(qstr, EventRecordsVO.class).offset(getOffset()).list();
            }
            if (logger.isTraceEnabled()) {
                logger.trace(qstr);
            }

            List<EventData> data = new ArrayList<>();
            eventdata.forEach(r -> {
                data.add(EventRecordsInventory.toEventData(r));
            });

            return data;
        }

        String toQueryCountString() {
            List<String> builder = new ArrayList<>();
            builder.add("SELECT count(a) FROM EventRecordsVO a");
            boolean needWhere = !getLabels().isEmpty() || getStartTime() != null || getEndTime() != null || getTime() != null || !getWhereConditions().isEmpty();
            if (needWhere) {
                builder.add("WHERE");
                List<String> conditions = new ArrayList<>();

                if (!getLabels().isEmpty()) {
                    Optional<Label> opt = getLabels().stream().filter(l -> l.getKey().equals(ZWatchConstants.LABEL_NAME) && l.getOp() == Label.Operator.Equal).findFirst();
                    EventFamily ef = getEfByLabelList(opt);
                    getLabels().forEach(label -> conditions.add(labelToCondition(label, ef)));
                }

                if (getStartTime() != null) {
                    conditions.add(String.format("a.createTime >= %s", getStartTime()));
                }
                if (getEndTime() != null) {
                    conditions.add(String.format("a.createTime <= %s", getEndTime()));
                }
                if (getTime() != null) {
                    conditions.add(String.format("a.createTime = %s", getTime()));
                }
                if (!getWhereConditions().isEmpty()) {
                    List<String> mysqlWhereConditions = new ArrayList<String>();
                    getWhereConditions().stream().forEach(cond -> {
                        String newsql = convertToMysqlSQL(cond, EventRecordsVO.class);
                        mysqlWhereConditions.add(newsql);
                    });
                    conditions.addAll(mysqlWhereConditions);
                }

                builder.add(StringUtils.join(conditions, " and "));
            }

            return StringUtils.join(builder, " ");
        }

        EventFamily getEfByLabelList(Optional<Label> opt) {
            // get event family by label,
            EventFamily ef = null;
            if (opt.isPresent()) {
                Label name = opt.get();
                if (isMoreThanOneEventFamilyWithName(name.getValue())) {
                    opt = getLabels().stream().filter(l -> l.getKey().equals(ZWatchConstants.LABEL_NAMESPACE) && l.getOp() == Label.Operator.Equal).findFirst();
                    if (!opt.isPresent()) {
                        throw new OperationFailureException(argerr("there are multiple EventFamily with the name[%s], you must specify" +
                                " the label[%s]", name.getValue(), ZWatchConstants.LABEL_NAMESPACE));
                    }

                    String namespace = opt.get().getValue();
                    ef = getEventFamilyByNameThenByNamespace(name.getValue(), namespace);
                } else {
                    ef = getEventFamilyByNameThenByNamespace(name.getValue(), null);
                }
            }
            return ef;
        }

        String toQueryString() {
            List<String> builder = new ArrayList<>();
            builder.add("SELECT a FROM EventRecordsVO a");
            Optional<Label> opt = getLabels().stream().filter(l -> l.getKey().equals(ZWatchConstants.LABEL_NAME) && l.getOp() == Label.Operator.Equal).findFirst();

            // get event family by label,
            EventFamily ef = null;
            if (opt.isPresent()) {
                Label name = opt.get();
                if (isMoreThanOneEventFamilyWithName(name.getValue())) {
                    opt = getLabels().stream().filter(l -> l.getKey().equals(ZWatchConstants.LABEL_NAMESPACE) && l.getOp() == Label.Operator.Equal).findFirst();
                    if (!opt.isPresent()) {
                        throw new OperationFailureException(argerr("there are multiple EventFamily with the name[%s], you must specify" +
                                " the label[%s]", name.getValue(), ZWatchConstants.LABEL_NAMESPACE));
                    }

                    String namespace = opt.get().getValue();
                    ef = getEventFamilyByNameThenByNamespace(name.getValue(), namespace);
                } else {
                    ef = getEventFamilyByNameThenByNamespace(name.getValue(), null);
                }
            }

            boolean needWhere = !getLabels().isEmpty() || getStartTime() != null || getEndTime() != null || getTime() != null || !getWhereConditions().isEmpty();
            if (needWhere) {
                builder.add("WHERE");
                List<String> conditions = new ArrayList<>();

                if (!getLabels().isEmpty()) {
                    EventFamily finalEf = ef;
                    getLabels().forEach(label -> conditions.add(labelToCondition(label, finalEf)));
                }

                if (getStartTime() != null) {
                    conditions.add(String.format("a.createTime >= %s", getStartTime()));
                }
                if (getEndTime() != null) {
                    conditions.add(String.format("a.createTime <= %s", getEndTime()));
                }
                if (getTime() != null) {
                    conditions.add(String.format("a.createTime = %s", getTime()));
                }
                if (!getWhereConditions().isEmpty()) {
                    List<String> mysqlWhereConditions = new ArrayList<String>();
                    getWhereConditions().stream().forEach(cond -> {
                        String newsql = convertToMysqlSQL(cond, EventRecordsVO.class);
                        mysqlWhereConditions.add(newsql);
                    });
                    conditions.addAll(mysqlWhereConditions);
                }

                builder.add(StringUtils.join(conditions, " and "));
            }

            builder.add("ORDER BY a.createTime DESC");

            return StringUtils.join(builder, " ");
        }

        public void markAsRead(String readBy) {
            String qstr = generateMarkAsReadSQL(readBy);
            if (logger.isTraceEnabled()) {
                logger.trace(qstr);
            }
            SQL.New(qstr).execute();
        }

        private String generateMarkAsReadSQL(String readBy) {

            boolean needWhere = !getLabels().isEmpty() || getStartTime() != null || getEndTime() != null;
            if (needWhere) {
                List<String> builder = new ArrayList<>();
//                builder.add("update EventsVO a set a.readStatus=1");
                builder.add("update EventRecordsVO a set a.readStatus=1, a.operatorAccountUuid='" + readBy + "'");
                builder.add("WHERE");
                List<String> conditions = new ArrayList<>();
                if (!getLabels().isEmpty()) {
                    getLabels().forEach(label -> conditions.add(labelToCondition(label, null)));
                }
                if (getStartTime() != null) {
                    conditions.add(String.format("a.createTime >= %s", getStartTime()));
                }
                if (getEndTime() != null) {
                    conditions.add(String.format("a.createTime <= %s", getEndTime()));
                }

                builder.add(StringUtils.join(conditions, " and "));
                return StringUtils.join(builder, " ");
            }

            return String.format(
                    "update EventRecordsVO a set a.readStatus=1 and operatorAccountUuid='%s' where a.readStatus=0",
                    readBy);
        }

        private String labelToCondition(Label label, EventFamily ef) {
            List<String> nameSpaceLabelList = null;
            if (ef != null) {
                nameSpaceLabelList = ef.getLabelNames();
            }
            List<String> mysqlField = FieldUtils.getAllFields(EventRecordsVO.class).stream().map(Field::getName).collect(Collectors.toList());
            if (ef != null && nameSpaceLabelList.contains(label.getKey()) ) {
                return EfLabelToMysqlCondition(label);
                //
            } else if (mysqlField.contains(label.getKey())) {
                return labelToMysqlConditionV2(label);
            }
            throw new OperationFailureException(argerr("invalid query label[%s]. Allowed label names are %s", label.getKey(), nameSpaceLabelList));
        }

        private String convertToMysqlSQL(String cond, Class clz) {              //  todo input parameter invalid format not like (("subscriptionUuid" = '' and "emergencyLevel" != 'Normal' and "readStatus" != '')
//            Pattern p = Pattern.compile(("\"[^\"]*\""), Pattern.CASE_INSENSITIVE); // todo maybe like (('subscriptionUuid' = '' and 'emergencyLevel' != 'Normal' and 'readStatus' != '')
//            String mysqlSQL = cond;                                            // ((subscriptionUuid = '' and emergencyLevel != 'Normal' and 'readStatus' != '')
//            Matcher m = p.matcher(cond);
//            while(m.find()){
//                System.out.println("bjw"+m.group(0));
//                mysqlSQL = mysqlSQL.replace(m.group(0), "a."+m.group(0).substring(1,m.group(0).length()-1));
//            }

            cond = cond.replace("=~", " like ").replace("/","'");

            Set<String> EventField = new HashSet<>();
            FieldUtils.getAllFields(clz).forEach(f -> {
                EventField.add(f.getName());
            });
            List<String> mysqlField = FieldUtils.getAllFields(clz).stream().map(Field::getName).collect(Collectors.toList());
            String[] splitCond = cond.split(" ");
            List<String> builder = new ArrayList<>();
            for (int it=0;it< splitCond.length; it++ ){
                int finalIt = it;
                Set<String> uuids = mysqlField.stream().filter(filed -> splitCond[finalIt].contains(filed)).collect(Collectors.toSet());
                if (uuids.size() > 0) {
                    String field = Collections.max(uuids);
                    int startidx = splitCond[finalIt].indexOf(field.substring(0,1));
                    String replaceCondition = "a."+field;
                    if (startidx == 0) {
                        builder.add(splitCond[finalIt].replaceFirst(field, replaceCondition));
                        continue;
                    }
                    if (splitCond[finalIt].charAt(startidx - 1) == '(') {
                        builder.add(splitCond[finalIt].replaceFirst(field, replaceCondition));
                        continue;
                    }
                    if (splitCond[finalIt].charAt(startidx - 1) == '"') {
                        builder.add(splitCond[finalIt].replaceFirst("\""+field+"\"", replaceCondition));
                        continue;
                    }
                    if (splitCond[finalIt].charAt(startidx - 1) == '\'') {
                        builder.add(splitCond[finalIt].replaceFirst("\'"+field+"\'", replaceCondition));
                        continue;
                    }
                    if (splitCond[finalIt].charAt(startidx - 1) == '.') {
                        builder.add(splitCond[finalIt].replace("\'"+field+"\'", replaceCondition));
                    }
                } else {
                    builder.add(splitCond[finalIt]);
                }
            }

            return StringUtils.join(builder, " ");
        }

        private EventFamily getEventFamilyByNameThenByNamespace(String name, String namespace) {
            List<EventFamily> efs = eventFamilies.get(name);
            if (efs == null) {
                throw new OperationFailureException(argerr("cannot find EventFamily[name:%s, namespace:%s]", name, namespace));
            }

            if (efs.size() == 1) {
                return efs.get(0);
            }

            if (namespace == null) {
                throw new CloudRuntimeException(String.format("namespace cannot be null as there are multiple EventFamily with the name[%s]", name));
            }

            Optional<EventFamily> opt = efs.stream().filter(e->e.getNamespace().equals(namespace)).findFirst();
            if (!opt.isPresent()) {
                throw new OperationFailureException(argerr("cannot find EventFamily[name:%s, namespace:%s]", name, namespace));
            }

            return opt.get();
        }

        private boolean isMoreThanOneEventFamilyWithName(String name) {
            List<EventFamily> efs = eventFamilies.get(name);
            if (efs == null) {
                throw new OperationFailureException(argerr("cannot find EventFamily[name:%s]", name));
            }

            return efs.size() > 1;
        }

        private String EfLabelToMysqlCondition(Label label) {
            String condition = String.format("\"%s\":\"%s\"", label.getKey(), label.getValue());
            if (label.getOp() == Label.Operator.Equal || label.getOp() == Label.Operator.Regex) {
                return String.format("a.labels like \'%%%s%%\'", condition);
            } else if (label.getOp() == Label.Operator.NotEqual || label.getOp() == Label.Operator.RegexAgainst) {
                return String.format("a.labels not like \'%%%s%%\'", condition);
            } else {
                throw new CloudRuntimeException(String.format("unknown operator[%s] of the condition[%s]", label.getOp(), label.toString()));
            }
        }
    }

    private String labelToMysqlConditionV2(Label label) {
        boolean sensitive = ZWatchGlobalConfig.CASE_SENSITIVE_SEARCH.value(Boolean.class);

        if (label.getOp() == Label.Operator.Equal) {
            if (label.getKey().equals("readStatus")) {
                return String.format("a.%s=%d", label.getKey(), StringUtils.equals(label.getValue(), ZWatchConstants.DATA_READ_STATUS_READ) ? 1: 0);
            }
            return String.format("a.%s='%s'", label.getKey(), label.getValue());
        } else if (label.getOp() == Label.Operator.Regex) {
            // the spaces between =~ are needed
            // Do not put keyword 'binary' before column or mysql will not use index
            String tmpl = sensitive ? "a.%s like binary '%s'" : "a.%s like '%s'";
            if (label.getValue().contains("|")) {
                List<String> builder = new ArrayList<>();
                String[] splitLikeCond = label.getValue().split("\\|");
                Arrays.stream(splitLikeCond).forEach(i->builder.add(String.format(tmpl, label.getKey(), i)));
                String allLikeExpressions = StringUtils.join(builder, " or ");
                // make the regex condition as following format:
                // ( a.A like binary A1 or a.A like binary A2)
                return String.format("( %s )", allLikeExpressions);
            }
            return String.format(tmpl, label.getKey(), label.getValue());
        } else if (label.getOp() == Label.Operator.NotEqual) {
            return String.format("a.%s != '%s'", label.getKey(), label.getValue());
        } else if (label.getOp() == Label.Operator.RegexAgainst) {
            String tmpl = sensitive ? "a.%s not like binary '%s'" : "a.%s not like '%s'";
            return String.format(tmpl, label.getKey(), label.getValue());
        } else {
            throw new CloudRuntimeException(String.format("unknown operator[%s] of the condition[%s]", label.getOp(), label.toString()));
        }
    }

    class MysqlAlarmQueryObject extends AlarmQueryObject {
        public MysqlAlarmQueryObject(AlarmQueryObject other) {
            super(other);
        }

        String toQueryString() {
            List<String> builder = new ArrayList<>();
            builder.add("SELECT a FROM AlarmRecordsVO a");

            boolean needWhere = !getLabels().isEmpty() || getStartTime() != null || getEndTime() != null || getTime() != null || !getWhereConditions().isEmpty();
            if (needWhere) {
                builder.add("WHERE");
                List<String> conditions = new ArrayList<>();

                if (!getLabels().isEmpty()) {
                    getLabels().forEach(label -> conditions.add(labelToMysqlConditionV2(label)));
                }

                if (getStartTime() != null) {
                    conditions.add(String.format("a.createTime  >= %s", getStartTime()));
                }
                if (getEndTime() != null) {
                    conditions.add(String.format("a.createTime  <= %s", getEndTime()));
                }

                if (getTime() != null) {
                    conditions.add(String.format("a.createTime  = %s", getTime()));
                }

                if (!getWhereConditions().isEmpty()) {
                    conditions.addAll(getWhereConditions());
                }

                builder.add(StringUtils.join(conditions, " and "));
            }

            builder.add("ORDER BY a.createTime DESC");
            return StringUtils.join(builder, " ");
        }

        List<AlarmData> query() {
            String qstr = toQueryString();
            List<AlarmRecordsVO> alarmdata;
            if (getLimit() != -1) {
                alarmdata = SQL.New(qstr, AlarmRecordsVO.class).limit(getLimit()).offset(getOffset()).list();
            } else {
                alarmdata = SQL.New(qstr, AlarmRecordsVO.class).offset(getOffset()).list();
            }
            if (logger.isTraceEnabled()) {
                logger.trace(qstr);
            }

            List<AlarmData> data = new ArrayList<>();
            alarmdata.forEach(r -> {
                data.add(AlarmRecordsInventory.toAlarmData(r));
            });

            return data;
        }

        String toQueryCountString() {
            List<String> builder = new ArrayList<>();
            builder.add("SELECT count(a) FROM AlarmRecordsVO a");
            boolean needWhere = !getLabels().isEmpty() || getStartTime() != null || getEndTime() != null || getTime() != null || !getWhereConditions().isEmpty();
            if (needWhere) {
                builder.add("WHERE");
                List<String> conditions = new ArrayList<>();
                if (!getLabels().isEmpty()) {
                    getLabels().forEach(label -> conditions.add(labelToMysqlConditionV2(label)));
                }

                if (getStartTime() != null) {
                    conditions.add(String.format("a.createTime >= %s", getStartTime()));
                }
                if (getEndTime() != null) {
                    conditions.add(String.format("a.createTime <= %s", getEndTime()));
                }

                builder.add(StringUtils.join(conditions, " and "));
            }

            return StringUtils.join(builder, " ");
        }

        Long count() {
            String qstr = toQueryCountString();
            if (logger.isTraceEnabled()) {
                logger.trace(qstr);
            }
            return SQL.New(qstr, Long.class).find();
        }

        public void markAsRead(String readBy) {
            String qstr = generateMarkAsReadSQL(readBy);
            if (logger.isTraceEnabled()) {
                logger.trace(qstr);
            }
            SQL.New(qstr).execute();
        }

        private String generateMarkAsReadSQL(String readBy) {

            boolean needWhere = !getLabels().isEmpty() || getStartTime() != null || getEndTime() != null;
            if (needWhere) {
                List<String> builder = new ArrayList<>();
//                builder.add("update EventsVO a set a.readStatus=1");
                builder.add("update AlarmRecordsVO a set a.readStatus=1, a.operatorAccountUuid='" + readBy + "'");
                builder.add("WHERE");
                List<String> conditions = new ArrayList<>();
                if (!getLabels().isEmpty()) {
                    getLabels().forEach(label -> conditions.add(labelToMysqlConditionV2(label)));
                }
                if (getStartTime() != null) {
                    conditions.add(String.format("a.createTime >= %s", getStartTime()));
                }
                if (getEndTime() != null) {
                    conditions.add(String.format("a.createTime <= %s", getEndTime()));
                }

                builder.add(StringUtils.join(conditions, " and "));
                return StringUtils.join(builder, " ");
            }

            return String.format(
                    "update AlarmRecordsVO a set a.readStatus=1 and operatorAccountUuid='%s' where a.readStatus=0",
                    readBy);
        }
    }

    class MysqlAuditQueryObject extends AuditQueryObject {
        public MysqlAuditQueryObject(AuditQueryObject other) {
            super(other);
        }

        private String labelToCondition(Label label) {
//            if (label.isCompatible()) {  //迁移之后不用检查 v1版本数据
//                return labelToConditionV1andV2(label);
//            } else {
                return labelToMysqlConditionV2(label);
            //}
        }

        /** compatible with old versions before(not include) ZStack V3.8.0
         *  default it is false and do replace,
         *  nor it is true and no replace.
         *  This code could works even ui do nothing
         */
        private void replaceError(Label label) {
            if (label.getKey().equals(AuditDataV2.FIELD_API_ERROR)) {
                label.setKey(AuditDataV2.TAG_SUCCESS);
                if (label.getValue().trim().equals("")) {
                    label.setValue("1");
                } else {
                    label.setValue("0");
                }
            }
        }

        String toQueryString() {
            List<String> builder = new ArrayList<>();
            builder.add("SELECT a FROM AuditsVO a");

            boolean needWhere = !getLabels().isEmpty() || getStartTime() != null || getEndTime() != null;
            if (needWhere) {
                builder.add("WHERE");
                List<String> conditions = new ArrayList<>();

                if (!getLabels().isEmpty()) {
                    getLabels().forEach(label -> conditions.add(labelToCondition(label)));
                }

                if (getStartTime() != null) {
                    conditions.add(String.format("a.createTime >= %s", getStartTime()));
                }
                if (getEndTime() != null) {
                    conditions.add(String.format("a.createTime <= %s", getEndTime())); //前后端都统一使用13位时间戳进行读取和查询
                }

                builder.add(StringUtils.join(conditions, " and "));
            }

            builder.add("ORDER BY a.createTime DESC");
//            builder.add(String.format("LIMIT %s", getLimit()));

            return StringUtils.join(builder, " ");
        }

        List<AuditData> query() {
            String qstr = toQueryString();
            String finnalQuerySql = convertTomysql(qstr);
            List<AuditsVO> auditdata = null;

            if (ZWatchGlobalConfig.CASE_SENSITIVE_SEARCH.value(Boolean.class)) {
                finnalQuerySql = finnalQuerySql.replace("SELECT a FROM", "SELECT a.* FROM");
                Query q = dbf.getEntityManager().createNativeQuery(finnalQuerySql, AuditsVO.class);
                q.setMaxResults(getLimit());
                auditdata = q.getResultList();
            } else {
                auditdata = SQL.New(finnalQuerySql, AuditsVO.class).limit(getLimit()).list();
            }

            List<AuditData> data = new ArrayList<>();
            auditdata.forEach(r -> {
                data.add(AuditsInventory.toAuditData(r));
            });
            return data;
        }

        private boolean isJSON2(String str) {
            JsonElement jsonElement;
            try {
                jsonElement = new JsonParser().parse(str);
            } catch (Exception e) {
                return false;
            }
            if (jsonElement == null) {
                return false;
            }
            return jsonElement.isJsonObject();
        }

        private String convertTomysql(String qstr) {
            return qstr;
        }
    }
}
