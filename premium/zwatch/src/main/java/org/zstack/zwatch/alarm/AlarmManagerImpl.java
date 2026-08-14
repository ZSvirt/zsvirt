package org.zstack.zwatch.alarm;

import com.google.common.base.Joiner;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.Platform;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.*;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.*;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.AbstractService;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.GlobalApiMessageInterceptor;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.NopeWhileDoneCompletion;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.identity.APIChangeResourceOwnerMsg;
import org.zstack.header.identity.Quota;
import org.zstack.header.identity.ReportQuotaExtensionPoint;
import org.zstack.header.identity.quota.QuotaMessageHandler;
import org.zstack.header.managementnode.ManagementNodeCanonicalEvent;
import org.zstack.header.managementnode.ManagementNodeChangeListener;
import org.zstack.header.managementnode.ManagementNodeInventory;
import org.zstack.header.managementnode.ManagementNodeReadyExtensionPoint;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.identity.AccountManager;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;
import org.zstack.zwatch.ZWatchConstants;
import org.zstack.zwatch.ZWatchGlobalConfig;
import org.zstack.zwatch.ZWatchQuotaConstant;
import org.zstack.zwatch.datatype.*;
import org.zstack.zwatch.driver.EventDatabaseDriver;
import org.zstack.zwatch.migratedb.MysqlAuditAction;
import org.zstack.zwatch.namespace.NamespaceEventManager;
import org.zstack.zwatch.namespace.ThirdpartyAlertNamespace;
import org.zstack.zwatch.quota.ZWatchAlarmNumQuotaDefinition;
import org.zstack.zwatch.quota.ZWatchEventNumQuotaDefinition;
import org.zstack.zwatch.ruleengine.*;

import javax.persistence.Tuple;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.operr;
import static org.zstack.utils.CollectionDSL.*;
import static org.zstack.zwatch.alarm.system.AlarmSystemTagUtils.*;
import static org.zstack.zwatch.alarm.system.AlarmSystemTagUtils.EVENT_SUBSCRIPTION_NAME_MAP;
import static org.zstack.zwatch.alarm.system.AlarmSystemTagUtils.EVENT_SUBSCRIPTION_CN_SYSTEM_TAG_MAP;
import static org.zstack.zwatch.utils.ParserUtils.getResourceUuid;

public class AlarmManagerImpl extends AbstractService implements AlarmManager, ManagementNodeReadyExtensionPoint, ReportQuotaExtensionPoint,
        ManagementNodeChangeListener {
    private final static CLogger logger = Utils.getLogger(AlarmManagerImpl.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private AccountManager acntMgr;
    @Autowired
    private RuleManager ruleMgr;
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ThreadFacade thdf;
    @Autowired
    private ResourceDestinationMaker destinationMaker;
    @Autowired
    private NamespaceEventManager eventMgr;
    @Autowired
    private EventFacade evtf;

    public static String ALARM_EXCLUDE_RESOURCE_LABEL = "ALARM_EXCLUDE_RESOURCE_LABEL";
    private static String ALARM_EXCLUDE_RESOURCE_SEPARATOR = "|";

    private Map<String, AlarmActionFactory> alarmActionFactories = new HashMap<>();
    private Map<String, AlarmFactory> alarmFactories = new HashMap<>();

    @Override
    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof AlarmMessage) {
            passThrough((AlarmMessage) msg);
        } else if (msg instanceof EventSubscriptionMessage) {
            passThrough((EventSubscriptionMessage) msg);
        } else if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage) msg);
        } else {
            handleLocalMessage(msg);
        }
    }

    private void passThrough(EventSubscriptionMessage msg) {
        EventSubscriptionVO vo = dbf.findByUuid(msg.getSubscriptionUuid(), EventSubscriptionVO.class);
        if (vo == null) {
            throw new OperationFailureException(operr("cannot find the event subscription[uuid:%s], it may have been deleted", msg.getSubscriptionUuid()));
        }

        new EventSubscriptionBase(vo).handleMessage((Message) msg);
    }

    private void passThrough(AlarmMessage msg) {
        AlarmVO vo = dbf.findByUuid(msg.getAlarmUuid(), AlarmVO.class);
        if (vo == null) {
            throw new OperationFailureException(operr("cannot find the alarm[uuid:%s], it may have been deleted", msg.getAlarmUuid()));
        }

        new AlarmBase(vo).handleMessage((Message) msg);
    }

    private void handleLocalMessage(Message msg) {
        if (msg instanceof SubscribeEventMsg) {
            handle((SubscribeEventMsg) msg);
        } else if (msg instanceof CreateAlarmMsg) {
            handle((CreateAlarmMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APICreateAlarmMsg) {
            handle((APICreateAlarmMsg) msg);
        } else if (msg instanceof APISubscribeEventMsg) {
            handle((APISubscribeEventMsg) msg);
        } else if (msg instanceof APIUpdateSubscribeEventMsg) {
            handle((APIUpdateSubscribeEventMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(SubscribeEventMsg msg) {
        EventSubscriptionInventory inv = new SQLBatchWithReturn<EventSubscriptionInventory>() {
            @Override
            protected EventSubscriptionInventory scripts() {
                EventSubscriptionVO vo = new EventSubscriptionVO();
                vo.setUuid(msg.getResourceUuid() == null ? Platform.getUuid() : msg.getResourceUuid());
                vo.setName(StringUtils.isNotBlank(msg.getName())
                        ? msg.getName()
                        : EVENT_SUBSCRIPTION_NAME_MAP.get(msg.getEventName()));
                vo.setEventName(msg.getEventName());
                vo.setNamespace(msg.getNamespace());
                vo.setState(EventSubscriptionState.Enabled);
                vo.setAccountUuid(msg.getAccountUuid());
                if (msg.getEmergencyLevel() != null) {
                    vo.setEmergencyLevel(EmergencyLevel.valueOf(msg.getEmergencyLevel()));
                }
                persist(vo);

                if (msg.getActions() != null) {
                    msg.getActions().forEach(a -> {
                        EventSubscriptionActionVO avo = new EventSubscriptionActionVO();
                        avo.setActionType(a.actionType);
                        avo.setActionUuid(a.actionUuid);
                        avo.setSubscriptionUuid(vo.getUuid());
                        persist(avo);
                    });
                }

                if (msg.getLabels() != null) {
                    msg.getLabels().forEach(l -> {
                        EventSubscriptionLabelVO lvo = new EventSubscriptionLabelVO();
                        lvo.setUuid(Platform.getUuid());
                        lvo.setKey(l.getKey());
                        lvo.setSubscriptionUuid(vo.getUuid());
                        lvo.setOperator(l.getOp());
                        lvo.setValue(l.getValue());
                        persist(lvo);
                    });
                }

                reload(vo);

                persistSystemTagOfLanguage(vo,
                        EventSubscriptionSystemTags.CN,
                        EVENT_SUBSCRIPTION_CN_SYSTEM_TAG_MAP.get(vo.getEventName()));

                return EventSubscriptionInventory.valueOf(vo);
            }
        }.execute();

        if (!inv.getActions().isEmpty() && inv.getState().equals(EventSubscriptionState.Enabled)) {
            subscribeEvent(inv);
        }

        SubscribeEventReply reply = new SubscribeEventReply();
        reply.setInventory(inv);
        bus.reply(msg, reply);
    }

    private void handle(APISubscribeEventMsg msg) {
        String resourceUuid = msg.getResourceUuid() == null ? Platform.getUuid() : msg.getResourceUuid();

        SubscribeEventMsg smsg = new SubscribeEventMsg();
        smsg.setName(msg.getName());
        smsg.setEventName(msg.getEventName());
        smsg.setActions(msg.getActions());
        smsg.setLabels(msg.getLabels());
        smsg.setNamespace(msg.getNamespace());
        smsg.setResourceUuid(resourceUuid);
        smsg.setAccountUuid(msg.getSession().getAccountUuid());
        smsg.setEmergencyLevel(msg.getEmergencyLevel());
        bus.makeTargetServiceIdByResourceUuid(smsg, AlarmConstants.SERVICE_ID, smsg.getResourceUuid());
        bus.send(smsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                APISubscribeEventEvent evt = new APISubscribeEventEvent(msg.getId());
                if (!reply.isSuccess()) {
                    evt.setError(reply.getError());
                    bus.publish(evt);
                    return;
                }

                evt.setInventory(((SubscribeEventReply)reply).getInventory());
                bus.publish(evt);
            }
        });
    }

    private void handle(APIUpdateSubscribeEventMsg msg) {
        APIUpdateSubscribeEventEvent event = new APIUpdateSubscribeEventEvent(msg.getId());

        EventSubscriptionVO eventSubscriptionVO = dbf.findByUuid( msg.getUuid(), EventSubscriptionVO.class);
        if (msg.getEmergencyLevel() != null) {
            eventSubscriptionVO.setEmergencyLevel(EmergencyLevel.valueOf(msg.getEmergencyLevel()));
        }

        if (msg.getName() != null) {
            eventSubscriptionVO.setName(msg.getName());
        }

        eventSubscriptionVO = dbf.updateAndRefresh(eventSubscriptionVO);
        event.setInventory(EventSubscriptionInventory.valueOf(eventSubscriptionVO));
        bus.publish(event);
    }

    private void subscribeEvent(EventSubscriptionInventory inv) {
        EventDatabaseDriver.EventSubscriber subscriber = new EventDatabaseDriver.EventSubscriber();
        subscriber.uuid = inv.getUuid();
        subscriber.eventName = inv.getEventName();
        subscriber.namespace = inv.getNamespace();
        subscriber.labels = inv.getLabels().stream().map(EventSubscriptionLabelInventory::toLabel).collect(Collectors.toList());

        if (inv.getNamespace().endsWith(ThirdpartyAlertNamespace.NAME)) {
            eventMgr.getEventDatabaseDriver().subscribeEvent(subscriber, event-> consumeThirdpartyEvent(inv, event));
        } else {
            eventMgr.getEventDatabaseDriver().subscribeEvent(subscriber, event-> consumeEvent(inv, event));
        }
    }

    private boolean isInterceptAlarm(EventSubscriptionInventory inv, EventData event){
        String resourceUuid = event.getResourceId();
        resourceUuid = resourceUuid == null ? inv.getUuid() : resourceUuid;
        EventDataAckVO ackVO = Q.New(EventDataAckVO.class)
                .eq(EventDataAckVO_.eventSubscriptionUuid, inv.getUuid())
                .eq(EventDataAckVO_.resourceUuid, resourceUuid)
                .orderBy(EventDataAckVO_.ackDate, SimpleQuery.Od.DESC)
                .limit(1)
                .find();
        if (ackVO == null){
            return false;
        }

        if (ackVO.isResumeAlert()) {
            return false;
        }

        long mills = TimeUnit.SECONDS.toMillis(ackVO.getAckPeriod());
        long silentTime = ackVO.getAckDate().getTime() + mills;
        long currentTime = System.currentTimeMillis();
        if (silentTime > currentTime) {
            logger.info(String.format("Alarm interception[subscriptionUuid=%s, resourceUuid=%s], intercept deadline %s", inv.getUuid(), resourceUuid, new Timestamp(silentTime)));
            return true;
        }

        return false;
    }

    private void consumeEvent(EventSubscriptionInventory inv, EventData event) {
        if (isInterceptAlarm(inv, event)) {
            return;
        }

        List<AlarmAction> actions = new ArrayList<>();
        //actions.add(new AuditAction());
        actions.add(new MysqlAuditAction());

        inv.getActions().forEach(action -> {
            AlarmActionFactory f = getAlarmActionFactory(action.getActionType());
            AlarmAction aa = f.createAlarmAction(action.getActionUuid());
           actions.add(aa);
        });

        String dataUuid = Platform.getUuid();
        actions.forEach(action -> {
            AlarmAction.TakeEventSubscriptionActionParam param = new AlarmAction.TakeEventSubscriptionActionParam();
            param.event = event;
            param.subscriptionUuid = inv.getUuid();
            param.subscriptionAccountUuid = acntMgr.getOwnerAccountUuidOfResource(inv.getUuid());
            param.dataUuid = dataUuid;

            action.takeAction(param);
        });
    }

    private void consumeThirdpartyEvent(EventSubscriptionInventory inv, EventData event) {
        List<AlarmAction> actions = new ArrayList<>();

        inv.getActions().forEach(action -> {
            AlarmActionFactory f = getAlarmActionFactory(action.getActionType());
            AlarmAction aa = f.createAlarmAction(action.getActionUuid());
            actions.add(aa);
        });

        String dataUuid = Platform.getUuid();
        actions.forEach(action -> {
            AlarmAction.TakeEventSubscriptionActionParam param = new AlarmAction.TakeEventSubscriptionActionParam();
            param.event = event;
            param.subscriptionUuid = inv.getUuid();
            param.subscriptionAccountUuid = acntMgr.getOwnerAccountUuidOfResource(inv.getUuid());
            param.dataUuid = dataUuid;

            action.takeActionForThirdpartyAlert(param);
        });
    }

    @Override
    public void managementNodeReady() {
        takeoverAlarmsAndEventSubscriptions(new NoErrorCompletion() {
            @Override
            public void done() {
                takeOverAlarms();
                takeOverEventSubscriptions();
            }
        });
    }

    @Override
    public List<Quota> reportQuota() {
        Quota quota = new Quota();
        quota.defineQuota(new ZWatchAlarmNumQuotaDefinition());
        quota.defineQuota(new ZWatchEventNumQuotaDefinition());
        quota.addQuotaMessageChecker(new QuotaMessageHandler<>(APICreateAlarmMsg.class)
                .addCounterQuota(ZWatchQuotaConstant.ZWATCH_ALARM_NUM));
        quota.addQuotaMessageChecker(new QuotaMessageHandler<>(APISubscribeEventMsg.class)
                .addCounterQuota(ZWatchQuotaConstant.ZWATCH_EVENT_NUM));
        quota.addQuotaMessageChecker(new QuotaMessageHandler<>(APIChangeResourceOwnerMsg.class)
                .addCheckCondition((msg) -> Q.New(AlarmVO.class)
                        .eq(AlarmVO_.uuid, msg.getResourceUuid())
                        .isExists())
                .addCounterQuota(ZWatchQuotaConstant.ZWATCH_ALARM_NUM));
        quota.addQuotaMessageChecker(new QuotaMessageHandler<>(APIChangeResourceOwnerMsg.class)
                .addCheckCondition((msg) -> Q.New(EventSubscriptionVO.class)
                        .eq(EventSubscriptionVO_.uuid, msg.getResourceUuid())
                        .isExists())
                .addCounterQuota(ZWatchQuotaConstant.ZWATCH_EVENT_NUM));

        return list(quota);
    }

    @Override
    public void nodeJoin(ManagementNodeInventory inv) {
        takeoverAlarmsAndEventSubscriptions(new NoErrorCompletion() {
            @Override
            public void done() {
                takeOverAlarms();
                takeOverEventSubscriptions();
            }
        });
    }

    private synchronized void takeoverAlarmsAndEventSubscriptions(NoErrorCompletion completion) {
        completion.done();
        List<String> uuids = eventMgr.getEventDatabaseDriver().getAllSubscriberUuids();
        logger.debug(String.format("subscriberUuids%s takeover by the current node", uuids));
    }

    private void takeOverAlarms() {
        List<String> alarmsManagedByUs = getAlarmsManagedByUs();

        // get current node managed alarm rule uuid list
        List<String> includeAlarmUuids = ruleMgr.getAllRules().stream()
                .map(Rule::getUuid)
                .filter(alarmsManagedByUs::contains)
                .collect(Collectors.toList());

        // remove the rules should not be managed by this node
        ruleMgr.deleteIf(r -> r instanceof ARule && !alarmsManagedByUs.contains(r.getUuid()));

        // remove all already managed alarm uuid from whole list
        alarmsManagedByUs.removeAll(includeAlarmUuids);

        if (alarmsManagedByUs.isEmpty()) {
            return;
        }

        // load unmanaged alarm
        loadAlarms(alarmsManagedByUs);
    }

    private List<String> getAlarmsManagedByUs() {
        logger.debug("Starting to take over alarm job");
        List<String> uuidsManagedByCurrentNode = new ArrayList<String>();
        int qun = 10000;
        long amount = dbf.count(AlarmVO.class);
        int times = (int) (amount / qun) + (amount % qun != 0 ? 1 : 0);
        int start = 0;
        for (int i = 0; i < times; i++) {
            SimpleQuery<AlarmVO> q = dbf.createQuery(AlarmVO.class);
            q.select(AlarmVO_.uuid);
            q.setLimit(qun);
            q.setStart(start);
            List<String> uuids = q.listValue();
            for (String uuid : uuids) {
                if (!destinationMaker.isManagedByUs(uuid)) {
                    continue;
                }

                uuidsManagedByCurrentNode.add(uuid);
            }
            start += qun;
        }

        return  uuidsManagedByCurrentNode;
    }

    private void takeOverEventSubscriptions() {
        long count = Q.New(EventSubscriptionVO.class).count();

        List<String> subscriptionsManagedByUs = new ArrayList<>();
        List<String> notManagedByUs = new ArrayList<>();

        SQL.New("select vo.uuid from EventSubscriptionVO vo", String.class).limit(1000).paginate(count, (List<String> uuids) -> uuids.forEach(uuid -> {
            if (destinationMaker.isManagedByUs(uuid)) {
                subscriptionsManagedByUs.add(uuid);
            } else {
                notManagedByUs.add(uuid);
            }
        }));

        List<String> uuids = eventMgr.getEventDatabaseDriver().getAllSubscriberUuids();

        List<String> needUnsubscribeUuids = notManagedByUs.stream()
                .filter(uuids::contains)
                .collect(Collectors.toList());
        for (String uuid : needUnsubscribeUuids) {
            eventMgr.getEventDatabaseDriver().unsubscribeEvent(uuid);
        }

        List<String> needSubscribeUuids = subscriptionsManagedByUs.stream()
                .filter(uuid -> !uuids.contains(uuid))
                .collect(Collectors.toList());
        for (String uuid : needSubscribeUuids) {
            EventSubscriptionVO vo = dbf.findByUuid(uuid, EventSubscriptionVO.class);
            if (vo.getState().equals(EventSubscriptionState.Enabled)) {
                subscribeEvent(EventSubscriptionInventory.valueOf(vo));
            }
        }
    }

    @Override
    public void nodeLeft(ManagementNodeInventory inv) {
        takeoverAlarmsAndEventSubscriptions(new NoErrorCompletion() {
            @Override
            public void done() {
                takeOverAlarms();
                takeOverEventSubscriptions();
            }
        });

        ManagementNodeCanonicalEvent.ManagementNodeLifeCycleData d = new ManagementNodeCanonicalEvent.ManagementNodeLifeCycleData();
        d.setNodeUuid(inv.getUuid());
        d.setLifeCycle(ManagementNodeCanonicalEvent.LifeCycle.NodeLeft.toString());
        d.setInventory(inv);
        evtf.fire(ZWatchConstants.NODE_LIFECYCLE_LEFT_CANONICAL_EVENT_PATH, d);
    }

    @Override
    public void iAmDead(ManagementNodeInventory inv) {

    }

    @Override
    public void iJoin(ManagementNodeInventory inv) {
        takeoverAlarmsAndEventSubscriptions(new NoErrorCompletion() {
            @Override
            public void done() {
                takeOverAlarms();
                takeOverEventSubscriptions();
            }
        });
    }

    public static class ARule extends MetricRule {
        RuleEvaluationResultListener listener;
        int repeatInterval;
        long lastProblemTime;
        long lastEvalTime;
        int repeatCount;

        ARule(String uuid) {
            super(uuid);
        }

        @Override
        public List<RuleEvaluationResult> eval() {
            lastEvalTime = System.currentTimeMillis();
            List<RuleEvaluationResult> results = super.eval();

            if (logger.isTraceEnabled()) {
                logger.trace(String.format("[Alarm Rule Evaluation] rule %s evaluation result: %s", JSONObjectUtil.toJsonString(this), JSONObjectUtil.toJsonString(results)));
            }

            return results;
        }

        @Override
        public boolean needEvaluate() {
            if (CoreGlobalProperty.UNIT_TEST_ON) {
                return true;
            }

            return System.currentTimeMillis() - lastEvalTime >= TimeUnit.SECONDS.toMillis(getDuration());
        }

        @Override
        public RuleEvaluationResultListener getRuleStateChangeListener() {
            return listener;
        }
    }

    class AlarmResourceRepeatCountChecker {
        private String alarmUuid;
        private ConcurrentHashMap<String, AtomicInteger> repeatCountMap = new ConcurrentHashMap<>();
        Set<String> excludeResourceUuids = Collections.synchronizedSet(new HashSet<>());

        AlarmResourceRepeatCountChecker(String alarmUuid) {
            this.alarmUuid = alarmUuid;
        }

        public String getAlarmUuid() {
            return alarmUuid;
        }

        public void setAlarmUuid(String alarmUuid) {
            this.alarmUuid = alarmUuid;
        }

        int incrementAndGet(String resourceUuid) {
            repeatCountMap.computeIfAbsent(resourceUuid, x -> new AtomicInteger(0));
            return repeatCountMap.get(resourceUuid).incrementAndGet();
        }

        boolean isExcludeResource(String resourceUuid) {
            return excludeResourceUuids.contains(resourceUuid);
        }

        void addToExcludeResource(String resourceUuid) {
            excludeResourceUuids.add(resourceUuid);
            repeatCountMap.remove(resourceUuid);
            new SQLBatch() {
                @Override
                protected void scripts() {
                    Tuple tuple = q(AlarmLabelVO.class)
                            .select(AlarmLabelVO_.uuid, AlarmLabelVO_.value)
                            .eq(AlarmLabelVO_.key, ALARM_EXCLUDE_RESOURCE_LABEL)
                            .eq(AlarmLabelVO_.alarmUuid, alarmUuid)
                            .findTuple();
                    if (tuple != null) {
                        sql(AlarmLabelVO.class)
                                .eq(AlarmLabelVO_.uuid, tuple.get(0))
                                .eq(AlarmLabelVO_.key, ALARM_EXCLUDE_RESOURCE_LABEL)
                                .set(AlarmLabelVO_.value, Joiner.on(ALARM_EXCLUDE_RESOURCE_SEPARATOR)
                                        .join(excludeResourceUuids))
                                .update();
                    } else {
                        AlarmLabelVO labelVO = new AlarmLabelVO();
                        labelVO.setUuid(Platform.getUuid());
                        labelVO.setAlarmUuid(alarmUuid);
                        labelVO.setKey(ALARM_EXCLUDE_RESOURCE_LABEL);
                        labelVO.setOperator(Label.Operator.Equal);
                        labelVO.setValue(resourceUuid);
                        persist(labelVO);
                    }
                }
            }.execute();
        }

        void resetAllCount() {
            SQL.New(AlarmLabelVO.class)
                    .eq(AlarmLabelVO_.alarmUuid, alarmUuid)
                    .eq(AlarmLabelVO_.key, ALARM_EXCLUDE_RESOURCE_LABEL)
                    .delete();

            excludeResourceUuids.clear();
            repeatCountMap.clear();
        }
    }

    private ARule asRule(AlarmVO vo) {
        ARule rule = new ARule(vo.getUuid());
        rule.setComparisonOperator(vo.getComparisonOperator());
        rule.setLabels(vo.getLabels().stream()
                .filter(label -> !label.getKey().equals(ALARM_EXCLUDE_RESOURCE_LABEL))
                .map(labelVO -> {
            Label label = new Label(labelVO.getOperator().toString());
            label.setKey(labelVO.getKey());
            label.setValue(labelVO.getValue());

            return label;
        }).collect(Collectors.toList()));
        rule.setDuration(vo.getPeriod());
        rule.setMetricName(vo.getMetricName());
        rule.setNamespaceName(vo.getNamespace());
        rule.setThreshold(vo.getThreshold());
        rule.setAccountUuid(acntMgr.getOwnerAccountUuidOfResource(vo.getUuid()));

        // TODO: fix this hack set rule state problem if status is alarm
        if (vo.getStatus().equals(AlarmStatus.Alarm)) {
            rule.setState(RuleEvaluationResult.RuleEvaluationState.Problem);
        }

        rule.repeatInterval = vo.getRepeatInterval();
        rule.repeatCount = vo.getRepeatCount();
        rule.lastEvalTime = System.currentTimeMillis();

        final AlarmResourceRepeatCountChecker checker = new AlarmResourceRepeatCountChecker(vo.getUuid());

        Optional opt = vo.getLabels().stream().filter(label -> label.getKey().equals(ALARM_EXCLUDE_RESOURCE_LABEL)).findFirst();

        if (opt.isPresent()) {
            checker.excludeResourceUuids.addAll(Arrays.asList(((AlarmLabelVO) opt.get()).getValue().split("\\" + ALARM_EXCLUDE_RESOURCE_SEPARATOR)));
        }

        final int repeatCount = vo.getRepeatCount();
        final String identifyLabelName = Namespace.getMetricNameSpace(vo.getNamespace(), vo.getMetricName()).getIdentityLabelName();
        rule.listener = new RuleEvaluationResultListener() {
            @Override
            public boolean needReactToProblemState() {
                if (repeatCount != -1) {
                    return true;
                }

                // now the alarm runs into Problem state, we check if we need to fire the alarm
                // by repeat interval

                if (rule.lastProblemTime == 0) {
                    // the first alarm
                    rule.lastProblemTime = System.currentTimeMillis();
                    return true;
                }

                if (rule.lastProblemTime + TimeUnit.SECONDS.toMillis(rule.repeatInterval) <= System.currentTimeMillis()) {
                    rule.lastProblemTime = System.currentTimeMillis();
                    return true;
                }

                return false;
            }

            @Override
            public void executeActionsFromProblemResults(List<RuleEvaluationResult> results, Rule rule) {
                new While<>(results).step((result, completion) -> {
                    if (repeatCount != -1 && isExcludedResult(result)) {
                        completion.done();
                        return;
                    }

                    if (isInterceptAlarm(result)) {
                        completion.done();
                        return;
                    }

                    ExecuteAlarmActionsMsg msg = new ExecuteAlarmActionsMsg();
                    msg.setAlarmUuid(vo.getUuid());
                    msg.setCurrentStatus(toAlarmStatus(result.getCurrentState()));
                    msg.setPreviousStatus(toAlarmStatus(result.getLastState()));
                    msg.setCurrentValue(result.getCurrentValue());
                    msg.setIdentifyLabel(result.getIdentifyLabel());

                    bus.makeTargetServiceIdByResourceUuid(msg, AlarmConstants.SERVICE_ID, vo.getUuid());
                    bus.send(msg, new CloudBusCallBack(completion) {
                        @Override
                        public void run(MessageReply reply) {
                            if (!reply.isSuccess()) {
                                logger.debug(String.format("Failed to execute alarm %s", vo.getUuid()));
                            }
                        }
                    });
                    completion.done();
                }, 20).run(new NopeWhileDoneCompletion());
            }

            @Override
            public void executeActionsFromRecoveredResults(List<RuleEvaluationResult> results, Rule rule) {
                logger.debug("do recover");
                new While<>(results).step((result, completion) -> {
                    if (!result.getCurrentState().equals(RuleEvaluationResult.RuleEvaluationState.OK) ||
                        !result.getLastState().equals(RuleEvaluationResult.RuleEvaluationState.Problem) ||
                        !vo.isEnableRecovery()) {
                        completion.done();
                        return;
                    }

                    ExecuteAlarmActionsMsg msg = new ExecuteAlarmActionsMsg();
                    msg.setAlarmUuid(vo.getUuid());
                    msg.setCurrentStatus(toAlarmStatus(result.getCurrentState()));
                    msg.setPreviousStatus(toAlarmStatus(result.getLastState()));
                    msg.setCurrentValue(result.getCurrentValue());
                    msg.setIdentifyLabel(result.getIdentifyLabel());

                    bus.makeTargetServiceIdByResourceUuid(msg, AlarmConstants.SERVICE_ID, vo.getUuid());
                    bus.send(msg, new CloudBusCallBack(completion) {
                        @Override
                        public void run(MessageReply reply) {
                            if (!reply.isSuccess()) {
                                logger.debug(String.format("Failed to execute alarm %s", vo.getUuid()));
                            }
                        }
                    });
                    completion.done();
                }, 20).run(new NopeWhileDoneCompletion());
            }

            private boolean isExcludedResult(RuleEvaluationResult result) {
                String resourceUuid = getResourceUuid(result, identifyLabelName);

                // resource uuid is null means the metric do not
                // have any identity label defined in metric so
                // use its alarm uuid for exclude resource check
                if (resourceUuid == null) {
                    resourceUuid = checker.getAlarmUuid();
                }

                // if exclude resource uuid contains identify return true for just audit
                if (checker.isExcludeResource(resourceUuid)) {
                    return true;
                }

                int count = checker.incrementAndGet(resourceUuid);
                logger.debug(String.format("resource [uuid:%s] to exists, count is %d, max count is %d", resourceUuid, count, repeatCount));

                // stop count it and put it to exclude resource
                if (count >= repeatCount) {
                    checker.addToExcludeResource(resourceUuid);
                }

                return false;
            }

            private boolean isInterceptAlarm(RuleEvaluationResult result) {
                String resourceUuid = getResourceUuid(result, identifyLabelName);
                resourceUuid = resourceUuid == null ? vo.getUuid() : resourceUuid;
                AlarmDataAckVO ackVO = Q.New(AlarmDataAckVO.class)
                        .eq(AlarmDataAckVO_.alarmUuid, vo.getUuid())
                        .eq(AlarmDataAckVO_.resourceUuid, resourceUuid)
                        .orderBy(AlertDataAckVO_.ackDate, SimpleQuery.Od.DESC)
                        .limit(1)
                        .find();

                if (ackVO == null){
                    return false;
                }

                if (ackVO.isResumeAlert()) {
                    return false;
                }

                long mills = TimeUnit.SECONDS.toMillis(ackVO.getAckPeriod());
                long silentTime = ackVO.getAckDate().getTime() + mills;
                long currentTime = System.currentTimeMillis();
                if (silentTime > currentTime) {
                    logger.info(String.format("Alarm interception[alarmUuid=%s, resourceUuid=%s], intercept deadline %s", vo.getUuid(), resourceUuid, new Timestamp(silentTime)));
                    return true;
                }

                return false;
            }

            public void problemState(RuleEvaluationResult res, Rule rule) {
                ChangeAlarmStatusMsg msg = new ChangeAlarmStatusMsg();
                msg.setAlarmUuid(vo.getUuid());
                msg.setCurrentStatus(toAlarmStatus(res.getCurrentState()));
                msg.setPreviousStatus(toAlarmStatus(res.getLastState()));
                bus.makeTargetServiceIdByResourceUuid(msg, AlarmConstants.SERVICE_ID, vo.getUuid());
                bus.send(msg);
            }

            @Override
            public void stateChanged(RuleEvaluationResult res, Rule rule) {
                if (res.getCurrentState() == RuleEvaluationResult.RuleEvaluationState.Problem) {
                    // state Problem is handled by problemState()
                    return;
                } else {
                    checker.resetAllCount();
                }

                ChangeAlarmStatusMsg msg = new ChangeAlarmStatusMsg();
                msg.setAlarmUuid(vo.getUuid());
                msg.setCurrentStatus(toAlarmStatus(res.getCurrentState()));
                msg.setPreviousStatus(toAlarmStatus(res.getLastState()));
                bus.makeTargetServiceIdByResourceUuid(msg, AlarmConstants.SERVICE_ID, vo.getUuid());
                bus.send(msg);
            }
        };

        return (ARule) alarmFactories.get(vo.getType().toString()).asRule(rule);
    }

    private AlarmStatus toAlarmStatus(RuleEvaluationResult.RuleEvaluationState state) {
        if (state == RuleEvaluationResult.RuleEvaluationState.OK) {
            return AlarmStatus.OK;
        } else if (state == RuleEvaluationResult.RuleEvaluationState.Problem) {
            return AlarmStatus.Alarm;
        } else if (state == RuleEvaluationResult.RuleEvaluationState.NoData) {
            return AlarmStatus.InsufficientData;
        } else {
            throw new CloudRuntimeException(String.format("unknown state[%s]", state));
        }
    }

    private void handle(CreateAlarmMsg msg) {
        CreateAlarmReply msgReply = new CreateAlarmReply();

        AlarmVO vo = new AlarmVO();
        vo.setUuid(msg.getUuid() == null ? Platform.getUuid() : msg.getUuid());
        vo.setName(msg.getName());
        vo.setDescription(msg.getDescription());
        vo.setComparisonOperator(msg.getComparisonOperator());
        vo.setMetricName(msg.getMetricName());
        vo.setNamespace(msg.getNamespace());
        vo.setPeriod(msg.getPeriod());
        vo.setStatus(AlarmStatus.OK);
        vo.setThreshold(msg.getThreshold());
        vo.setState(AlarmState.Enabled);
        vo.setRepeatInterval(msg.getRepeatInterval() == null ? ZWatchGlobalConfig.ALARM_REPEAT_INTERVAL.value(Integer.class) : msg.getRepeatInterval());
        vo.setAccountUuid(msg.getAccountUuid());
        vo.setType(AlarmType.valueOf(msg.getType()));
        vo.setEnableRecovery(msg.getEnableRecovery());
        vo.setRepeatCount(msg.getRepeatCount());
        if (msg.getEmergencyLevel() != null) {
            vo.setEmergencyLevel(EmergencyLevel.valueOf(msg.getEmergencyLevel()));
        }

        new SQLBatch() {
            @Override
            protected void scripts() {
                persist(vo);
                flush();

                if (msg.getLabels() != null) {
                    msg.getLabels().forEach(l -> {
                        AlarmLabelVO lvo = new AlarmLabelVO();
                        lvo.setUuid(Platform.getUuid());
                        lvo.setAlarmUuid(vo.getUuid());
                        lvo.setKey(l.getKey());
                        lvo.setValue(l.getValue());
                        lvo.setOperator(l.getOp());
                        persist(lvo);
                    });
                }

                if (msg.getActions() != null) {
                    msg.getActions().forEach(a -> {
                        AlarmActionVO avo = new AlarmActionVO();
                        avo.setAlarmUuid(vo.getUuid());
                        avo.setActionType(a.actionType);
                        avo.setActionUuid(a.actionUuid);
                        persist(avo);
                    });
                }

                String textToken = ACTIVE_ALARM_CN_SYSTEM_TAG.containsKey(vo.getName())
                        ? ACTIVE_ALARM_CN_SYSTEM_TAG.get(vo.getName())
                        : vo.getName();
                persistSystemTagOfLanguage(vo,
                        EventSubscriptionSystemTags.CN,
                        textToken);

                reload(vo);
            }
        }.execute();

        LoadAlarmRuleMsg lmsg = new LoadAlarmRuleMsg();
        lmsg.setAlarmUuid(vo.getUuid());
        bus.makeTargetServiceIdByResourceUuid(lmsg, AlarmConstants.SERVICE_ID, lmsg.getAlarmUuid());
        bus.send(lmsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    dbf.removeByPrimaryKey(vo.getUuid(), AlarmVO.class);
                    msgReply.setError(reply.getError());
                    bus.reply(msg, msgReply);
                    return;
                }

                LoadAlarmRuleReply rly = (LoadAlarmRuleReply) reply;
                msgReply.setInventory(AlarmInventory.valueOf(vo));
                bus.reply(msg, msgReply);
            }
        });
    }

    private void handle(APICreateAlarmMsg msg) {
        APICreateAlarmEvent evt = new APICreateAlarmEvent(msg.getId());

        CreateAlarmMsg innerMsg = new CreateAlarmMsg();
        innerMsg.setUuid(msg.getResourceUuid());
        innerMsg.setName(msg.getName());
        innerMsg.setDescription(msg.getDescription());
        innerMsg.setAccountUuid(msg.getSession().getAccountUuid());
        innerMsg.setActions(msg.getActions());
        innerMsg.setComparisonOperator(msg.getComparisonOperator());
        innerMsg.setPeriod(msg.getPeriod());
        innerMsg.setEmergencyLevel(msg.getEmergencyLevel());
        innerMsg.setNamespace(msg.getNamespace());
        innerMsg.setMetricName(msg.getMetricName());
        innerMsg.setThreshold(msg.getThreshold());
        innerMsg.setRepeatInterval(msg.getRepeatInterval());
        innerMsg.setRepeatCount(msg.getRepeatCount());
        innerMsg.setLabels(msg.getLabels());
        innerMsg.setType(msg.getType());
        innerMsg.setEnableRecovery(msg.isEnableRecovery());
        bus.makeLocalServiceId(innerMsg, AlarmConstants.SERVICE_ID);

        bus.send(innerMsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    evt.setError(reply.getError());
                    bus.publish(evt);
                    return;
                }

                CreateAlarmReply rly = (CreateAlarmReply) reply;
                evt.setInventory(rly.getInventory());
                bus.publish(evt);
            }
        });
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(AlarmConstants.SERVICE_ID);
    }

    @Override
    public boolean start() {
        populateExtensions();
        return true;
    }

    @Override
    public AlarmActionFactory getAlarmActionFactory(String type) {
        AlarmActionFactory f = alarmActionFactories.get(type);
        if (f == null) {
            throw new CloudRuntimeException(String.format("cannot find AlarmActionFactory with type[%s]", type));
        }
        return f;
    }

    @Override
    public void loadAlarms(List<String> alarmUuids) {
        List<AlarmVO> vos = dbf.listByPrimaryKeys(alarmUuids, AlarmVO.class);
        vos.forEach(vo->ruleMgr.addRule(asRule(vo)));
    }

    @Override
    public void loadAlarms(String alarmUuid) {
        AlarmVO vo = dbf.findByUuid(alarmUuid, AlarmVO.class);
        ruleMgr.addRule(asRule(vo));
    }

    @Override
    public void loadEventSubscription(String subscriptionUuid) {
        EventSubscriptionVO vo = dbf.findByUuid(subscriptionUuid, EventSubscriptionVO.class);
        if (vo.getState().equals(EventSubscriptionState.Enabled)) {
            subscribeEvent(EventSubscriptionInventory.valueOf(vo));
        }
    }

    private void populateExtensions() {
        pluginRgty.getExtensionList(AlarmActionFactory.class).forEach(f -> {
            AlarmActionFactory old = alarmActionFactories.get(f.getActionType());
            if (old != null) {
                throw new CloudRuntimeException(String.format("duplicate AlarmActionFactory[%s, %s] with the same type[%s]", old, f, f.getActionType()));
            }
            alarmActionFactories.put(f.getActionType(), f);
        });

        pluginRgty.getExtensionList(AlarmFactory.class).forEach(f -> {
            AlarmActionFactory old = alarmActionFactories.get(f.getAlarmType());
            if (old != null) {
                throw new CloudRuntimeException(String.format("duplicate AlarmFactory[%s, %s] with the same type[%s]", old, f, f.getAlarmType()));
            }
            alarmFactories.put(f.getAlarmType(), f);
        });
    }

    @Override
    public boolean stop() {
        return true;
    }
}
