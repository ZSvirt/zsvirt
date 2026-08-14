package org.zstack.zwatch.monitorgroup;

import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.cloudbus.ResourceDestinationMaker;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.db.SQLBatch;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.PeriodicTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.AbstractService;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.managementnode.ManagementNodeReadyExtensionPoint;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.identity.AccountManager;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;
import org.zstack.zwatch.ZWatchGlobalConfig;
import org.zstack.zwatch.alarm.AlarmManagerImpl;
import org.zstack.zwatch.alarm.AlarmStatus;
import org.zstack.zwatch.alarm.AlarmVO;
import org.zstack.zwatch.datatype.EmergencyLevel;
import org.zstack.zwatch.datatype.Namespace;
import org.zstack.zwatch.monitorgroup.api.*;
import org.zstack.zwatch.monitorgroup.entity.*;
import org.zstack.zwatch.monitorgroup.msg.CleanupMonitorGroupInvalidInstanceMsg;
import org.zstack.zwatch.monitorgroup.msg.MonitorGroupMsg;
import org.zstack.zwatch.ruleengine.*;
import org.zstack.zwatch.utils.ParserUtils;

import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.zstack.core.Platform.argerr;

public class MonitorGroupManagerImpl extends AbstractService implements ManagementNodeReadyExtensionPoint {
    protected static final CLogger logger = Utils.getLogger(MonitorGroupManagerImpl.class);

    private Future cleanupMonitorGroupInstanceTask;
    private boolean cleanupInstanceInProgress = false;

    private Map<String, Set<String>> instanceAlarmMap = Collections.synchronizedMap(new HashMap<>());

    @Autowired
    private CloudBus bus;
    @Autowired
    protected ThreadFacade thdf;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ResourceDestinationMaker destinationMaker;
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    protected AccountManager accountManager;
    @Autowired
    RuleManager ruleMgr;

    @Override
    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof MonitorGroupMsg) {
            passThrough((MonitorGroupMsg) msg);
        } else if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage) msg);
        } else {
            handleLocalMessage(msg);
        }
    }

    private void handleLocalMessage(Message msg) {
        bus.dealWithUnknownMessage(msg);
    }

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APICreateMonitorGroupMsg) {
            handle((APICreateMonitorGroupMsg) msg);
        } else if (msg instanceof APICreateMonitorTemplateMsg) {
            handle((APICreateMonitorTemplateMsg) msg);
        } else if (msg instanceof APIAddMetricRuleTemplateMsg) {
            handle((APIAddMetricRuleTemplateMsg) msg);
        } else if (msg instanceof APIAddEventRuleTemplateMsg) {
            handle((APIAddEventRuleTemplateMsg) msg);
        } else if (msg instanceof APIDeleteEventRuleTemplateMsg) {
            handle((APIDeleteEventRuleTemplateMsg) msg);
        } else if (msg instanceof APIDeleteMetricRuleTemplateMsg) {
            handle((APIDeleteMetricRuleTemplateMsg) msg);
        } else if (msg instanceof APICloneMonitorTemplateMsg) {
            handle((APICloneMonitorTemplateMsg) msg);
        }else if (msg instanceof APIUpdateMetricRuleTemplateMsg) {
            handle((APIUpdateMetricRuleTemplateMsg) msg);
        }else if (msg instanceof APIUpdateEventRuleTemplateMsg) {
            handle((APIUpdateEventRuleTemplateMsg) msg);
        }else if (msg instanceof APIUpdateMonitorTemplateMsg) {
            handle((APIUpdateMonitorTemplateMsg) msg);
        } else if (msg instanceof APIDeleteMonitorTemplateMsg) {
            handle((APIDeleteMonitorTemplateMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void passThrough(MonitorGroupMsg msg) {
        MonitorGroupVO vo = dbf.findByUuid(msg.getMonitorGroupUuid(), MonitorGroupVO.class);

        if (vo == null) {
            String err = String.format("Cannot find MonitorGroup[uuid:%s], it may have been deleted", msg.getMonitorGroupUuid());
            bus.replyErrorByMessageType((Message) msg, err);
            return;
        }

        MonitorGroupBase monitorGroupBase = new MonitorGroupBase(vo);
        monitorGroupBase.handleMessage((Message) msg);
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(MonitorGroupConstants.SERVICE_ID);
    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        if (cleanupMonitorGroupInstanceTask != null) {
            cleanupMonitorGroupInstanceTask.cancel(true);
        }

        return true;
    }

    @Override
    public void managementNodeReady() {
        ZWatchGlobalConfig.MONITOR_GROUP_CLEANUP_INSTANCE_INTERVAL.installUpdateExtension((oldConfig, newConfig) -> startMonitorGroupInstanceTask());
        startMonitorGroupInstanceTask();

        installAlarmRuleStateChangeListener();
    }

    private synchronized void startMonitorGroupInstanceTask() {
        if (cleanupMonitorGroupInstanceTask != null) {
            cleanupMonitorGroupInstanceTask.cancel(true);
        }

        cleanupMonitorGroupInstanceTask = thdf.submitPeriodicTask(new PeriodicTask() {
            @Override
            public TimeUnit getTimeUnit() {
                return TimeUnit.SECONDS;
            }

            @Override
            public long getInterval() {
                return ZWatchGlobalConfig.MONITOR_GROUP_CLEANUP_INSTANCE_INTERVAL.value(Long.class);
            }

            @Override
            public String getName() {
                return "cleanup-monitorGroup-nonexistent-instance-task";
            }

            @Override
            public void run() {
                if (!cleanupInstanceInProgress) {
                    cleanupInstanceInProgress = true;
                    cleanupMonitorGroupInstance();
                }
            }
        });
    }

    private void installAlarmRuleStateChangeListener() {
        ruleMgr.installRuleStateChangeListener(new RuleEvaluationResultListener() {
            private String getResourceUuid(RuleEvaluationResult result, AlarmVO vo) {
                Map<String, String> identifyMap = new HashMap<>();
                if (Strings.isNotEmpty(result.getIdentifyLabel())) {
                    identifyMap.putAll(ParserUtils.parseIdentifyLabel(result.getIdentifyLabel()));
                }

                final String identifyLabelName = Namespace.getMetricNameSpace(vo.getNamespace(), vo.getMetricName()).getIdentityLabelName();
                String resourceUuid = identifyMap.get(identifyLabelName);

                if (resourceUuid == null) {
                    resourceUuid = vo.getUuid();
                }

                return resourceUuid;
            }

            private String makeMonitorGroupInstanceKey(String groupUuid, String instanceUuid) {
                return groupUuid.concat("-").concat(instanceUuid);
            }

            @Override
            public void executeActionsFromProblemResults(List<RuleEvaluationResult> results, Rule rule) {
                if (!(rule instanceof AlarmManagerImpl.ARule)) {
                    return;
                }

                String alarmUuid = rule.getUuid();
                boolean exist = Q.New(MonitorGroupAlarmVO.class)
                        .eq(MonitorGroupAlarmVO_.alarmUuid, alarmUuid)
                        .isExists();
                if (!exist) {
                    return;
                }

                AlarmVO alarmVO = dbf.findByUuid(alarmUuid, AlarmVO.class);
                if (alarmVO == null) {
                    return;
                }

                List<String> resourceUuids = new ArrayList<>();
                for (RuleEvaluationResult result : results) {
                    String resourceUuid = getResourceUuid(result, alarmVO);
                    if (resourceUuid != null) {
                        resourceUuids.add(resourceUuid);
                    }
                }
                if (resourceUuids.isEmpty()) {
                    return;
                }

                String groupUuid = Q.New(MonitorGroupAlarmVO.class)
                        .select(MonitorGroupAlarmVO_.groupUuid)
                        .eq(MonitorGroupAlarmVO_.alarmUuid, alarmUuid)
                        .findValue();

                logger.debug(String.format("update monitorGroup[%s] instances[%s] status as %s, The associated alarmUuid is %s",
                        groupUuid, resourceUuids, AlarmStatus.Alarm.name(), alarmUuid));

                SQL.New(MonitorGroupInstanceVO.class)
                        .set(MonitorGroupInstanceVO_.status, AlarmStatus.Alarm)
                        .eq(MonitorGroupInstanceVO_.groupUuid, groupUuid)
                        .in(MonitorGroupInstanceVO_.instanceUuid, resourceUuids)
                        .update();

                for (String instanceUuid : resourceUuids) {
                    String key = makeMonitorGroupInstanceKey(groupUuid, instanceUuid);
                    Set<String> alarmUuids = instanceAlarmMap.computeIfAbsent(key, k -> Collections.synchronizedSet(new HashSet<>()));
                    alarmUuids.add(alarmUuid);
                }
            }

            @Override
            public void executeActionsFromRecoveredResults(List<RuleEvaluationResult> results, Rule rule) {
                if (!(rule instanceof AlarmManagerImpl.ARule)) {
                    return;
                }

                String alarmUuid = rule.getUuid();
                boolean exist = Q.New(MonitorGroupAlarmVO.class)
                        .eq(MonitorGroupAlarmVO_.alarmUuid, alarmUuid)
                        .isExists();
                if (!exist) {
                    return;
                }

                AlarmVO alarmVO = dbf.findByUuid(alarmUuid, AlarmVO.class);
                if (alarmVO == null) {
                    return;
                }

                List<String> resourceUuids = new ArrayList<>();
                for (RuleEvaluationResult result : results) {
                    String resourceUuid = getResourceUuid(result, alarmVO);
                    if (resourceUuid != null) {
                        resourceUuids.add(resourceUuid);
                    }
                }
                if (resourceUuids.isEmpty()) {
                    return;
                }

                String groupUuid = Q.New(MonitorGroupAlarmVO.class)
                        .select(MonitorGroupAlarmVO_.groupUuid)
                        .eq(MonitorGroupAlarmVO_.alarmUuid, alarmUuid)
                        .findValue();

                List<String> recoveredResourceUuids = new ArrayList<>();
                for (String resourceUuid : resourceUuids) {
                    String key = makeMonitorGroupInstanceKey(groupUuid, resourceUuid);
                    Set<String> alarmUuids = instanceAlarmMap.get(key);
                    if (alarmUuids == null) {
                        recoveredResourceUuids.add(resourceUuid);
                    } else {
                        alarmUuids.remove(alarmUuid);
                        if (alarmUuids.isEmpty()) {
                            recoveredResourceUuids.add(resourceUuid);
                        }
                    }
                }
                if (recoveredResourceUuids.isEmpty()) {
                    return;
                }

                logger.debug(String.format("update monitorGroup[%s] instances[%s] status as %s, The associated alarmUuid is %s",
                        groupUuid, recoveredResourceUuids, AlarmStatus.OK.name(), alarmUuid));

                SQL.New(MonitorGroupInstanceVO.class)
                        .set(MonitorGroupInstanceVO_.status, AlarmStatus.OK)
                        .eq(MonitorGroupInstanceVO_.groupUuid, groupUuid)
                        .in(MonitorGroupInstanceVO_.instanceUuid, recoveredResourceUuids)
                        .update();
            }

            @Override
            public void stateChanged(RuleEvaluationResult res, Rule rule) {
                // do nothing
                return;
            }

            @Autowired
            public void problemState(RuleEvaluationResult res, Rule rule) {
                // do nothing
            }
        });
    }

    private void cleanupMonitorGroupInstance() {
        List<String> groupUuids = Q.New(MonitorGroupVO.class)
                .select(MonitorGroupVO_.uuid)
                .listValues();
        if (groupUuids.isEmpty()) {
            cleanupInstanceInProgress = false;
            return;
        }

        List<CleanupMonitorGroupInvalidInstanceMsg> msgs = new ArrayList<>();
        for (String groupUuid : groupUuids) {
            if (!destinationMaker.isManagedByUs(groupUuid)) {
                continue;
            }

            CleanupMonitorGroupInvalidInstanceMsg msg = new CleanupMonitorGroupInvalidInstanceMsg();
            msg.setGroupUuid(groupUuid);
            bus.makeTargetServiceIdByResourceUuid(msg, MonitorGroupConstants.SERVICE_ID, groupUuid);
            msgs.add(msg);
        }

        if (msgs.isEmpty()) {
            cleanupInstanceInProgress = false;
            return;
        }

        new While<>(msgs).each((msg, comp) -> {
            bus.send(msg, new CloudBusCallBack(comp) {
                @Override
                public void run(MessageReply reply) {
                    comp.done();
                }
            });
        }).run(new WhileDoneCompletion(null) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                cleanupInstanceInProgress = false;
            }
        });
    }

    private void handle(APICreateMonitorGroupMsg msg) {
        APICreateMonitorGroupEvent event = new APICreateMonitorGroupEvent(msg.getId());

        MonitorGroupVO vo = new MonitorGroupVO();
        vo.setUuid(msg.getResourceUuid() == null ? Platform.getUuid() : msg.getResourceUuid());
        vo.setName(msg.getName());
        vo.setState(MonitorGroupState.Enabled);
        vo.setDescription(msg.getDescription());
        if (msg.getActions() != null && !msg.getActions().isEmpty()) {
            vo.setActions(JSONObjectUtil.toJsonString(msg.getActions()));
        }
        vo.setCreateDate(new Timestamp(System.currentTimeMillis()));
        vo.setAccountUuid(msg.getSession().getAccountUuid());


        vo = dbf.persist(vo);
        event.setInventory(MonitorGroupInventory.valueOf(vo));
        bus.publish(event);
    }

    private void handle(APICreateMonitorTemplateMsg msg) {
        APICreateMonitorTemplateEvent event = new APICreateMonitorTemplateEvent(msg.getId());
        MonitorTemplateVO templateVO = new MonitorTemplateVO();
        templateVO.setUuid(msg.getResourceUuid() == null ? Platform.getUuid() : msg.getResourceUuid());
        templateVO.setName(msg.getName());
        templateVO.setDescription(msg.getDescription());
        templateVO.setAccountUuid(msg.getSession().getAccountUuid());
        templateVO.setCreateDate(new Timestamp(System.currentTimeMillis()));
        templateVO = dbf.persist(templateVO);
        event.setInventory(MonitorTemplateInventory.valueOf(templateVO));
        bus.publish(event);
    }

    private void handle(APIUpdateMonitorTemplateMsg msg) {
        APIUpdateMonitorTemplateEvent event = new APIUpdateMonitorTemplateEvent(msg.getId());

        MonitorTemplateVO templateVO = dbf.findByUuid(msg.getUuid(), MonitorTemplateVO.class);
        boolean needUpdate = false;

        if (msg.getName() != null) {
            templateVO.setName(msg.getName());
            needUpdate = true;
        }

        if (msg.getDescription() != null) {
            templateVO.setDescription(msg.getDescription());
            needUpdate = true;
        }

        if (needUpdate) {
            templateVO = dbf.updateAndRefresh(templateVO);
        }

        event.setInventory(MonitorTemplateInventory.valueOf(templateVO));
        bus.publish(event);
    }

    private long getTemplateRuleNum(String monitorTemplateUuid) {
        long count = Q.New(MetricRuleTemplateVO.class)
                .eq(MetricRuleTemplateVO_.monitorTemplateUuid, monitorTemplateUuid)
                .count();
        count += Q.New(EventRuleTemplateVO.class)
                .eq(EventRuleTemplateVO_.monitorTemplateUuid, monitorTemplateUuid)
                .count();
        return count;
    }

    private void handle(APIAddMetricRuleTemplateMsg msg) {
        APIAddMetricRuleTemplateEvent event = new APIAddMetricRuleTemplateEvent(msg.getId());

        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return String.format("monitorTemplate-%s", msg.getMonitorTemplateUuid());
            }

            @Override
            public void run(SyncTaskChain chain) {
                long count = getTemplateRuleNum(msg.getMonitorTemplateUuid());
                if (count >= ZWatchGlobalConfig.MONITOR_TEMPLATE_MAXIMUM_RULE_NUM.value(Long.class)) {
                    event.setError(argerr("The rule in the template has reached the maximum limit"));
                    bus.publish(event);
                    chain.next();
                    return;
                }

                MetricRuleTemplateVO templateVO = new MetricRuleTemplateVO();
                templateVO.setUuid(msg.getResourceUuid() == null ? Platform.getUuid() : msg.getResourceUuid());
                templateVO.setMonitorTemplateUuid(msg.getMonitorTemplateUuid());
                templateVO.setName(msg.getName());
                templateVO.setComparisonOperator(ComparisonOperator.valueOf(msg.getComparisonOperator()));
                templateVO.setMetricName(msg.getMetricName());
                templateVO.setNamespace(msg.getNamespace());
                templateVO.setPeriod(msg.getPeriod());
                templateVO.setThreshold(msg.getThreshold());
                templateVO.setRepeatInterval(msg.getRepeatInterval() == null ? ZWatchGlobalConfig.ALARM_REPEAT_INTERVAL.value(Integer.class) : msg.getRepeatInterval());
                templateVO.setAccountUuid(msg.getSession().getAccountUuid());
                templateVO.setEnableRecovery(msg.getEnableRecovery());
                templateVO.setRepeatCount(msg.getRepeatCount());
                if (msg.getLabels() != null) {
                    templateVO.setLabels(JSONObjectUtil.toJsonString(msg.getLabels()));
                }
                if (msg.getEmergencyLevel() != null) {
                    templateVO.setEmergencyLevel(EmergencyLevel.valueOf(msg.getEmergencyLevel()));
                }
                templateVO.setCreateDate(new Timestamp(System.currentTimeMillis()));

                templateVO = dbf.persist(templateVO);

                updateMonitorGroup(msg.getMonitorTemplateUuid(), Boolean.FALSE);

                event.setInventory(MetricRuleTemplateInventory.valueOf(templateVO));
                bus.publish(event);
                chain.next();
            }

            @Override
            public String getName() {
                return "add-metric-rule-template";
            }
        });
    }

    private void handle(APIUpdateMetricRuleTemplateMsg msg) {
        APIUpdateMetricRuleTemplateEvent event = new APIUpdateMetricRuleTemplateEvent(msg.getId());

        MetricRuleTemplateVO vo = dbf.findByUuid(msg.getUuid(), MetricRuleTemplateVO.class);
        boolean needUpdate = false;

        if (msg.getName() != null) {
            vo.setName(msg.getName());
            needUpdate = true;
        }

        if (msg.getComparisonOperator() != null) {
            vo.setComparisonOperator(ComparisonOperator.valueOf(msg.getComparisonOperator()));
            needUpdate = true;
        }

        if (msg.getPeriod() != null) {
            vo.setPeriod(msg.getPeriod());
            needUpdate = true;
        }

        if (msg.getThreshold() != null) {
            vo.setThreshold(msg.getThreshold());
            needUpdate = true;
        }

        if (msg.getRepeatInterval() != null) {
            vo.setRepeatInterval(msg.getRepeatInterval());
            needUpdate = true;
        }

        if (msg.getLabels() != null) {
            vo.setLabels(JSONObjectUtil.toJsonString(msg.getLabels()));
            needUpdate = true;
        }

        if (msg.getRepeatCount() != null) {
            vo.setRepeatCount(msg.getRepeatCount());
            needUpdate = true;
        }

        if (msg.getEnableRecovery() != null) {
            vo.setEnableRecovery(msg.getEnableRecovery());
            needUpdate = true;
        }

        if (msg.getEmergencyLevel() != null) {
            vo.setEmergencyLevel(EmergencyLevel.valueOf(msg.getEmergencyLevel()));
            needUpdate = true;
        }

        if (needUpdate) {
            vo = dbf.updateAndRefresh(vo);

            String monitorTemplateUuid = dbf.findByUuid(msg.getUuid(), MetricRuleTemplateVO.class).getMonitorTemplateUuid();
            updateMonitorGroup(monitorTemplateUuid, Boolean.FALSE);
        }

        event.setInventory(MetricRuleTemplateInventory.valueOf(vo));
        bus.publish(event);
    }

    private void handle(APIDeleteMetricRuleTemplateMsg msg) {
        APIDeleteMetricRuleTemplateEvent event = new APIDeleteMetricRuleTemplateEvent(msg.getId());
        String monitorTemplateUuid = dbf.findByUuid(msg.getUuid(), MetricRuleTemplateVO.class).getMonitorTemplateUuid();
        SQL.New(MetricRuleTemplateVO.class)
                .eq(MetricRuleTemplateVO_.uuid, msg.getUuid())
                .hardDelete();
        updateMonitorGroup(monitorTemplateUuid, Boolean.FALSE);
        bus.publish(event);
    }

    private void handle(APIUpdateEventRuleTemplateMsg msg) {
        APIUpdateEventRuleTemplateEvent event = new APIUpdateEventRuleTemplateEvent(msg.getId());

        EventRuleTemplateVO templateVO = dbf.findByUuid(msg.getUuid(), EventRuleTemplateVO.class);
        boolean needUpdate = false;

        if (msg.getName() != null) {
            templateVO.setName(msg.getName());
            needUpdate = true;
        }

        if (msg.getEmergencyLevel() != null) {
            templateVO.setEmergencyLevel(EmergencyLevel.valueOf(msg.getEmergencyLevel()));
            needUpdate = true;
        }

        if (msg.getLabels() != null) {
            templateVO.setLabels(JSONObjectUtil.toJsonString(msg.getLabels()));
            needUpdate = true;
        }

        if (needUpdate) {
            templateVO = dbf.updateAndRefresh(templateVO);
            String monitorTemplateUuid = dbf.findByUuid(msg.getUuid(), EventRuleTemplateVO.class).getMonitorTemplateUuid();
            updateMonitorGroup(monitorTemplateUuid, Boolean.FALSE);
        }

        event.setInventory(EventRuleTemplateInventory.valueOf(templateVO));
        bus.publish(event);
    }

    private void handle(APIAddEventRuleTemplateMsg msg) {
        APIAddEventRuleTemplateEvent event = new APIAddEventRuleTemplateEvent(msg.getId());

        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return String.format("monitorTemplate-%s", msg.getMonitorTemplateUuid());
            }

            @Override
            public void run(SyncTaskChain chain) {
                long count = getTemplateRuleNum(msg.getMonitorTemplateUuid());
                if (count >= ZWatchGlobalConfig.MONITOR_TEMPLATE_MAXIMUM_RULE_NUM.value(Long.class)) {
                    event.setError(argerr("The rule in the template has reached the maximum limit"));
                    bus.publish(event);
                    chain.next();
                    return;
                }

                String uuid = msg.getResourceUuid() != null ? msg.getResourceUuid() : Platform.getUuid();
                EventRuleTemplateVO templateVO = new EventRuleTemplateVO();
                templateVO.setMonitorTemplateUuid(msg.getMonitorTemplateUuid());
                templateVO.setUuid(uuid);
                templateVO.setName(msg.getName());
                templateVO.setNamespace(msg.getNamespace());
                if (msg.getLabels() != null) {
                    templateVO.setLabels(JSONObjectUtil.toJsonString(msg.getLabels()));
                }
                templateVO.setEventName(msg.getEventName());
                if (msg.getEmergencyLevel() != null) {
                    templateVO.setEmergencyLevel(EmergencyLevel.valueOf(msg.getEmergencyLevel()));
                }
                templateVO.setAccountUuid(msg.getSession().getAccountUuid());
                templateVO.setCreateDate(new Timestamp(System.currentTimeMillis()));

                templateVO = dbf.persist(templateVO);

                updateMonitorGroup(msg.getMonitorTemplateUuid(), Boolean.FALSE);

                event.setInventory(EventRuleTemplateInventory.valueOf(templateVO));
                bus.publish(event);
                chain.next();
            }

            @Override
            public String getName() {
                return String.format("add-event-rule-template");
            }
        });
    }

    private void handle(APIDeleteEventRuleTemplateMsg msg) {
        APIDeleteEventRuleTemplateEvent event = new APIDeleteEventRuleTemplateEvent(msg.getId());
        String monitorTemplateUuid = dbf.findByUuid(msg.getUuid(), EventRuleTemplateVO.class).getMonitorTemplateUuid();
        SQL.New(EventRuleTemplateVO.class)
                .eq(EventRuleTemplateVO_.uuid, msg.getUuid())
                .hardDelete();
        updateMonitorGroup(monitorTemplateUuid, Boolean.FALSE);
        bus.publish(event);
    }

    private void handle(APIDeleteMonitorTemplateMsg msg) {
        APIDeleteMonitorTemplateEvent event = new APIDeleteMonitorTemplateEvent(msg.getId());


        new SQLBatch() {
            @Override
            protected void scripts() {
                sql(MetricRuleTemplateVO.class)
                        .eq(MetricRuleTemplateVO_.monitorTemplateUuid, msg.getUuid())
                        .hardDelete();

                sql(EventRuleTemplateVO.class)
                        .eq(EventRuleTemplateVO_.monitorTemplateUuid, msg.getUuid())
                        .hardDelete();

                sql(MonitorGroupTemplateRefVO.class)
                        .eq(MonitorGroupTemplateRefVO_.templateUuid, msg.getUuid())
                        .hardDelete();

                sql(MonitorTemplateVO.class)
                        .eq(MonitorTemplateVO_.uuid, msg.getUuid())
                        .hardDelete();
            }
        }.execute();

        bus.publish(event);
    }

    private void handle(APICloneMonitorTemplateMsg msg) {
        APICloneMonitorTemplateEvent event = new APICloneMonitorTemplateEvent(msg.getId());

        MonitorTemplateVO templateVO = dbf.findByUuid(msg.getUuid(), MonitorTemplateVO.class);
        templateVO.setUuid(msg.getResourceUuid() != null ? msg.getResourceUuid() : Platform.getUuid());
        templateVO.setName(msg.getName());
        templateVO.setAccountUuid(msg.getSession().getAccountUuid());
        templateVO.setCreateDate(new Timestamp(System.currentTimeMillis()));
        templateVO.setDescription(msg.getDescription());
        templateVO.setMonitorGroupTemplateRefs(null);

        List<MetricRuleTemplateVO> metricRuleTemplateVOS = Q.New(MetricRuleTemplateVO.class)
                .eq(MetricRuleTemplateVO_.monitorTemplateUuid, msg.getUuid())
                .list();
        for (MetricRuleTemplateVO metricRuleTemplateVO : metricRuleTemplateVOS) {
            metricRuleTemplateVO.setUuid(Platform.getUuid());
            metricRuleTemplateVO.setMonitorTemplateUuid(templateVO.getUuid());
            metricRuleTemplateVO.setAccountUuid(msg.getSession().getAccountUuid());
            metricRuleTemplateVO.setCreateDate(new Timestamp(System.currentTimeMillis()));
        }

        List<EventRuleTemplateVO> eventRuleTemplateVOS = Q.New(EventRuleTemplateVO.class)
                .eq(EventRuleTemplateVO_.monitorTemplateUuid, msg.getUuid())
                .list();
        for (EventRuleTemplateVO eventRuleTemplateVO : eventRuleTemplateVOS) {
            eventRuleTemplateVO.setUuid(Platform.getUuid());
            eventRuleTemplateVO.setMonitorTemplateUuid(templateVO.getUuid());
            eventRuleTemplateVO.setAccountUuid(msg.getSession().getAccountUuid());
            eventRuleTemplateVO.setCreateDate(new Timestamp(System.currentTimeMillis()));
        }

        new SQLBatch() {
            @Override
            protected void scripts() {
                persist(templateVO);

                for (MetricRuleTemplateVO metricRuleTemplateVO : metricRuleTemplateVOS) {
                    persist(metricRuleTemplateVO);
                }

                for (EventRuleTemplateVO eventRuleTemplateVO : eventRuleTemplateVOS) {
                    persist(eventRuleTemplateVO);
                }
            }
        }.execute();

        event.setInventory(MonitorTemplateInventory.valueOf(templateVO));
        bus.publish(event);
    }

    private void updateMonitorGroup(String monitorTemplateUuid, Boolean isApplied) {
        SQL.New(MonitorGroupTemplateRefVO.class)
                .eq(MonitorGroupTemplateRefVO_.templateUuid, monitorTemplateUuid)
                .set(MonitorGroupTemplateRefVO_.isApplied, isApplied)
                .update();
    }
}
