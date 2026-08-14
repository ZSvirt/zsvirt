package org.zstack.monitoring;

import com.google.common.util.concurrent.Striped;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.compute.host.PostHostConnectExtensionPoint;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.cloudbus.ResourceDestinationMaker;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigUpdateExtensionPoint;
import org.zstack.core.db.*;
import org.zstack.core.defer.Defer;
import org.zstack.core.defer.Deferred;
import org.zstack.core.thread.PeriodicTask;
import org.zstack.core.thread.SyncTask;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.header.AbstractService;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NopeCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.host.HostDeleteExtensionPoint;
import org.zstack.header.host.HostException;
import org.zstack.header.host.HostInventory;
import org.zstack.header.managementnode.ManagementNodeReadyExtensionPoint;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmJustBeforeDeleteFromDbExtensionPoint;
import org.zstack.header.vo.ResourceVO;
import org.zstack.header.vo.ResourceVO_;
import org.zstack.identity.AccountManager;
import org.zstack.monitoring.actions.*;
import org.zstack.monitoring.items.Item;
import org.zstack.monitoring.items.ItemInventory;
import org.zstack.monitoring.targets.HostTarget;
import org.zstack.monitoring.targets.VmTarget;
import org.zstack.monitoring.trigger.expression.TriggerExpression;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.ForEachFunction;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.argerr;
import static org.zstack.core.Platform.operr;

/**
 * Created by xing5 on 2017/6/3.
 */
public class MonitorManagerImpl extends AbstractService implements
        MonitorManager, PostHostConnectExtensionPoint, HostDeleteExtensionPoint,
        ManagementNodeReadyExtensionPoint, VmJustBeforeDeleteFromDbExtensionPoint {
    private static final CLogger logger = Utils.getLogger(MonitorManagerImpl.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private AccountManager acntMgr;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ThreadFacade thdf;
    @Autowired
    private ResourceDestinationMaker destMaker;

    private Map<String, MonitorProviderFactory> monitorFactories = new HashMap<>();
    private Map<String, Map<String, Item>> itemsByResourceType = new HashMap<>();
    private Map<String, Item> itemsByNames = new HashMap<>();
    private Future<Void> triggerRecoveryCheckerThread;
    private Map<String, MonitorTriggerActionFactory> monitorActionFactories = new HashMap<>();

    @Override
    public boolean start() {
        try {
            populateExtensions();
            groupItemsByResourceType();
            configureGlobalConfig();
        } catch (Exception e)  {
            throw new CloudRuntimeException(e);
        }

        return true;
    }

    private void configureGlobalConfig() {
        MonitorGlobalConfig.TRIGGER_RECOVERY_CHECKER_INTERVAL.installUpdateExtension(new GlobalConfigUpdateExtensionPoint() {
            @Override
            public void updateGlobalConfig(GlobalConfig oldConfig, GlobalConfig newConfig) {
                startRecoveryChecker();
            }
        });
    }

    private Item getItemByResourceTypeAndItemName(String resourceType, String key) {
        Map<String, Item> m = itemsByResourceType.get(resourceType);
        if (m == null) {
            return null;
        }

        return m.get(key);
    }

    private void groupItemsByResourceType() throws IllegalAccessException, InstantiationException {
        Set itemClasses = Platform.getReflections().getSubTypesOf(Item.class).stream()
                .filter(clz -> !Modifier.isAbstract(clz.getModifiers())).collect(Collectors.toSet());

        for (Object o : itemClasses) {
            Class clz = (Class) o;
            Item item = (Item) clz.newInstance();

            Map<String, Item> m = itemsByResourceType.get(item.getResourceType());
            if (m == null) {
                m = new HashMap<>();
                itemsByResourceType.put(item.getResourceType(), m);
            }

            Item old = m.get(item.getName());
            if (old != null) {
                throw new CloudRuntimeException(String.format("duplicate Item[%s,%s] for the same name[%s]", item.getClass(), old.getClass(), item.getName()));
            }

            m.put(item.getName(), item);

            old = itemsByNames.get(item.getName());
            if (old != null) {
                throw new CloudRuntimeException(String.format("duplicate Item[%s,%s] for the same name[%s]", item.getClass(), old.getClass(), item.getName()));
            }
            itemsByNames.put(item.getName(), item);
        }
    }

    private void populateExtensions() {
        for (MonitorProviderFactory f : pluginRgty.getExtensionList(MonitorProviderFactory.class)) {
            MonitorProviderFactory old = monitorFactories.get(f.getMonitorProviderType());
            if (old != null) {
                throw new CloudRuntimeException(String.format("duplicated MonitorProviderFactory[%s, %s] with" +
                        " the same provider type[%s]", f.getClass(), old.getClass(), f.getMonitorProviderType()));
            }

            monitorFactories.put(f.getMonitorProviderType(), f);
        }

        for (MonitorTriggerActionFactory f : pluginRgty.getExtensionList(MonitorTriggerActionFactory.class)) {
            MonitorTriggerActionFactory old = monitorActionFactories.get(f.getMonitorActionType());
            if (old != null) {
                throw new CloudRuntimeException(String.format("duplicated, MonitorActionFactory[%s, %s] with the" +
                        " same type[%s]", f.getClass(), old.getClass(), f.getMonitorActionType()));
            }

            monitorActionFactories.put(f.getMonitorActionType(), f);
        }
    }

    @Override
    public MonitorTriggerActionFactory getMonitorActionFactory(String type) {
        MonitorTriggerActionFactory f = monitorActionFactories.get(type);
        DebugUtils.Assert(f != null, String.format("cannot find MonitorActionFactory with the type[%s]", type));
        return f;
    }

    @Override
    public MonitorProviderFactory getMonitorProviderFactory(String type) {
        MonitorProviderFactory f = monitorFactories.get(type);
        DebugUtils.Assert(f != null, String.format("cannot find MonitorProviderFactory with the type[%s]", type));
        return f;
    }

    @Override
    public boolean stop() {
        return true;
    }

    private void passThroughTriggerMessages(MonitorTriggerMessage msg) {
        MonitorTriggerVO vo = dbf.findByUuid(msg.getMonitorTriggerUuid(), MonitorTriggerVO.class);
        if (vo == null) {
            throw new OperationFailureException(operr("cannot find monitor trigger[uuid:%s], it may have been deleted", msg.getMonitorTriggerUuid()));
        }

        new MonitorTriggerBase(vo).handleMessage((Message) msg);
    }

    private void passThroughActionMessages(MonitorTriggerActionMessage msg) {
        MonitorTriggerActionVO vo = dbf.findByUuid(msg.getMonitorTriggerActionUuid(), MonitorTriggerActionVO.class);
        if (vo == null) {
            throw new OperationFailureException(operr("cannot find monitor trigger action[uuid:%s], it may have been deleted", msg.getMonitorTriggerActionUuid()));
        }

        new MonitorTriggerActionBase(vo).handleMessage((Message) msg);
    }

    @Override
    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof MonitorTriggerMessage) {
            passThroughTriggerMessages((MonitorTriggerMessage) msg);
        } else if (msg instanceof MonitorTriggerActionMessage) {
            passThroughActionMessages((MonitorTriggerActionMessage) msg);
        } else if (msg instanceof APIMessage) {
            handleApiMessage(msg);
        } else {
            handleLocalMessage(msg);
        }
    }

    private void handleLocalMessage(Message msg) {
        bus.dealWithUnknownMessage(msg);
    }

    private void handleApiMessage(Message msg) {
        if (msg instanceof APICreateMonitorTriggerMsg) {
            handle((APICreateMonitorTriggerMsg) msg);
        } else if (msg instanceof APICreateMonitorTriggerActionMsg) {
            handle((APICreateMonitorTriggerActionMsg) msg);
        } else if(msg instanceof APIUpdateEmailMonitorTriggerActionMsg) {
            handle((APIUpdateEmailMonitorTriggerActionMsg) msg);
        } else if (msg instanceof APIDeleteAlertMsg) {
            handle((APIDeleteAlertMsg) msg);
        } else if (msg instanceof APIGetMonitorItemMsg) {
            handle((APIGetMonitorItemMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(APIGetMonitorItemMsg msg) {
        APIGetMonitorItemReply reply = new APIGetMonitorItemReply();
        Map<String, Item> items = itemsByResourceType.get(msg.getResourceType());
        if (items == null) {
            throw new OperationFailureException(argerr("the resource[type:%s] doesn't have any monitoring items", msg.getResourceType()));
        }

        List<ItemInventory> invs = new ArrayList<>();
        for (Item item : items.values()) {
            invs.add(new ItemInventory(item));
        }
        reply.setInventories(invs);
        bus.reply(msg, reply);
    }

    private void handle(APIDeleteAlertMsg msg) {
        SQL.New(AlertVO.class).in(AlertVO_.uuid, msg.getUuids()).hardDelete();

        APIDeleteAlertEvent evt = new APIDeleteAlertEvent(msg.getId());
        bus.publish(evt);
    }

    private void handle(APIUpdateEmailMonitorTriggerActionMsg msg){
        APIUpdateMonitorTriggerActionEvent evt = new APIUpdateMonitorTriggerActionEvent(msg.getId());

        EmailTriggerActionVO vo = dbf.findByUuid(msg.getUuid(), EmailTriggerActionVO.class);

        if (msg.getName() != null) {
            vo.setName(msg.getName());
        }
        if (msg.getMediaUuid() != null){
            vo.setMediaUuid(msg.getMediaUuid());
        }
        if (msg.getEmail() != null){
            vo.setEmail(msg.getEmail());
        }
        if (msg.getDescription() != null) {
            vo.setDescription(msg.getDescription());
        }
        vo = dbf.updateAndRefresh(vo);

        evt.setInventory(EmailTriggerActionInventory.valueOf(vo));
        bus.publish(evt);
    }

    private void handle(APICreateMonitorTriggerActionMsg msg) {
        APICreateMonitorTriggerActionEvent evt = new APICreateMonitorTriggerActionEvent(msg.getId());

        MonitorTriggerActionFactory f = getMonitorActionFactory(msg.getType());

        final MonitorTriggerActionVO vo = new SQLBatchWithReturn<MonitorTriggerActionVO>() {
            @Override
            protected MonitorTriggerActionVO scripts() {
                MonitorTriggerActionVO vo = new MonitorTriggerActionVO();
                vo.setUuid(msg.getResourceUuid() == null ? Platform.getUuid() : msg.getResourceUuid());
                vo.setName(msg.getName());
                vo.setDescription(vo.getDescription());
                vo.setState(MonitorTriggerActionState.Enabled);
                vo.setType(f.getMonitorActionType());
                vo.setAccountUuid(msg.getSession().getAccountUuid());

                vo = f.createMonitorTriggerAction(vo, msg);

                persist(vo);
                flush();

                if (msg.getTriggerUuids() != null) {
                    for (String triggerUuid : msg.getTriggerUuids()) {
                        MonitorTriggerActionRefVO ref = new MonitorTriggerActionRefVO();
                        ref.setTriggerUuid(triggerUuid);
                        ref.setActionUuid(vo.getUuid());
                        persist(ref);
                    }
                }

                return reload(vo);
            }
        }.execute();

        CollectionUtils.safeForEach(pluginRgty.getExtensionList(MonitorTriggerActionExtension.class),
                new ForEachFunction<MonitorTriggerActionExtension>() {
                    @Override
                    public void run(MonitorTriggerActionExtension arg) {
                        arg.afterCreateMonitorTriggerAction(vo);
                    }
                });

        evt.setInventory(f.getInventory(vo.getUuid()));
        bus.publish(evt);
    }

    @Deferred
    private void handle(APICreateMonitorTriggerMsg msg) {
        if (!acntMgr.isAdmin(msg.getSession())) {
            String ownerUuid = acntMgr.getOwnerAccountUuidOfResource(msg.getTargetResourceUuid());
            if (ownerUuid != null && !msg.getSession().getAccountUuid().equals(ownerUuid)) {
                throw new OperationFailureException(operr("the resource[uuid:%s] doesn't belong to the account[uuid:%s]", msg.getTargetResourceUuid(), msg.getSession().getAccountUuid()));
            }
        }


        MonitorTriggerVO vo = new MonitorTriggerVO();
        vo.setUuid(msg.getResourceUuid() == null ? Platform.getUuid() : msg.getResourceUuid());
        vo.setName(msg.getName());
        vo.setDescription(msg.getDescription());
        vo.setExpression(msg.getExpression());
        vo.setRecoveryExpression(msg.getRecoveryExpression());
        vo.setTargetResourceUuid(msg.getTargetResourceUuid());
        vo.setDuration(msg.getDuration());
        vo.setState(MonitorTriggerState.Enabled);
        vo.setStatus(MonitorTriggerStatus.OK);
        vo.setAccountUuid(msg.getSession().getAccountUuid());

        new SQLBatch() {
            @Override
            protected void scripts() {
                persist(vo);
                reload(vo);
            }
        }.execute();

        Defer.guard(() -> dbf.remove(vo));

        APICreateMonitorTriggerEvent evt = new APICreateMonitorTriggerEvent(msg.getId());
        MonitorTriggerInventory inv = MonitorTriggerInventory.valueOf(vo);
        createTrigger(inv, new Completion(msg) {
            @Override
            public void success() {
                evt.setInventory(inv);
                bus.publish(evt);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                dbf.remove(vo);
                evt.setError(errorCode);
                bus.publish(evt);
            }
        });

    }

    @Override
    public Item getItem(String resourceUuid, TriggerExpression triggerExpression) {
        String resourceType = Q.New(ResourceVO.class).select(ResourceVO_.resourceType)
                .eq(ResourceVO_.uuid, resourceUuid).findValue();
        if (resourceType == null) {
            throw new OperationFailureException(argerr("cannot find type for the resource[uuid:%s]", resourceUuid));
        }

        Item item = getItemByResourceTypeAndItemName(Platform.getBaseResourceType(resourceType), triggerExpression.getItem());
        if (item == null) {
            throw new OperationFailureException(argerr("no monitoring item found for the resourceType[%s] and item[%s]", resourceType, triggerExpression.getItem()));
        }

        return item;
    }

    @Override
    public Item getItemByExpressionString(String expressionString) {
        TriggerExpression expression = TriggerExpression.expressionFromString(expressionString);
        Item ret = itemsByNames.get(expression.getItem());
        if (ret == null) {
            throw new CloudRuntimeException(String.format("cannot find item[%s]", expression.getItem()));
        }

        return ret;
    }

    private void createTrigger(MonitorTriggerInventory inv, Completion completion) {
        TriggerExpression triggerExpression = TriggerExpression.expressionFromString(inv.getExpression());
        Item item = getItem(inv.getTargetResourceUuid(), triggerExpression);
        MonitorProviderFactory f = getMonitorProviderFactory(item.getProvider());
        MonitorProvider provider = f.createProvider();
        provider.createTrigger(inv, item, triggerExpression, completion);
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(MonitorConstants.SERVICE_ID);
    }

    @Override
    public Flow createPostHostConnectFlow(HostInventory host) {
        return new NoRollbackFlow() {
            String __name__ = "setup-monitor";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                HostTarget template = new HostTarget(host);

                FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
                chain.setName(String.format("setup-monitor-for-host-%s", host.getUuid()));
                chain.allowEmptyFlow();

                for (String providerType : template.getProviderTypes()) {
                    MonitorProviderFactory f = getMonitorProviderFactory(providerType);
                    chain.then(new NoRollbackFlow() {
                        String __name__ = String.format("call-provider-%s", providerType);

                        @Override
                        public void run(FlowTrigger t, Map data) {
                            MonitorProvider provider = f.createProvider();
                            provider.setupMonitor(template, new Completion(t) {
                                @Override
                                public void success() {
                                    t.next();
                                }

                                @Override
                                public void fail(ErrorCode errorCode) {
                                    t.fail(errorCode);
                                }
                            });
                        }
                    });
                }

                chain.done(new FlowDoneHandler(trigger) {
                    @Override
                    public void handle(Map data) {
                        trigger.next();
                    }
                }).error(new FlowErrorHandler(trigger) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        trigger.fail(errCode);
                    }
                }).start();
            }
        };
    }

    @Override
    public void preDeleteHost(HostInventory inventory) throws HostException {

    }

    @Override
    public void beforeDeleteHost(HostInventory inventory) {
    }

    @Override
    public void afterDeleteHost(HostInventory host) {
        HostTarget template = new HostTarget(host);
        for (String providerType : template.getProviderTypes()) {
            MonitorProviderFactory f = getMonitorProviderFactory(providerType);
            MonitorProvider provider = f.createProvider();
            provider.deleteMonitor(template, new Completion(null) {
                @Override
                public void success() {
                    logger.debug(String.format("successfully delete monitoring stuff for the host[name:%s, uuid:%s]",
                            host.getName(), host.getUuid()));
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    logger.warn(String.format("failed to delete monitoring stuff for the host[name:%s, uuid:%s], %s",
                            host.getName(), host.getUuid(), errorCode));
                }
            });
        }
    }

    private synchronized void startRecoveryChecker() {
        if (triggerRecoveryCheckerThread != null) {
            triggerRecoveryCheckerThread.cancel(true);
        }

        triggerRecoveryCheckerThread = thdf.submitPeriodicTask(new PeriodicTask() {
            @Override
            public TimeUnit getTimeUnit() {
                return TimeUnit.SECONDS;
            }

            @Override
            public long getInterval() {
                return MonitorGlobalConfig.TRIGGER_RECOVERY_CHECKER_INTERVAL.value(Long.class);
            }

            @Override
            public String getName() {
                return "monitoring-trigger-recovery-checker";
            }

            @Override
            public void run() {
                long total = Q.New(MonitorTriggerVO.class).eq(MonitorTriggerVO_.status, MonitorTriggerStatus.Problem).count();
                int step = 1000;
                int times = (int) (total / step) + (total % step != 0 ? 1 : 0);
                int start = 0;

                for (int i=0; i<times; i++) {
                    List<MonitorTriggerVO> triggers = Q.New(MonitorTriggerVO.class)
                            .eq(MonitorTriggerVO_.status, MonitorTriggerStatus.Problem)
                            .limit(step).start(start).list();
                    start += step;
                    checkTriggerRecovery(triggers);
                }
            }
        });
    }

    private static Striped<Lock> recoveryLocks = Striped.lock(100);

    @Override
    public void vmJustBeforeDeleteFromDb(VmInstanceInventory vm) {
        VmTarget target = new VmTarget(vm);
        for (String providerType : target.getProviderTypes()) {
            MonitorProviderFactory f = getMonitorProviderFactory(providerType);
            MonitorProvider provider = f.createProvider();
            provider.deleteMonitor(target, new Completion(null) {
                @Override
                public void success() {
                    logger.debug(String.format("successfully delete monitoring stuff for the vm[name:%s, uuid:%s]",
                            vm.getName(), vm.getUuid()));
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    logger.warn(String.format("failed to delete monitoring stuff for the vm[name:%s, uuid:%s], %s",
                            vm.getName(), vm.getUuid(), errorCode));
                }
            });
        }
    }

    class RecoveryChecker {
        MonitorTriggerVO trigger;

        RecoveryChecker(MonitorTriggerVO trigger) {
            this.trigger = trigger;
        }

        @Deferred
        void check() {
            Lock lock = recoveryLocks.get(trigger.getUuid());
            if (!lock.tryLock()) {
                logger.debug(String.format("monitor trigger[uuid:%s, name:%s] is still being checked, skip this check",
                        trigger.getUuid(), trigger.getName()));
                return;
            }
            Defer.defer(lock::unlock);

            TriggerExpression expression = TriggerExpression.expressionFromString(trigger.getExpression());
            Item item = itemsByNames.get(expression.getItem());
            if (item.checkIfTriggerRecovered(MonitorTriggerInventory.valueOf(trigger), expression) && trigger.getStatus() == MonitorTriggerStatus.Problem) {
                ChangeMonitorTriggerStatusMsg msg = new ChangeMonitorTriggerStatusMsg();
                msg.setUuid(trigger.getUuid());
                msg.setStatus(MonitorTriggerStatus.OK);
                msg.setContext(JSONObjectUtil.fromTypedJsonString(trigger.getContextData()));
                bus.makeTargetServiceIdByResourceUuid(msg, MonitorConstants.SERVICE_ID, trigger.getUuid());
                bus.send(msg);
            }
        }
    }


    private void checkTriggerRecovery(List<MonitorTriggerVO> triggers) {
        for (MonitorTriggerVO trigger : triggers) {
            if (!destMaker.isManagedByUs(trigger.getUuid())) {
                continue;
            }

            thdf.syncSubmit(new SyncTask<Void>() {
                @Override
                public Void call() throws Exception {
                    new RecoveryChecker(trigger).check();
                    return null;
                }

                @Override
                public String getName() {
                    return "trigger-recovery-checker";
                }

                @Override
                public String getSyncSignature() {
                    return getName();
                }

                @Override
                public int getSyncLevel() {
                    return 5;
                }
            });
        }
    }


    @Override
    public void managementNodeReady() {
        //startRecoveryChecker();
        loadTriggers();
    }

    private void loadTriggers() {
        List<MonitorTriggerVO> vos =  Q.New(MonitorTriggerVO.class).list();
        vos.forEach(vo -> {
            if (destMaker.isManagedByUs(vo.getUuid())) {
                createTrigger(MonitorTriggerInventory.valueOf(vo), new NopeCompletion());
            }
        });
    }
}
