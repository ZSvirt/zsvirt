package org.zstack.zwatch;

import javax.persistence.Tuple;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.AbstractService;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.host.*;
import org.zstack.header.identity.AccountConstant;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.managementnode.ManagementNodeVO;
import org.zstack.header.managementnode.ManagementNodeVO_;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.vm.*;
import org.zstack.identity.Account;
import org.zstack.identity.AccountManager;
import org.zstack.identity.ResourceHelper;
import org.zstack.sns.*;
import org.zstack.utils.Bash;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.path.PathUtil;
import org.zstack.zwatch.alarm.*;
import org.zstack.zwatch.alarm.sns.SNSActionFactory;
import org.zstack.zwatch.alarm.sns.SNSTextTemplateType;
import org.zstack.zwatch.api.*;
import org.zstack.zwatch.datatype.*;
import org.zstack.zwatch.datatype.metric.Metric;
import org.zstack.zwatch.driver.DatabaseDriver;
import org.zstack.zwatch.driver.EventDatabaseDriver;
import org.zstack.zwatch.migratedb.MigrateDBEventDatabaseDriver;
import org.zstack.zwatch.namespace.CustomNamespace;
import org.zstack.zwatch.namespace.VmNamespace;

import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.operr;
import static org.zstack.zwatch.ZWatchConstants.*;
import static org.zstack.zwatch.alarm.sns.AbstractTextTemplate.defaultSupportedAlarmParams;
import static org.zstack.zwatch.alarm.sns.AbstractTextTemplate.defaultSupportedEventParams;

public class ZWatchManagerImpl extends AbstractService implements ZWatchManager, HostAfterConnectedExtensionPoint,
        VmInstanceAttachNicExtensionPoint {
    protected static final CLogger logger = Utils.getLogger(ZWatchManagerImpl.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private AccountManager acntMgr;
    @Autowired
    protected ThreadFacade thdf;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private PluginRegistry pluginRgty;

    private EventDatabaseDriver eventDBDriver;

    private MigrateDBEventDatabaseDriver mysqlDriver;

    private Map<String, CountCache> eventAndAlarmDataCountCacheMap = new ConcurrentHashMap<>();

    public ZWatchManagerImpl(EventDatabaseDriver eventDBDriver) {
        this.eventDBDriver = eventDBDriver;
//        initMySQL();
    }

    private void initMySQL() {
//        this.mysqlDriver =  Platform.getComponentLoader().getComponentByBeanName("MigrateDBEventDatabaseDriver");
        this.mysqlDriver = new MigrateDBEventDatabaseDriver();
    };

    @Override
    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage) msg);
        } else {
            handleLocalMessage(msg);
        }
    }

    private void handleLocalMessage(Message msg) {
        if (msg instanceof GetMetricDataMsg) {
            handle((GetMetricDataMsg) msg);
        } else if (msg instanceof GetManagementNodeDirCapacityMsg) {
            handle((GetManagementNodeDirCapacityMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APIGetMetricDataMsg) {
            handle((APIGetMetricDataMsg) msg);
        } else if (msg instanceof APIGetPrometheusMetricLabelValueMsg) {
            handle((APIGetPrometheusMetricLabelValueMsg) msg);
        } else if (msg instanceof APIDeleteMetricDataMsg) {
            handle((APIDeleteMetricDataMsg) msg);
        } else if (msg instanceof APIGetEventDataMsg) {
            handle((APIGetEventDataMsg) msg);
        } else if (msg instanceof APIGetZWatchAlertHistogramMsg) {
            handle((APIGetZWatchAlertHistogramMsg) msg);
        } else if (msg instanceof APIGetAuditDataMsg) {
            handle((APIGetAuditDataMsg) msg);
        } else if (msg instanceof APIGetAlarmDataMsg) {
            handle((APIGetAlarmDataMsg) msg);
        } else if (msg instanceof APIGetAllMetricMetadataMsg) {
            handle((APIGetAllMetricMetadataMsg) msg);
        } else if (msg instanceof APIGetAllEventMetadataMsg) {
            handle((APIGetAllEventMetadataMsg) msg);
        } else if (msg instanceof APIGetMetricLabelValueMsg) {
            handle((APIGetMetricLabelValueMsg) msg);
        } else if (msg instanceof APIPutMetricDataMsg) {
            handle((APIPutMetricDataMsg) msg);
        } else if (msg instanceof APIUpdateEventDataMsg) {
            handle((APIUpdateEventDataMsg) msg);
        } else if (msg instanceof APIUpdateAlarmDataMsg) {
            handle((APIUpdateAlarmDataMsg) msg);
        } else if (msg instanceof APIGetManagementNodeDirCapacityMsg) {
            handle((APIGetManagementNodeDirCapacityMsg) msg);
        } else if (msg instanceof APIAckAlarmDataMsg) {
            handle((APIAckAlarmDataMsg) msg);
        } else if (msg instanceof APIAckEventDataMsg) {
            handle((APIAckEventDataMsg) msg);
        } else if (msg instanceof APIUpdateAlertDataAckMsg) {
            handle((APIUpdateAlertDataAckMsg) msg);
        } else if (msg instanceof APIGetTextTemplateArgMsg) {
            handle((APIGetTextTemplateArgMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private LabelValueQueryObject getPrometheusQueryLabelFromAPIMsg(APIGetPrometheusMetricLabelValueMsg msg) {
        List<Label> allLabels = msg.getFilterLabels().stream().map(Label::new).collect(Collectors.toList());
        List<Label> filters = allLabels.stream().filter(l -> l.getOp() == Label.Operator.Filter).collect(Collectors.toList());
        List<Label> metricLabels = allLabels.stream().filter(l ->
                l.getOp() == Label.Operator.Equal ||
                        l.getOp() == Label.Operator.Regex ||
                        l.getOp() == Label.Operator.RegexAgainst
        ).collect(Collectors.toList());

        LabelValueQueryObject qo = new LabelValueQueryObject();
        qo.setMetricName(msg.getMetricName());
        qo.setLabelNames(msg.getLabelNames());
        qo.setAccountUuid(msg.getSession().getAccountUuid());
        qo.setNamespaceName(msg.getNamespace());
        qo.setFilteredLabels(metricLabels);
        qo.setFilters(filters);
        qo.setStartTime(msg.getStartTime());
        qo.setEndTime(msg.getEndTime());

        return qo;
    }

    private void handle(APIGetPrometheusMetricLabelValueMsg msg) {
        Namespace ns = Namespace.getMetricNameSpace(msg.getNamespace(), msg.getMetricName());

        LabelValueQueryObject qo = getPrometheusQueryLabelFromAPIMsg(msg);
        Map<String, List<String>> ret = ns.queryPrometheusLabelValues(qo);

        APIGetPrometheusMetricLabelValueReply reply = new APIGetPrometheusMetricLabelValueReply();
        reply.setLabelValues(ret);
        bus.reply(msg, reply);
    }

    private void handle(APIGetTextTemplateArgMsg msg) {
        APIGetTextTemplateArgReply reply = new APIGetTextTemplateArgReply();
        reply.setDefaultSupportedParams(ImmutableMap.of(
                SNSTextTemplateType.ALARM.toString(), defaultSupportedAlarmParams,
                SNSTextTemplateType.EVENT.toString(), defaultSupportedEventParams));
        bus.reply(msg, reply);
    }

    private void handle(GetManagementNodeDirCapacityMsg msg) {
        GetManagementNodeDirCapacityReply reply = new GetManagementNodeDirCapacityReply();

        List<CapacityData> dataList = new ArrayList<>();
        new Bash() {
            @Override
            protected void scripts() {
                getDirUsedSize("mnLog", CoreGlobalProperty.LOG_DIR);
                getDirUsedSize("mysql", "/var/lib/mysql/");
                getDirUsedSize("prometheus", PathUtil.join(CoreGlobalProperty.DATA_DIR, "prometheus"), "120");
                getDirUsedSize("mysqlBackup", "/var/lib/zstack/mysql-backup/");
                getDirUsedSize("upgradeBackup", PathUtil.join(CoreGlobalProperty.USER_HOME, "upgrade"));

                sudoRun("timeout 10 df -B 1 %s | tail -1", CoreGlobalProperty.USER_HOME);
                String size = stdout().replaceAll("\r|\n|\t", " ");
                CapacityData total = new CapacityData();
                total.setName("total");

                CapacityData used = new CapacityData();
                used.setName("used");

                dataList.add(total);
                dataList.add(used);

                if (lastReturnCode != 0 || Strings.isEmpty(size)) {
                    total.setSize("-1");
                    used.setSize("-1");
                    return;
                }

                String[] values = size.split(" +");
                total.setSize(values[1].trim());
                used.setSize(values[2].trim());
            }

            private void getDirUsedSize(String name, String path) {
                getDirUsedSize(name, path, null);
            }

            private void getDirUsedSize(String name, String path, String timeout) {
                sudoRun("timeout %s du -s -B1 %s", timeout == null ? "10" : timeout, path);

                CapacityData data = new CapacityData();
                String ret = stdout().replaceAll("\r|\n|\t", " ");
                if (lastReturnCode != 0 || Strings.isEmpty(ret)) {
                    data.setName(name);
                    data.setSize("-1");
                    dataList.add(data);
                    return;
                }

                String[] values = ret.split(" +");
                String used = values[0].trim();

                data.setName(name);
                data.setSize(used);
                dataList.add(data);
            }
        }.execute();

        reply.setDataList(dataList);
        bus.reply(msg, reply);
    }

    private void handle(APIGetManagementNodeDirCapacityMsg msg) {
        List<String> managementNodeUuids = new ArrayList<>();

        if (msg.getManagementNodeUuids() == null || msg.getManagementNodeUuids().isEmpty()) {
            managementNodeUuids = Q.New(ManagementNodeVO.class).select(ManagementNodeVO_.uuid).listValues();
        } else {
            managementNodeUuids.addAll(msg.getManagementNodeUuids());
        }

        List<GetManagementNodeDirCapacityMsg> msgs = new ArrayList<>();
        for (String nodeUuid : managementNodeUuids) {
            GetManagementNodeDirCapacityMsg gmsg = new GetManagementNodeDirCapacityMsg();
            gmsg.setManagementNodeUuid(nodeUuid);
            bus.makeServiceIdByManagementNodeId(gmsg, SERVICE_ID, gmsg.getManagementNodeUuid());
            msgs.add(gmsg);
        }

        Map<String, List<CapacityData>> ret = new HashMap<>();
        new While<>(msgs).each((gmsg, completion) -> {
            bus.send(gmsg, new CloudBusCallBack(completion) {
                @Override
                public void run(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        ret.put(gmsg.getManagementNodeUuid(), new ArrayList<>());
                        completion.done();
                        return;
                    }

                    ret.put(gmsg.getManagementNodeUuid(), ((GetManagementNodeDirCapacityReply) reply).getDataList());
                    completion.done();
                }
            });
        }).run(new WhileDoneCompletion(msg) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                APIGetManagementNodeDirCapacityReply reply = new APIGetManagementNodeDirCapacityReply();
                reply.setResult(ret);
                bus.reply(msg, reply);
            }
        });
    }
    // 3.2 version history message has no readStatus information (hardcode for ZSTAC-17862)
    private List<AlarmData> getNoReadStatusAlarmData(APIGetAlarmDataMsg msg) {
        if (msg.getLabelList() == null) {
            return Collections.emptyList();
        }

        boolean flag = msg.getLabelList().stream().anyMatch( label ->
                ZWatchConstants.DATA_READ_STATUS.equals(label.getKey()) && ZWatchConstants.DATA_READ_STATUS_READ.equalsIgnoreCase(label.getValue())
        );
        if (!flag) {
            return Collections.emptyList();
        }

        List<Label> labels = msg.getLabelList().stream()
                .filter(label -> !ZWatchConstants.DATA_READ_STATUS.equals(label.getKey()))
                .collect(Collectors.toList());
        labels.add(new Label(ZWatchConstants.DATA_READ_STATUS, ""));

        AlarmQueryObject qo = AlarmQueryObject.New()
                .startTime(msg.getStartTime())
                .endTime(msg.getEndTime())
                .labels(labels)
                .limit(msg.getLimit())
                .build();

        return eventDBDriver.query(qo);
    }

    private void handle(APIGetAlarmDataMsg msg) {
        APIGetAlarmDataReply reply = new APIGetAlarmDataReply();

        List<String> whereConditions = new ArrayList<>();

        if (msg.getEndpointUuid() != null) {

            SNSApplicationEndpointVO endpointVO = dbf.findByUuid(msg.getEndpointUuid(), SNSApplicationEndpointVO.class);

            List<SNSSubscriberVO> snsSubscriberVOS = Q.New(SNSSubscriberVO.class)
                    .eq(SNSSubscriberVO_.endpointUuid, msg.getEndpointUuid())
                    .list();

            List<String> topicUuids = snsSubscriberVOS.stream().map(SNSSubscriberVO::getTopicUuid).collect(Collectors.toList());


            List<String> alarmUuids = Q.New(AlarmActionVO.class)
                    .select(AlarmActionVO_.alarmUuid)
                    .eq(AlarmActionVO_.actionUuid, topicUuids)
                    .eq(AlarmActionVO_.actionType, SNSActionFactory.type.toString())
                    .listValues();

            if (alarmUuids.isEmpty()) {
                bus.reply(msg, reply);
                return;
            }

            List<AlarmVO> alarmVOS = Q.New(AlarmVO.class)
                    .in(AlarmVO_.uuid, alarmUuids)
                    .list();

            List<String> conditions = new ArrayList<>();

            for (AlarmVO alarmVO : alarmVOS) {
                Long startTime = null, endTime = null;

                AlarmActionVO alarmActionVO = alarmVO.getActions().stream().filter(actionVO -> topicUuids.contains(actionVO.getActionUuid())).findFirst().orElse(null);

                if (alarmActionVO == null) {
                    continue;
                }

                List<Long> endTimes = new ArrayList<>();
                if (msg.getEndTime() != null) {
                    endTimes.add(msg.getEndTime());
                }

                List<Long> startTimes = new ArrayList<>();
                if (msg.getStartTime() != null) {
                    startTimes.add(msg.getStartTime());
                }

                if (endpointVO.getState() == SNSApplicationEndpointState.Disabled) {
                    endTimes.add(endpointVO.getLastOpDate().getTime());
                }

                for (SNSSubscriberVO vo : snsSubscriberVOS) {
                    startTimes.add(vo.getCreateDate().getTime());
                }

                if (alarmVO.getState() == AlarmState.Disabled) {
                    endTimes.add(alarmActionVO.getLastOpDate().getTime());
                }
                startTimes.add(alarmActionVO.getCreateDate().getTime());

                if (endTimes.size() > 0) {
                    endTime = Collections.min(endTimes);
                }

                if (startTimes.size() > 0) {
                    startTime = Collections.max(startTimes);
                }


                if (endTime != null && startTime != null) {
                    conditions.add(String.format(ZWatchConstants.alarmMysqlAllString, alarmVO.getUuid(), startTime, endTime));
                    continue;
                }

                if (endTime != null) {
                    conditions.add(String.format(ZWatchConstants.alarmMysqlEndTimeString, alarmVO.getUuid(), endTime));
                    continue;
                }

                if (startTime != null) {
                    conditions.add(String.format(ZWatchConstants.alarmMysqlStartTimeString, alarmVO.getUuid(), startTime));
                    continue;
                }
                conditions.add(String.format(ZWatchConstants.alarmMysqlNotTimeString, alarmVO.getUuid()));
            }

            StringBuffer buffer = new StringBuffer();
            buffer.append("(");
            buffer.append(StringUtils.join(conditions, " or "));
            buffer.append(")");

            whereConditions.add(buffer.toString());
        }

        AlarmQueryObject qo = AlarmQueryObject.New()
                .startTime(msg.getStartTime())
                .endTime(msg.getEndTime())
                .labels(msg.getLabelList())
                .whereConditions(whereConditions)
                .limit(msg.getLimit())
                .offset(msg.getStart())
                .build();

        if (!msg.isCount()) {
            List<AlarmData> data = eventDBDriver.query(qo);
//            data.addAll(getNoReadStatusAlarmData(msg));
//            reply.setHistories(data);

           // List<AlarmData> data = mysqlDriver.query(qo);
            reply.setHistories(data);
            bus.reply(msg, reply);
            return;
        }
        Long count = eventDBDriver.getQueryCount(qo);
//        Long count = mysqlDriver.getQueryCount(qo);
        reply.setTotal(count);
        bus.reply(msg, reply);
    }

    private void handle(APIPutMetricDataMsg msg) {
        CustomNamespace ns = Namespace.getCustomNamespace();
        ns.write(msg.getNamespace(), msg.getData(), msg.getSession().getAccountUuid());
        bus.publish(new APIPutMetricDataEvent(msg.getId()));
    }

    private LabelValueQueryObject getQueryLabelFromAPIMsg(APIGetMetricLabelValueMsg msg) {
        List<Label> allLabels = msg.getFilterLabels().stream().map(Label::new).collect(Collectors.toList());
        List<Label> filters = allLabels.stream().filter(l -> l.getOp() == Label.Operator.Filter).collect(Collectors.toList());
        List<Label> metricLabels = allLabels.stream().filter(l ->
                            l.getOp() == Label.Operator.Equal ||
                            l.getOp() == Label.Operator.Regex ||
                            l.getOp() == Label.Operator.RegexAgainst
        ).collect(Collectors.toList());

        LabelValueQueryObject qo = new LabelValueQueryObject();
        qo.setMetricName(msg.getMetricName());
        qo.setLabelNames(msg.getLabelNames());
        qo.setAccountUuid(msg.getSession().getAccountUuid());
        qo.setNamespaceName(msg.getNamespace());
        qo.setFilteredLabels(metricLabels);
        qo.setFilters(filters);
        qo.setStartTime(msg.getStartTime());
        qo.setEndTime(msg.getEndTime());

        return qo;
    }
    
    private void handle(APIGetMetricLabelValueMsg msg) {
        Namespace ns = Namespace.getMetricNameSpace(msg.getNamespace(), msg.getMetricName());

        LabelValueQueryObject qo = getQueryLabelFromAPIMsg(msg);
        List<Map> ret = ns.queryLabelValues(qo);
        ret = ns.filterLabelValues(ret, qo);

        APIGetMetricLabelValueReply reply = new APIGetMetricLabelValueReply();
        reply.setLabels(ret);
        bus.reply(msg, reply);
    }

    private void handle(APIGetAllEventMetadataMsg msg) {
        List<APIGetAllEventMetadataReply.EventStruct> eventStructs = new ArrayList<>();
        List<Namespace> namespaces = filter(msg.getNamespace());
        List<String> names = msg.getName() == null ? new ArrayList<>() : Arrays.asList(msg.getName().split(","));
        namespaces.forEach(ns -> {
            if (ns.getEvents() != null) {
                ns.getEvents().forEach(e -> {
                    if (names.isEmpty() || names.contains(e.getName())) {
                        APIGetAllEventMetadataReply.EventStruct struct = new APIGetAllEventMetadataReply.EventStruct();
                        struct.name = e.getName();
                        struct.namespace = ns.getName();
                        struct.description = e.getDescription();
                        struct.labelNames = e.getLabelNames();
                        eventStructs.add(struct);
                    }
                });
            }
        });

        APIGetAllEventMetadataReply reply = new APIGetAllEventMetadataReply();
        reply.setEvents(eventStructs);
        bus.reply(msg, reply);
    }

    private List<Namespace> filter(String namespaceStr) {
        List<Namespace> res = new ArrayList<>();
        List<String> namespaces = namespaceStr == null ? new ArrayList<>() : Arrays.asList(namespaceStr.split(","));
        for(List<Namespace> nss: Namespace.namespaces.values()) {
            for (Namespace ns: nss) {
                if (namespaces.isEmpty() || namespaces.contains(ns.getName())) {
                    res.add(ns);
                }
            }
        }
        return res;
    }

    private void handle(APIGetAllMetricMetadataMsg msg) {
        List<APIGetAllMetricMetadataReply.MetricStruct> metricStructs = new ArrayList<>();
        List<Namespace> namespaces = filter(msg.getNamespace());
        List<String> names = msg.getName() == null ? new ArrayList<>() : Arrays.asList(msg.getName().split(","));
        if (!acntMgr.isAdmin(msg.getSession())) {
            namespaces.forEach(ns -> {
                if (ns.getMetrics() != null) {
                    ns.getMetrics().stream().filter(m -> !m.isAdminOnly()).forEach(m -> {
                        if (names.isEmpty() || names.contains(m.getName())) {
                            metricStructs.add(buildMetricStructWithInfo(ns, m));
                        }
                    });
                }
            });
        } else {
            namespaces.forEach(ns -> {
                if (ns.getMetrics() != null) {
                    ns.getMetrics().forEach(m -> {
                        if (names.isEmpty() || names.contains(m.getName())) {
                            metricStructs.add(buildMetricStructWithInfo(ns, m));
                        }
                    });
                }
            });
        }

        metricStructs.addAll(Namespace.getCustomNamespace().getAllMetricMetadata(msg.getSession().getAccountUuid()));

        APIGetAllMetricMetadataReply reply = new APIGetAllMetricMetadataReply();
        reply.setMetrics(metricStructs);
        bus.reply(msg, reply);
    }

    private APIGetAllMetricMetadataReply.MetricStruct buildMetricStructWithInfo(Namespace ns, Metric m) {
        APIGetAllMetricMetadataReply.MetricStruct struct = new APIGetAllMetricMetadataReply.MetricStruct();
        struct.name = m.getName();
        struct.namespace = ns.getName();
        struct.description = m.getDescription();
        DatabaseDriver driver = ns.getDatabaseDriver();
        struct.driver = driver == null ? "" : driver.getClass().getSimpleName();
        struct.labelNames = ns.getEffectiveMetricLabelNames(m);
        return struct;
    }

    private void handle(APIGetAuditDataMsg msg) {
        AuditQueryObject qo = AuditQueryObject.New()
                .startTime(msg.getStartTime())
                .endTime(msg.getEndTime())
                .labels(msg.getLabelList())
                .limit(msg.getLimit())
                .build();

        List<AuditData> data = eventDBDriver.query(qo);
//        List<AuditData> data = mysqlDriver.query(qo);

        data = filterAudiDataByAccount(data, msg);

        APIGetAuditDataReply reply = new APIGetAuditDataReply();
        reply.setAudits(data);
        bus.reply(msg, reply);
    }

    private List<AuditData> filterAudiDataByAccount(List<AuditData> data, APIGetAuditDataMsg msg) {
        if (Account.isAdminPermission(msg.getSession())) {
            return data;
        }

        return auditFilter(data, msg.getSession());
    }

    private List<AuditData> auditFilter(List<AuditData> data, SessionInventory session) {
        return data.stream().filter(d -> session.getAccountUuid().equals(d.getOperatorAccountUuid())).collect(Collectors.toList());
    }

    // 3.2 version history message has no readStatus information (hardcode for ZSTAC-17862)
    private List<EventData> getNoReadStatusEventData(APIGetEventDataMsg msg) {
        if (msg.getLabelList() == null) {
            return Collections.emptyList();
        }

        boolean flag = msg.getLabelList().stream().anyMatch( label ->
            ZWatchConstants.DATA_READ_STATUS.equals(label.getKey()) && ZWatchConstants.DATA_READ_STATUS_READ.equalsIgnoreCase(label.getValue())
        );
        if (!flag) {
            return Collections.emptyList();
        }

        List<Label> labels = msg.getLabelList().stream()
                .filter(label -> !ZWatchConstants.DATA_READ_STATUS.equals(label.getKey()))
                .collect(Collectors.toList());
        labels.add(new Label(ZWatchConstants.DATA_READ_STATUS, ""));

        EventQueryObject qo = EventQueryObject.New()
                .startTime(msg.getStartTime())
                .endTime(msg.getEndTime())
                .labels(labels)
                .limit(msg.getLimit())
                .build();

        List<EventData> data = eventDBDriver.query(qo);
        data = filterEventDataByAccount(data, msg);

        return data;
    }

    private void handle(APIGetEventDataMsg msg) {
        APIGetEventDataReply reply = new APIGetEventDataReply();

        List<String> whereConditions = new ArrayList<>();

        if (msg.getEndpointUuid() != null) {

            SNSApplicationEndpointVO endpointVO = dbf.findByUuid(msg.getEndpointUuid(), SNSApplicationEndpointVO.class);

            List<SNSSubscriberVO> snsSubscriberVOS = Q.New(SNSSubscriberVO.class)
                    .eq(SNSSubscriberVO_.endpointUuid, msg.getEndpointUuid())
                    .list();

            if (snsSubscriberVOS.isEmpty()) {
                bus.reply(msg, reply);
                return;
            }

            List<String> topicUuids = snsSubscriberVOS.stream().map(SNSSubscriberVO::getTopicUuid).collect(Collectors.toList());

            List<String> subscriptionUuids = Q.New(EventSubscriptionActionVO.class)
                    .select(EventSubscriptionActionVO_.subscriptionUuid)
                    .eq(EventSubscriptionActionVO_.actionUuid, topicUuids)
                    .eq(EventSubscriptionActionVO_.actionType, SNSActionFactory.type.toString())
                    .listValues();

            if (subscriptionUuids.isEmpty()) {
                bus.reply(msg, reply);
                return;
            }

            List<EventSubscriptionVO> subscriptionVOS = Q.New(EventSubscriptionVO.class)
                    .in(EventSubscriptionVO_.uuid, subscriptionUuids)
                    .list();


            List<String> conditions = new ArrayList<>();

            for (EventSubscriptionVO subscriptionVO : subscriptionVOS) {
                Long startTime = null, endTime = null;

                EventSubscriptionActionVO subscriptionActionVO = subscriptionVO.getActions().stream().filter(actionVO -> topicUuids.contains(actionVO.getActionUuid())).findFirst().orElse(null);

                if (subscriptionActionVO == null) {
                    continue;
                }

                List<Long> endTimes = new ArrayList<>();
                if (msg.getEndTime() != null) {
                    endTimes.add(msg.getEndTime());
                }

                List<Long> startTimes = new ArrayList<>();
                if (msg.getStartTime() != null) {
                    startTimes.add(msg.getStartTime());
                }

                if (endpointVO.getState() == SNSApplicationEndpointState.Disabled) {
                    endTimes.add(endpointVO.getLastOpDate().getTime());
                }

                for (SNSSubscriberVO vo : snsSubscriberVOS) {
                    startTimes.add(vo.getCreateDate().getTime());
                }


                if (subscriptionVO.getState() == EventSubscriptionState.Disabled) {
                    endTimes.add(subscriptionVO.getLastOpDate().getTime());
                }
                startTimes.add(subscriptionVO.getCreateDate().getTime());

                if (!endTimes.isEmpty()) {
                    endTime = Collections.min(endTimes);
                }

                if (!startTimes.isEmpty()) {
                    startTime = Collections.max(startTimes);
                }

                if (startTime != null && endTime != null) {
                    conditions.add(String.format(ZWatchConstants.subscriptionMysqlAllString, subscriptionVO.getUuid(), startTime, endTime));
                    continue;
                }

                if (startTime != null) {
                    conditions.add(String.format(ZWatchConstants.subscriptionMysqlStartString, subscriptionVO.getUuid(), startTime));
                    continue;
                }

                if (endTime != null) {
                    conditions.add(String.format(ZWatchConstants.subscriptionMysqlEndTimeString, subscriptionVO.getUuid(), endTime));
                    continue;
                }

                conditions.add(String.format(ZWatchConstants.subscriptionMysqlNotTimeString, subscriptionVO.getUuid()));
            }

            StringBuffer buffer = new StringBuffer();
            buffer.append("(");
            buffer.append(StringUtils.join(conditions, " or "));
            buffer.append(")");

            whereConditions.add(buffer.toString());
        } else {
            if (msg.getConditionExpression() != null) {
                whereConditions.add(msg.getConditionExpression());
            }
        }

        EventQueryObject qo = EventQueryObject.New()
                .startTime(msg.getStartTime())
                .endTime(msg.getEndTime())
                .labels(msg.getLabelList())
                .whereConditions(whereConditions)
                .limit(msg.getLimit())
                .offset(msg.getStart())
                .build();

        if (!msg.isCount()) {
            // hardcode for ZSTAC-18142 & ZSV-8407
            if (!Account.supportToQueryEventsFromAllAccounts(msg.getSession()) && msg.getLabelList() != null) {
                for (Label label : msg.getLabelList()) {
                    if (ZWatchConstants.DATA_READ_STATUS.equals(label.getKey())) {
                        qo.getLabels().add(new Label(ZWatchConstants.DATA_ACCOUNT_UUID, msg.getSession().getAccountUuid()));
                        break;
                    }
                }
            }
            List<EventData> data = eventDBDriver.query(qo);
//            List<EventData> data = mysqlDriver.query(qo);
            data = filterEventDataByAccount(data, msg);
//            data.addAll(getNoReadStatusEventData(msg));

            reply.setEvents(data);
            bus.reply(msg, reply);
            return;
        }

        if (!Account.supportToQueryEventsFromAllAccounts(msg.getSession())) {
            qo.getLabels().add(new Label(ZWatchConstants.DATA_ACCOUNT_UUID, msg.getSession().getAccountUuid()));
        }
        Long count = eventDBDriver.getQueryCount(qo);
//        Long count = mysqlDriver.getQueryCount(qo);
        reply.setTotal(count);
        bus.reply(msg, reply);
    }

    private CountCache getValidCountCache(String key) {
        CountCache countCache = eventAndAlarmDataCountCacheMap.get(key);
        if (countCache != null && countCache.getExpireDate() > System.currentTimeMillis()) {
            return countCache;
        }

        return null;
    }

    private void updateCountCache(String key, long size) {
        long expireSecs =  ZWatchGlobalConfig.COUNT_EVENTDATA_AND_ALARMDATA_CACHE_EXPIRE_TIME.value(Long.class);
        if (expireSecs == 0) {
            eventAndAlarmDataCountCacheMap.remove(key);
            return;
        }

        long minimum = ZWatchGlobalConfig.MINIMUM_COUNT_AMOUNT_ALLOWED_ADDED_TO_CACHE.value(Long.class);
        if (minimum > size) {
            eventAndAlarmDataCountCacheMap.remove(key);
            return;
        }

        CountCache countCache = new CountCache();
        countCache.setSize(size);
        countCache.setExpireDate(System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(expireSecs));
        eventAndAlarmDataCountCacheMap.put(key, countCache);
    }

    private void handle(APIUpdateEventDataMsg msg) {
        APIUpdateEventDataEvent event = new APIUpdateEventDataEvent(msg.getId());

        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getName() {
                return String.format("update-event-data-account-%s", msg.getSession().getAccountUuid());
            }

            @Override
            public String getSyncSignature() {
                return String.format("update-event-data-account-%s", msg.getSession().getAccountUuid());
            }

            @Override
            public void run(SyncTaskChain chain) {

                updateEventData(msg, new Completion(msg, chain) {
                    @Override
                    public void success() {
                        bus.publish(event);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        event.setError(errorCode);
                        bus.publish(event);
                        chain.next();
                    }
                });
            }
        });
    }

    private void updateEventData(APIUpdateEventDataMsg msg, Completion completion) {
        if (msg.getReadStatus() == null) {
            completion.success();
            return;
        }

        if (msg.getReadStatus() != null && DATA_READ_STATUS_READ.equalsIgnoreCase(msg.getReadStatus())) {
            msg.getLabelList().add(new Label(ZWatchConstants.DATA_READ_STATUS, DATA_READ_STATUS_UNREAD));
        }

        EventQueryObject qo = null;

        String updateMode = msg.getUpdateMode();
        if (UPDATE_DATA_MODE_ONLYONE.equals(updateMode)) {
            msg.getLabelList().add(new Label(ZWatchConstants.DATA_UUID, msg.getDataUuid()));
            qo = EventQueryObject.New()
                    .labels(msg.getLabelList())
                    .limit(1)
                    .build();
        } else if (UPDATE_DATA_MODE_INRANGE.equals(updateMode)) {
            qo = EventQueryObject.New()
                    .labels(msg.getLabelList())
                    .noLimit()
                    .startTime(msg.getDataStartTime())
                    .endTime(msg.getDataEndTime())
                    .build();
        } else if (UPDATE_DATA_MODE_ALL.equals(updateMode)) {
            qo = EventQueryObject.New()
                    .labels(msg.getLabelList())
                    .noLimit()
                    .build();
        } else {
            assert false;
        }
        eventDBDriver.markAsRead(qo, msg.getSession().getAccountUuid());
//        mysqlDriver.update(qo);
        completion.success();
    }

    private void handle(APIUpdateAlarmDataMsg msg) {
        APIUpdateAlarmDataEvent event = new APIUpdateAlarmDataEvent(msg.getId());

        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return String.format("update-alarm-data-account-%s", msg.getSession().getAccountUuid());
            }

            @Override
            public void run(SyncTaskChain chain) {
                updateAlarmData(msg, new Completion(msg, chain) {
                    @Override
                    public void success() {
                        bus.publish(event);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        event.setError(errorCode);
                        bus.publish(event);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return String.format("update-alarm-data-account-%s", msg.getSession().getAccountUuid());
            }
        });
    }

    private void updateAlarmData(APIUpdateAlarmDataMsg msg, Completion completion) {
        if (msg.getReadStatus() == null) {
            completion.success();
            return;
        }

        if (msg.getReadStatus() != null) {
            msg.getLabelList().add(new Label(ZWatchConstants.DATA_READ_STATUS, DATA_READ_STATUS_UNREAD));
        }

        AlarmQueryObject qo = null;

        String updateMode = msg.getUpdateMode();
        if (UPDATE_DATA_MODE_ONLYONE.equals(updateMode)) {
            msg.getLabelList().add(new Label(ZWatchConstants.DATA_UUID, msg.getDataUuid()));
            qo = AlarmQueryObject.New()
                    .labels(msg.getLabelList())
                    .limit(1)
                    .build();
        } else if (UPDATE_DATA_MODE_INRANGE.equals(updateMode)) {
            qo = AlarmQueryObject.New()
                    .labels(msg.getLabelList())
                    .noLimit()
                    .startTime(msg.getDataStartTime())
                    .endTime(msg.getDataEndTime())
                    .build();
        } else if (UPDATE_DATA_MODE_ALL.equals(updateMode)) {
            qo = AlarmQueryObject.New()
                    .labels(msg.getLabelList())
                    .noLimit()
                    .build();
        } else {
            assert false;
        }
        eventDBDriver.markAsRead(qo, msg.getSession().getAccountUuid());
//        mysqlDriver.update(qo);
        completion.success();
    }

    private void handle(APIAckAlarmDataMsg msg) {
        APIAckAlertDataEvent event = new APIAckAlertDataEvent(msg.getId());

        AlarmDataAckVO vo = dbf.findByUuid(msg.getAlertDataUuid(), AlarmDataAckVO.class);
        boolean exist = true;
        if (vo == null) {
            vo = new AlarmDataAckVO();
            exist = false;
        }

        String resourceUuid = !StringUtils.isEmpty(msg.getResourceUuid()) ? msg.getResourceUuid() : msg.getAlarmUuid();
        vo.setAlertDataUuid(msg.getAlertDataUuid());
        vo.setResumeAlert(false);
        vo.setAlertType(ZWatchConstants.ALARM_DATA_TYPE);
        vo.setResourceUuid(resourceUuid);
        vo.setAlarmUuid(msg.getAlarmUuid());
        vo.setAckDate(new Timestamp(System.currentTimeMillis()));
        vo.setAckPeriod(msg.getAckPeriodSec());
        vo.setOperatorAccountUuid(msg.getSession().getAccountUuid());

        if (exist) {
            dbf.updateAndRefresh(vo);
        } else {
            dbf.persistAndRefresh(vo);
        }

        event.setInventory(AlarmDataAckInventory.valueOf(vo));
        bus.publish(event);
    }

    private void handle(APIAckEventDataMsg msg) {
        APIAckAlertDataEvent event = new APIAckAlertDataEvent(msg.getId());

        EventDataAckVO vo = dbf.findByUuid(msg.getAlertDataUuid(), EventDataAckVO.class);
        boolean exist = true;
        if (vo == null) {
            vo = new EventDataAckVO();
            exist = false;
        }

        String resourceUuid = msg.getResourceUuid() != null ? msg.getResourceUuid() : msg.getEventSubscriptionUuid();
        vo.setEventSubscriptionUuid(msg.getEventSubscriptionUuid());
        vo.setAlertDataUuid(msg.getAlertDataUuid());
        vo.setAlertType(ZWatchConstants.EVENT_DATA_TYPE);
        vo.setResumeAlert(false);
        vo.setResourceUuid(resourceUuid);
        vo.setAckDate(new Timestamp(System.currentTimeMillis()));
        vo.setAckPeriod(msg.getAckPeriodSec());
        vo.setOperatorAccountUuid(msg.getSession().getAccountUuid());

        if (exist) {
            dbf.updateAndRefresh(vo);
        } else {
            dbf.persistAndRefresh(vo);
        }

        event.setInventory(EventDataAckInventory.valueOf(vo));
        bus.publish(event);
    }

    private void handle(APIUpdateAlertDataAckMsg msg) {
        APIUpdateAlertDataAckEvent event = new APIUpdateAlertDataAckEvent(msg.getId());
        boolean flag = false;

        AlertDataAckVO alertDataAckVO = Q.New(AlertDataAckVO.class)
                .eq(AlertDataAckVO_.alertDataUuid, msg.getAlertDataUuid())
                .find();

        if (msg.getResumeAlert() != null) {
            alertDataAckVO.setResumeAlert(msg.getResumeAlert());
            flag = true;
        }

        if (flag) {
            alertDataAckVO = dbf.updateAndRefresh(alertDataAckVO);
        }

        event.setInventory(AlertDataAckInventory.valueOf(alertDataAckVO));
        bus.publish(event);
    }

    private List<EventData> filterEventDataByAccount(List<EventData> data, APIMessage msg) {
        if (AccountConstant.isAdmin(msg.getSession())) {
            return data;
        }

        List<EventData> result = new ArrayList();

        List<EventData> belongToAccountDatas = data.stream().filter(
                v -> msg.getSession().getAccountUuid().equals(v.getAccountUuid()))
                .collect(Collectors.toList());
        result.addAll(belongToAccountDatas);

        // 3.2 version history message has no account information
        List<EventData> noAccountDatas = data.stream().filter(
                v -> v.getAccountUuid() == null)
                .collect(Collectors.toList());

        Collection<String> resourceUuids = ResourceHelper.filterAccessibleResources(
                noAccountDatas.stream().map(EventData::getResourceId).collect(Collectors.toSet()),
                msg.getSession().getAccountUuid()
        );

        belongToAccountDatas = noAccountDatas.stream().filter(d->resourceUuids.contains(d.getResourceId())).collect(Collectors.toList());
        result.addAll(belongToAccountDatas);
        return result;
    }

    private void handle(APIGetMetricDataMsg msg) {
        MetricQueryObject qo = MetricQueryObject.New().startTime(msg.getStartTime())
                .namespace(msg.getNamespace())
                .endTime(msg.getEndTime())
                .period(msg.getPeriod())
                .labels(msg.getLabelList())
                .valueConditions(msg.getValueConditionList())
                .metricName(msg.getMetricName())
                .account(msg.getSession().getAccountUuid())
                .build();

        GetMetricDataFunc func = new GetMetricDataFunc();
        func.queryObject = qo;
        func.functions = msg.getFunctionList();
        func.session = msg.getSession();

        APIGetMetricDataReply reply = new APIGetMetricDataReply();
        reply.setData(func.apply());
        bus.reply(msg, reply);
    }

    private void handle(APIDeleteMetricDataMsg msg) {
        List<Label> labels = null;
        if (msg.getLabels() != null && !msg.getLabels().isEmpty()) {
            labels = msg.getLabels().stream().map(Label::new).collect(Collectors.toList());
        }

        final List<Label> finalLabels = labels;
        if ("all".equals(msg.getNamespace())) {
            Namespace.getNameSpaces().forEach(namespace -> namespace.deleteAll(msg.getMetricName(), finalLabels));
        } else {
            Namespace namespace = Namespace.getMetricNameSpace(msg.getNamespace(), msg.getMetricName());
            if (namespace != null) {
                namespace.deleteAll(msg.getMetricName(), finalLabels);
            }
        }
        APIDeleteMetricDataEvent event = new APIDeleteMetricDataEvent(msg.getId());
        bus.publish(event);
    }

    private List<Datapoint> getMetricDataPoints(GetMetricDataMsg msg) {
        List<Label> labels = null;
        if (msg.getLabels() != null && !msg.getLabels().isEmpty()) {
            labels = msg.getLabels().stream().map(Label::new).collect(Collectors.toList());
        }

        List<Function> functions = null;
        if (msg.getFunctions() != null && !msg.getFunctions().isEmpty()) {
            functions = msg.getFunctions().stream().map(Function::fromString).collect(Collectors.toList());
        }

        if (msg.getOffsetAheadOfCurrentTime() != null) {
            msg.setEndTime(TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()));
            msg.setStartTime(msg.getEndTime() - msg.getOffsetAheadOfCurrentTime());
        }

        SessionInventory session = new SessionInventory();
        session.setAccountUuid(AccountConstant.INITIAL_SYSTEM_ADMIN_UUID);

        MetricQueryObject qo = MetricQueryObject.New().startTime(msg.getStartTime())
                .namespace(msg.getNamespace())
                .endTime(msg.getEndTime())
                .period(msg.getPeriod())
                .labels(labels)
                .metricName(msg.getMetricName())
                .build();

        GetMetricDataFunc func = new GetMetricDataFunc();
        func.queryObject = qo;
        func.functions = functions;
        func.session = session;

        return func.apply();
    }


    private void handle(GetMetricDataMsg msg) {
        GetMetricDataReply reply = new GetMetricDataReply();

        reply.setDatas(getMetricDataPoints(msg));
        bus.reply(msg, reply);
    }

    @Override
    public void getMetricData(GetMetricDataMsg msg, ReturnValueCompletion<List<Datapoint>> completion) {
        completion.success(getMetricDataPoints(msg));
    }

    private void handle(APIGetZWatchAlertHistogramMsg msg) {
        APIGetZWatchAlertHistogramReply reply = new APIGetZWatchAlertHistogramReply();
        List<Histogram> histograms = new ArrayList<>();
        reply.setHistograms(histograms);

        String tableName = msg.getTableName();
        long intervalHours = msg.getIntervalHours();
        String accountUuid = msg.getSession().getAccountUuid();
        boolean isAdmin = acntMgr.isAdmin(msg.getSession());

        List<String> groupColumns = msg.getGroupColumns();
        String groupColumnStr = StringUtils.join(groupColumns, ",");
        if (!groupColumnStr.isEmpty()) {
            groupColumnStr = ",".concat(groupColumnStr);
        }

        long endTime = TimeUnit.MILLISECONDS.toHours(msg.getEndTime());
        endTime = TimeUnit.HOURS.toSeconds(endTime);
        long startTime = TimeUnit.MILLISECONDS.toHours(msg.getStartTime());
        startTime = TimeUnit.HOURS.toSeconds(startTime);
        long timeZoneDiffSecs = TimeUnit.HOURS.toSeconds(getTimeZoneDiffHours());
        long startTimeHour = TimeUnit.SECONDS.toHours(startTime + timeZoneDiffSecs) % 24;

        SQL sql;
        if (isAdmin) {
            String sqlStr = String.format("SELECT hour, count(id) %s FROM %s " +
                            "WHERE hour >= :startTime " +
                            "AND hour <= :endTime " +
                            "GROUP BY hour %s",
                    groupColumnStr, tableName, groupColumnStr);
            sql = SQL.New(sqlStr, Tuple.class);
        } else {
            String sqlStr = String.format("SELECT hour, count(id) %s FROM %s " +
                            "WHERE hour >= :startTime " +
                            "AND hour <= :endTime " +
                            "AND accountUuid = :accountUuid " +
                            "GROUP BY hour %s",
                    groupColumnStr, tableName, groupColumnStr);
            sql = SQL.New(sqlStr, Tuple.class);
            sql.param("accountUuid", accountUuid);
        }

        Map<String, Histogram> histogramMap = new HashMap<>();
        List<Tuple> tuples = sql
                .param("startTime", startTime)
                .param("endTime", endTime)
                .list();
        for (Tuple tuple : tuples) {
            int i = -1;
            long timeSec = tuple.get(++ i, Long.class);
            long count = tuple.get(++ i, Long.class);
            long timeHour = (TimeUnit.SECONDS.toHours(timeSec + timeZoneDiffSecs - TimeUnit.HOURS.toSeconds(startTimeHour)) / intervalHours) * intervalHours;
            long time = TimeUnit.HOURS.toMillis(timeHour) - TimeUnit.SECONDS.toMillis(timeZoneDiffSecs) + TimeUnit.HOURS.toMillis(startTimeHour);

            List<Histogram.Tag> tags = new ArrayList<>();
            for (String column : groupColumns) {
                Histogram.Tag tag = new Histogram.Tag();
                tag.setName(column);
                tag.setValue(tuple.get(++ i, String.class));
                tags.add(tag);
            }

            final long finalTime = time;
            List<String> tagValues = tags.stream()
                    .map(Histogram.Tag::getValue)
                    .collect(Collectors.toList());
            String key = String.format("%s-%s", time, StringUtils.join(tagValues, ",")) ;
            Histogram histogram = histogramMap.computeIfAbsent(key, k -> {
                Histogram h = new Histogram();
                h.setCount(0);
                h.setTime(finalTime);
                h.setTags(tags);
                return h;
            });

            histogram.setCount(count + histogram.getCount());
        }

        histograms.addAll(histogramMap.values());

        List<Long> allPoints = new ArrayList<>();
        for (long i = startTime; i < endTime; i += TimeUnit.HOURS.toSeconds(intervalHours)) {
            allPoints.add(TimeUnit.SECONDS.toMillis(i));
        }
        for (Histogram histogram : histograms) {
            allPoints.remove(histogram.getTime());
        }
        for (long time : allPoints) {
            Histogram histogram = new Histogram();
            histogram.setCount(0L);
            histogram.setTime(time);
            histogram.setTags(Collections.emptyList());
            histograms.add(histogram);
        }

        bus.reply(msg, reply);
    }

    private long getTimeZoneDiffHours() {
        Calendar c = Calendar.getInstance();
        c.setTime(new Date(0));
        return c.get(Calendar.HOUR_OF_DAY);
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(ZWatchConstants.SERVICE_ID);
    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public void afterAttachNicToVm(VmNicInventory nic) {
        if (CoreGlobalProperty.UNIT_TEST_ON) {
            return;
        }

        if (nic == null) return;

        List<Label> labels = new ArrayList<>();
        labels.add(new Label(VmNamespace.LabelNames.VMUuid.name(), Label.Operator.Equal, nic.getVmInstanceUuid()));
        labels.add(new Label(VmNamespace.LabelNames.NetworkDeviceLetter.name(), Label.Operator.Equal,
                nic.getInternalName()));

        List<String> metricNames = new ArrayList<>();
        metricNames.add(VmNamespace.TotalNetworkInBytesIn5Min.getName());
        metricNames.add(VmNamespace.NetworkOutBytes.getName());
        metricNames.add(VmNamespace.NetworkInBytes.getName());
        metricNames.add(VmNamespace.NetworkOutPackets.getName());
        metricNames.add(VmNamespace.NetworkInPackets.getName());

        new While<>(metricNames).each((metricName, completion) -> {
            Namespace.getMetricNameSpace(Namespace.zstackNamespaceName(VmNamespace.NAME), metricName)
                    .deleteAll(metricName, labels);
            completion.done();
        }).run(new WhileDoneCompletion(null) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (errorCodeList != null && errorCodeList.getCauses() != null && !errorCodeList.getCauses().isEmpty()) {
                    logger.warn(String.format("vm [%s] attach nic [%s] delete metric error: [%s]",
                            nic.getVmInstanceUuid(), nic.getUuid(), errorCodeList.getCauses()));
                }
            }
        });
    }

    class CountCache {
        private long size;
        private long expireDate;

        public long getSize() {
            return size;
        }

        public void setSize(long size) {
            this.size = size;
        }

        public long getExpireDate() {
            return expireDate;
        }

        public void setExpireDate(long expireDate) {
            this.expireDate = expireDate;
        }
    }

    @Override
    public void afterHostConnected(HostInventory host) {
        if (!host.getStatus().equals(HostStatus.Connected.toString())) {
            return;
        }
        initQgaZWatchMonitor(host.getUuid());
        startHostPhysicalMemoryMonitor(host.getUuid());
    }

    private void initQgaZWatchMonitor(String hostUuid) {
        //initial qga zwatch monitor to connected host
        InitQgaZWatchMonitorMsg msg = new InitQgaZWatchMonitorMsg();
        msg.setHostUuid(hostUuid);
        msg.setNoStatusCheck(true);
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hostUuid);
        bus.send(msg, new CloudBusCallBack(null) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    logger.warn(String.format("failed to set qga zwatch  monitor initial at host[uuid:%s] after host connected: %s", hostUuid, reply.getError()));
                } else {
                    logger.debug(String.format("successfully to set qga zwatch  monitor initial at host[uuid:%s] after host connected", hostUuid));
                }
            }
        });
    }

    private void startHostPhysicalMemoryMonitor(String hostUuid) {
        StartHostPhysicalMemoryMonitorMsg msg = new StartHostPhysicalMemoryMonitorMsg();
        msg.setHostUuid(hostUuid);
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hostUuid);
        bus.send(msg, new CloudBusCallBack(null) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    logger.warn(String.format("failed to start host physical memory monitor at host[uuid:%s] after host connected: %s", hostUuid, reply.getError()));
                } else {
                    logger.debug(String.format("successfully to start host physical memory monitor at host[uuid:%s] after host connected", hostUuid));
                }
            }
        });

    }
}
