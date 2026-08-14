package org.zstack.zwatch.monitorgroup;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.Platform;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.db.*;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.header.core.Completion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.host.HostVO;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.vo.ResourceVO;
import org.zstack.header.vo.ResourceVO_;
import org.zstack.header.vpc.VpcRouterVmVO;
import org.zstack.identity.AccountManager;
import org.zstack.kvm.KVMHostVO;
import org.zstack.network.service.virtualrouter.VirtualRouterVmVO;
import org.zstack.utils.ObjectUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;
import org.zstack.xdragon.XDragonHostVO;
import org.zstack.zwatch.ZWatchGlobalConfig;
import org.zstack.zwatch.alarm.*;
import org.zstack.zwatch.datatype.EventData;
import org.zstack.zwatch.datatype.Label;
import org.zstack.zwatch.datatype.Namespace;
import org.zstack.zwatch.datatype.metric.Metric;
import org.zstack.zwatch.message.AlarmDeletionMsg;
import org.zstack.zwatch.message.EventSubscriptionDeletionMsg;
import org.zstack.zwatch.message.UpdateEventSubscriptionLabelMsg;
import org.zstack.zwatch.monitorgroup.api.*;
import org.zstack.zwatch.monitorgroup.entity.*;
import org.zstack.zwatch.monitorgroup.msg.CleanupMonitorGroupInvalidInstanceMsg;
import org.zstack.zwatch.monitorgroup.msg.DeleteMonitorGroupAlarmMsg;
import org.zstack.zwatch.monitorgroup.msg.DeleteMonitorGroupEventSubscriptionMsg;
import org.zstack.zwatch.monitorgroup.msg.RevokeMonitorTemplateFromMonitorGroupMsg;
import org.zstack.zwatch.namespace.KVMHostNamespace;
import org.zstack.zwatch.namespace.XDragonHostNamespace;
import org.zstack.zwatch.utils.ResourceVOToNamespaceMappingUtils;

import javax.persistence.Tuple;
import java.sql.Timestamp;
import java.util.*;

import static org.zstack.core.Platform.argerr;
import static org.zstack.zwatch.utils.ResourceVOToNamespaceMappingUtils.getNamespaceName;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class MonitorGroupBase {
    protected static final CLogger logger = Utils.getLogger(MonitorGroupBase.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ThreadFacade thdf;
    @Autowired
    private ErrorFacade errf;
    @Autowired
    protected EventFacade evtf;
    @Autowired
    protected AccountManager accountManager;

    protected MonitorGroupVO self;
    protected MonitorGroupVO originalCopy;
    private String syncThreadName;

    private static final Map<String, String> concreteResourceTypeMap = new HashMap<>();
    static {
        concreteResourceTypeMap.put(VpcRouterVmVO.class.getName(), VirtualRouterVmVO.class.getSimpleName());
        concreteResourceTypeMap.put(VirtualRouterVmVO.class.getName(), VirtualRouterVmVO.class.getSimpleName());
        concreteResourceTypeMap.put(KVMHostVO.class.getName(), KVMHostVO.class.getSimpleName());
        concreteResourceTypeMap.put(XDragonHostVO.class.getName(), XDragonHostVO.class.getSimpleName());
    }

    protected MonitorGroupVO getSelf() {
        return self;
    }

    protected MonitorGroupInventory getSelfInventory() {
        return MonitorGroupInventory.valueOf(self);
    }

    public MonitorGroupBase() {
    }

    public MonitorGroupBase(MonitorGroupVO vo) {
        this.self = vo;
        this.syncThreadName = "monitorGroup-" + vo.getUuid();
        this.originalCopy = ObjectUtils.newAndCopy(vo, vo.getClass());
    }

    @MessageSafe
    public void handleMessage(final Message msg) {
        if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage) msg);
        } else {
            handleLocalMessage(msg);
        }
    }

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APIUpdateMonitorGroupMsg) {
            handle((APIUpdateMonitorGroupMsg) msg);
        } else if (msg instanceof APIAddInstanceToMonitorGroupMsg) {
            handle((APIAddInstanceToMonitorGroupMsg) msg);
        } else if (msg instanceof APIRemoveInstanceFromMonitorGroupMsg) {
            handle((APIRemoveInstanceFromMonitorGroupMsg) msg);
        } else if (msg instanceof APIDeleteMonitorGroupMsg) {
            handle((APIDeleteMonitorGroupMsg) msg);
        } else if (msg instanceof APIApplyMonitorTemplateToMonitorGroupMsg) {
            handle((APIApplyMonitorTemplateToMonitorGroupMsg) msg);
        } else if (msg instanceof APIRevokeMonitorTemplateFromMonitorGroupMsg) {
            handle((APIRevokeMonitorTemplateFromMonitorGroupMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handleLocalMessage(Message msg) {
        if (msg instanceof RevokeMonitorTemplateFromMonitorGroupMsg) {
            handle((RevokeMonitorTemplateFromMonitorGroupMsg) msg);
        } else if (msg instanceof DeleteMonitorGroupAlarmMsg) {
            handle((DeleteMonitorGroupAlarmMsg) msg);
        } else if (msg instanceof DeleteMonitorGroupEventSubscriptionMsg) {
            handle((DeleteMonitorGroupEventSubscriptionMsg) msg);
        } else if (msg instanceof CleanupMonitorGroupInvalidInstanceMsg) {
            handle((CleanupMonitorGroupInvalidInstanceMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(APIUpdateMonitorGroupMsg msg) {
        APIUpdateMonitorGroupEvent event = new APIUpdateMonitorGroupEvent(msg.getId());

        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return syncThreadName;
            }

            @Override
            public void run(SyncTaskChain chain) {
                MonitorGroupVO vo = dbf.findByUuid(msg.getUuid(), MonitorGroupVO.class);

                if (msg.getName() != null) {
                    vo.setName(msg.getName());
                }

                if (msg.getDescription() != null) {
                    vo.setDescription(msg.getDescription());
                }

                String stateEvent = msg.getStateEvent();
                if (stateEvent != null) {
                    if (stateEvent.equals("enable")) {
                        vo.setState(MonitorGroupState.Enabled);
                    } else {
                        vo.setState(MonitorGroupState.Disabled);
                    }
                }

                if (msg.getDescription() != null) {
                    vo.setDescription(msg.getDescription());
                }

                vo = dbf.updateAndRefresh(vo);

                if (msg.getActions() == null) {
                    event.setInventory(MonitorGroupInventory.valueOf(vo));
                    bus.publish(event);
                    chain.next();
                    return;
                }

                updateAlarmAndEventAction(msg.getActions(), new Completion(chain,msg) {
                    @Override
                    public void success() {
                        MonitorGroupVO vo = dbf.findByUuid(msg.getUuid(), MonitorGroupVO.class);
                        vo.setActions(JSONObjectUtil.toJsonString(msg.getActions()));
                        dbf.updateAndRefresh(vo);
                        event.setInventory(MonitorGroupInventory.valueOf(vo));
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
                return String.format("update-monitor-group-%s", msg.getUuid());
            }
        });
    }

    private void updateAlarmAndEventAction(List<APICreateAlarmMsg.ActionParam> paramList, Completion completion){
        String monitorGroupUuid = self.getUuid();

        //MonitorGroupAlarmUuid
        List<String> alarmUuids = Q.New(MonitorGroupAlarmVO.class)
                .select(MonitorGroupAlarmVO_.alarmUuid)
                .eq(MonitorGroupAlarmVO_.groupUuid, monitorGroupUuid)
                .listValues();

        List<String> eventSubscriptionUuids = Q.New(MonitorGroupEventSubscriptionVO.class)
                .select(MonitorGroupEventSubscriptionVO_.eventSubscriptionUuid)
                .eq(MonitorGroupEventSubscriptionVO_.groupUuid,monitorGroupUuid)
                .listValues();

        if (alarmUuids.isEmpty() && eventSubscriptionUuids.isEmpty()) {
            completion.success();
            return;
        }
        FlowChain fchain = FlowChainBuilder.newSimpleFlowChain();
        fchain.setName((String.format("update-alarm-action-from-monitor-group-%s", monitorGroupUuid)));
        fchain.then(new NoRollbackFlow() {
            @Override
            public void run(FlowTrigger trigger, Map data) {

                List<ErrorCode> errs = new ArrayList<>();
                new While<>(alarmUuids).each((alarmUuid, compl) -> {
                    UpdateAlarmMsg updateAlarmMsg = new UpdateAlarmMsg();
                    updateAlarmMsg.setUuid(alarmUuid);
                    updateAlarmMsg.setActions(paramList);
                    bus.makeTargetServiceIdByResourceUuid(updateAlarmMsg, AlarmConstants.SERVICE_ID, alarmUuid);
                    bus.send(updateAlarmMsg, new CloudBusCallBack(compl) {
                        @Override
                        public void run(MessageReply reply) {
                            if (!reply.isSuccess()) {
                                errs.add(reply.getError());
                            }
                            compl.done();
                        }
                    });
                }).run(new WhileDoneCompletion(trigger){
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        if (!errs.isEmpty()) {
                            trigger.fail(errs.get(0));
                            return;
                        }
                        trigger.next();
                    }
                });
            }
        }).then(new NoRollbackFlow() {
            @Override
            public void run(FlowTrigger trigger, Map data) {

                List<ErrorCode> errs = new ArrayList<>();
                new While<>(eventSubscriptionUuids).each((eventSubscriptionUuid, compl) -> {
                    UpdateEventSubscriptionMsg subscriptionMsg = new UpdateEventSubscriptionMsg();
                    subscriptionMsg.setUuid(eventSubscriptionUuid);
                    subscriptionMsg.setActions(paramList);
                    bus.makeTargetServiceIdByResourceUuid(subscriptionMsg,AlarmConstants.SERVICE_ID, eventSubscriptionUuid);
                    bus.send(subscriptionMsg, new CloudBusCallBack(compl) {
                        @Override
                        public void run(MessageReply reply) {
                            if (!reply.isSuccess()) {
                                errs.add(reply.getError());
                            }
                            compl.done();
                        }
                    });
                }).run(new WhileDoneCompletion(trigger) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        if (!errs.isEmpty()) {
                            trigger.fail(errs.get(0));
                            return;
                        }
                        trigger.next();
                    }
                });
            }
        }).done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                completion.success();
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                completion.fail(errCode);
            }
        }).start();
    }


    private void handle(APIAddInstanceToMonitorGroupMsg msg) {
        APIAddInstanceToMonitorGroupEvent event = new APIAddInstanceToMonitorGroupEvent(msg.getId());

        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return syncThreadName;
            }

            @Override
            public void run(SyncTaskChain chain) {
                long instanceNum = Q.New(MonitorGroupInstanceVO.class)
                        .eq(MonitorGroupInstanceVO_.groupUuid, msg.getGroupUuid())
                        .count();
                long max = ZWatchGlobalConfig.MONITOR_GROUP_MAXIMUM_INSTANCE_NUM.value(Long.class);
                if (instanceNum >= max) {
                    event.setError(argerr("The instance in the group has reached the maximum limit"));
                    bus.publish(event);
                    chain.next();
                    return;
                }

                addInstance(msg.getInstanceUuid(), new Completion(chain, msg) {
                    @Override
                    public void success() {
                        String resourceType = Q.New(ResourceVO.class)
                                .select(ResourceVO_.resourceType)
                                .eq(ResourceVO_.uuid, msg.getInstanceUuid())
                                .findValue();
                        String concreteResourceType = Q.New(ResourceVO.class)
                                .select(ResourceVO_.concreteResourceType)
                                .eq(ResourceVO_.uuid, msg.getInstanceUuid())
                                .findValue();
                        if (concreteResourceTypeMap.containsKey(concreteResourceType)) {
                            resourceType = concreteResourceTypeMap.get(concreteResourceType);
                        }
                        MonitorGroupInstanceVO instanceVO = new MonitorGroupInstanceVO();
                        instanceVO.setUuid(Platform.getUuid());
                        instanceVO.setGroupUuid(msg.getGroupUuid());
                        instanceVO.setInstanceResourceType(resourceType);
                        instanceVO.setInstanceUuid(msg.getInstanceUuid());
                        instanceVO.setStatus(AlarmStatus.OK);
                        instanceVO.setCreateDate(new Timestamp(System.currentTimeMillis()));

                        String accountUuid = accountManager.getOwnerAccountUuidOfResource(msg.getGroupUuid());
                        instanceVO.setAccountUuid(accountUuid);
                        instanceVO = dbf.persist(instanceVO);
                        event.setInventory(MonitorGroupInstanceInventory.valueOf(instanceVO));
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
                return String.format("add-instance-%s-to-group-%s", msg.getInstanceUuid(), self.getUuid());
            }
        });
    }

    private void addInstance(String instanceUuid, Completion completion) {
        String resourceType = Q.New(ResourceVO.class)
                .select(ResourceVO_.resourceType)
                .eq(ResourceVO_.uuid, instanceUuid)
                .findValue();
        String concreteResourceType = Q.New(ResourceVO.class)
                .select(ResourceVO_.concreteResourceType)
                .eq(ResourceVO_.uuid, instanceUuid)
                .findValue();
        if (concreteResourceTypeMap.containsKey(concreteResourceType)) {
            resourceType = concreteResourceTypeMap.get(concreteResourceType);
        }

        String namespace = getNamespaceName(resourceType);
        String labelKey = ResourceVOToNamespaceMappingUtils.getRestrictLabelNames(resourceType);
        List<String> instanceUuids = Q.New(MonitorGroupInstanceVO.class)
                .select(MonitorGroupInstanceVO_.instanceUuid)
                .eq(MonitorGroupInstanceVO_.instanceResourceType, resourceType)
                .eq(MonitorGroupInstanceVO_.groupUuid, self.getUuid())
                .listValues();
        instanceUuids.add(instanceUuid);
        String instanceUuidListStr = String.join("|" , instanceUuids);

        FlowChain fchain = FlowChainBuilder.newSimpleFlowChain();
        fchain.setName(String.format("add-instance-%s-to-monitor-group-%s", instanceUuid, self.getUuid()));
        fchain.then(new NoRollbackFlow() {
            String __name__ = "update-alarms-label";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                List<String> alarmUuids = Q.New(MonitorGroupAlarmVO.class)
                        .select(MonitorGroupAlarmVO_.alarmUuid)
                        .eq(MonitorGroupAlarmVO_.groupUuid, self.getUuid())
                        .listValues();
                if (alarmUuids.isEmpty()) {
                    trigger.next();
                    return;
                }

                alarmUuids = Q.New(AlarmVO.class)
                        .select(AlarmVO_.uuid)
                        .eq(AlarmVO_.namespace, namespace)
                        .in(AlarmVO_.uuid, alarmUuids)
                        .listValues();
                if (alarmUuids.isEmpty()) {
                    trigger.next();
                    return;
                }

                List<ErrorCode> errs = new ArrayList<>();
                new While<>(alarmUuids).each((alarmUuid, compl) -> {
                    AlarmLabelVO labelVO = Q.New(AlarmLabelVO.class)
                            .eq(AlarmLabelVO_.alarmUuid, alarmUuid)
                            .eq(AlarmLabelVO_.key, labelKey)
                            .limit(1)
                            .find();
                    if (labelVO != null) {
                        UpdateAlarmLabelMsg updateAlarmLabelMsg = new UpdateAlarmLabelMsg();
                        updateAlarmLabelMsg.setUuid(labelVO.getUuid());
                        updateAlarmLabelMsg.setAlarmUuid(alarmUuid);
                        updateAlarmLabelMsg.setValue(instanceUuidListStr);
                        bus.makeTargetServiceIdByResourceUuid(updateAlarmLabelMsg, AlarmConstants.SERVICE_ID, alarmUuid);

                        bus.send(updateAlarmLabelMsg, new CloudBusCallBack(compl) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    errs.add(reply.getError());
                                }
                                compl.done();
                            }
                        });
                        return;
                    }
                    compl.done();

                }).run(new WhileDoneCompletion(trigger){
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        if (!errs.isEmpty()) {
                            trigger.fail(errs.get(0));
                            return;
                        }

                        trigger.next();
                    }
                });
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "update-eventSubscriptions-label";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                List<String> subscriptionUuids = Q.New(MonitorGroupEventSubscriptionVO.class)
                        .select(MonitorGroupEventSubscriptionVO_.eventSubscriptionUuid)
                        .eq(MonitorGroupEventSubscriptionVO_.groupUuid, self.getUuid())
                        .listValues();
                if (subscriptionUuids.isEmpty()) {
                    trigger.next();
                    return;
                }

                subscriptionUuids = Q.New(EventSubscriptionVO.class)
                        .select(EventSubscriptionVO_.uuid)
                        .eq(EventSubscriptionVO_.namespace, namespace)
                        .in(EventSubscriptionVO_.uuid, subscriptionUuids)
                        .listValues();
                if (subscriptionUuids.isEmpty()) {
                    trigger.next();
                    return;
                }

                List<ErrorCode> errs = new ArrayList<>();
                new While<>(subscriptionUuids).each((subscriptionUuid, compl) -> {
                    EventSubscriptionLabelVO labelVO = Q.New(EventSubscriptionLabelVO.class)
                            .eq(EventSubscriptionLabelVO_.subscriptionUuid, subscriptionUuid)
                            .eq(EventSubscriptionLabelVO_.key, labelKey)
                            .limit(1)
                            .find();
                    if (labelVO != null) {
                        UpdateEventSubscriptionLabelMsg updateLabelMsg = new UpdateEventSubscriptionLabelMsg();
                        updateLabelMsg.setUuid(labelVO.getUuid());
                        updateLabelMsg.setSubscriptionUuid(subscriptionUuid);
                        updateLabelMsg.setValue(instanceUuidListStr);
                        bus.makeTargetServiceIdByResourceUuid(updateLabelMsg, AlarmConstants.SERVICE_ID, subscriptionUuid);

                        bus.send(updateLabelMsg, new CloudBusCallBack(compl) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    errs.add(reply.getError());
                                }
                                compl.done();
                            }
                        });
                        return;
                    }
                    compl.done();
                }).run(new WhileDoneCompletion(trigger){
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        if (!errs.isEmpty()) {
                            trigger.fail(errs.get(0));
                            return;
                        }

                        trigger.next();
                    }
                });
            }
        }).done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                SQL.New(MonitorGroupInstanceVO.class)
                        .eq(MonitorGroupInstanceVO_.instanceUuid, instanceUuid)
                        .eq(MonitorGroupInstanceVO_.groupUuid, self.getUuid())
                        .hardDelete();
                completion.success();
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                completion.fail(errCode);
            }
        }).start();
    }

    private void handle(APIRemoveInstanceFromMonitorGroupMsg msg) {
        APIRemoveInstanceFromMonitorGroupEvent event = new APIRemoveInstanceFromMonitorGroupEvent(msg.getId());

        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return syncThreadName;
            }

            @Override
            public void run(SyncTaskChain chain) {
                removeInstance(msg.getInstanceUuid(), new Completion(msg, chain) {
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
                return String.format("remove-instance-%s", msg.getInstanceUuid());
            }
        });
    }

    private void removeInstance(String instanceUuid, Completion completion) {
        MonitorGroupInstanceVO instanceVO = Q.New(MonitorGroupInstanceVO.class)
                .eq(MonitorGroupInstanceVO_.groupUuid, self.getUuid())
                .eq(MonitorGroupInstanceVO_.instanceUuid, instanceUuid)
                .find();
        if (instanceVO == null) {
            completion.success();
            return;
        }

        String namespace = getNamespaceName(instanceVO.getInstanceResourceType());
        String labelKey = ResourceVOToNamespaceMappingUtils.getRestrictLabelNames(instanceVO.getInstanceResourceType());
        List<String> remainInstanceUuids = Q.New(MonitorGroupInstanceVO.class)
                .select(MonitorGroupInstanceVO_.instanceUuid)
                .eq(MonitorGroupInstanceVO_.instanceResourceType, instanceVO.getInstanceResourceType())
                .eq(MonitorGroupInstanceVO_.groupUuid, self.getUuid())
                .notEq(MonitorGroupInstanceVO_.instanceUuid, instanceVO.getInstanceUuid())
                .listValues();
        String instanceUuidListStr = String.join("|" , remainInstanceUuids);

        FlowChain fchain = FlowChainBuilder.newSimpleFlowChain();
        fchain.setName(String.format("remove-instance-%s-from-monitor-group-%s", instanceUuid, self.getUuid()));
        fchain.then(new NoRollbackFlow() {
            String __name__ = "update-alarms-label";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                List<String> alarmUuids = Q.New(MonitorGroupAlarmVO.class)
                        .select(MonitorGroupAlarmVO_.alarmUuid)
                        .eq(MonitorGroupAlarmVO_.groupUuid, self.getUuid())
                        .listValues();
                if (alarmUuids.isEmpty()) {
                    trigger.next();
                    return;
                }

                alarmUuids = Q.New(AlarmVO.class)
                        .select(AlarmVO_.uuid)
                        .eq(AlarmVO_.namespace, namespace)
                        .in(AlarmVO_.uuid, alarmUuids)
                        .listValues();
                if (alarmUuids.isEmpty()) {
                    trigger.next();
                    return;
                }

                List<ErrorCode> errs = new ArrayList<>();
                new While<>(alarmUuids).each((alarmUuid, compl) -> {
                    AlarmLabelVO labelVO = Q.New(AlarmLabelVO.class)
                            .eq(AlarmLabelVO_.alarmUuid, alarmUuid)
                            .eq(AlarmLabelVO_.key, labelKey)
                            .limit(1)
                            .find();
                    if (labelVO != null) {
                        UpdateAlarmLabelMsg updateAlarmLabelMsg = new UpdateAlarmLabelMsg();
                        updateAlarmLabelMsg.setUuid(labelVO.getUuid());
                        updateAlarmLabelMsg.setAlarmUuid(alarmUuid);
                        updateAlarmLabelMsg.setValue(instanceUuidListStr);
                        bus.makeTargetServiceIdByResourceUuid(updateAlarmLabelMsg, AlarmConstants.SERVICE_ID, alarmUuid);

                        bus.send(updateAlarmLabelMsg, new CloudBusCallBack(compl) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    errs.add(reply.getError());
                                }
                                compl.done();
                            }
                        });
                        return;
                    }
                    compl.done();

                }).run(new WhileDoneCompletion(trigger){
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        if (!errs.isEmpty()) {
                            trigger.fail(errs.get(0));
                            return;
                        }

                        trigger.next();
                    }
                });
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "update-eventSubscriptions-label";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                List<String> subscriptionUuids = Q.New(MonitorGroupEventSubscriptionVO.class)
                        .select(MonitorGroupEventSubscriptionVO_.eventSubscriptionUuid)
                        .eq(MonitorGroupEventSubscriptionVO_.groupUuid, self.getUuid())
                        .listValues();
                if (subscriptionUuids.isEmpty()) {
                    trigger.next();
                    return;
                }

                subscriptionUuids = Q.New(EventSubscriptionVO.class)
                        .select(EventSubscriptionVO_.uuid)
                        .eq(EventSubscriptionVO_.namespace, namespace)
                        .in(EventSubscriptionVO_.uuid, subscriptionUuids)
                        .listValues();
                if (subscriptionUuids.isEmpty()) {
                    trigger.next();
                    return;
                }

                List<ErrorCode> errs = new ArrayList<>();
                new While<>(subscriptionUuids).each((subscriptionUuid, compl) -> {
                    EventSubscriptionLabelVO labelVO = Q.New(EventSubscriptionLabelVO.class)
                            .eq(EventSubscriptionLabelVO_.subscriptionUuid, subscriptionUuid)
                            .eq(EventSubscriptionLabelVO_.key, labelKey)
                            .limit(1)
                            .find();
                    if (labelVO != null) {
                        UpdateEventSubscriptionLabelMsg updateLabelMsg = new UpdateEventSubscriptionLabelMsg();
                        updateLabelMsg.setUuid(labelVO.getUuid());
                        updateLabelMsg.setSubscriptionUuid(subscriptionUuid);
                        updateLabelMsg.setValue(instanceUuidListStr);
                        bus.makeTargetServiceIdByResourceUuid(updateLabelMsg, AlarmConstants.SERVICE_ID, subscriptionUuid);

                        bus.send(updateLabelMsg, new CloudBusCallBack(compl) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    errs.add(reply.getError());
                                }
                                compl.done();
                            }
                        });
                        return;
                    }
                    compl.done();
                }).run(new WhileDoneCompletion(trigger){
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        if (!errs.isEmpty()) {
                            trigger.fail(errs.get(0));
                            return;
                        }

                        trigger.next();
                    }
                });
            }
        }).done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                SQL.New(MonitorGroupInstanceVO.class)
                        .eq(MonitorGroupInstanceVO_.instanceUuid, instanceUuid)
                        .eq(MonitorGroupInstanceVO_.groupUuid, self.getUuid())
                        .hardDelete();
                completion.success();
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
               completion.fail(errCode);
            }
        }).start();
    }

    private void handle(CleanupMonitorGroupInvalidInstanceMsg msg) {
        MessageReply reply = new MessageReply();

        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return syncThreadName;
            }

            @Override
            public void run(SyncTaskChain chain) {
                List<String> instanceUuids = Q.New(MonitorGroupInstanceVO.class)
                        .select(MonitorGroupInstanceVO_.instanceUuid)
                        .eq(MonitorGroupInstanceVO_.groupUuid, msg.getGroupUuid())
                        .listValues();
                if (instanceUuids.isEmpty()) {
                    bus.reply(msg, reply);
                    chain.next();
                    return;
                }

                List<String> existingInstanceUuids = Q.New(ResourceVO.class)
                        .select(ResourceVO_.uuid)
                        .in(ResourceVO_.uuid, instanceUuids)
                        .listValues();
                instanceUuids.removeAll(existingInstanceUuids);
                if (instanceUuids.isEmpty()) {
                    bus.reply(msg, reply);
                    chain.next();
                    return;
                }

                new While<>(instanceUuids).each((instanceUuid, comp) -> {
                    removeInstance(instanceUuid, new Completion(comp) {
                        @Override
                        public void success() {
                            comp.done();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            logger.warn(String.format("remove instance[%s] from monitorGroup[%s] failed, %s", instanceUuid, self.getUuid(), errorCode.getDetails()));
                            comp.done();
                        }
                    });
                }).run(new WhileDoneCompletion(msg, chain) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        bus.reply(msg, reply);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return String.format("cleanup-monitorGroup-%s-nonexistent-instances", self.getUuid());
            }
        });
    }

    private void handle(APIDeleteMonitorGroupMsg msg) {
        APIDeleteMonitorGroupEvent event = new APIDeleteMonitorGroupEvent(msg.getId());

        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return syncThreadName;
            }

            @Override
            public void run(SyncTaskChain chain) {
                deleteMonitorGroup(new Completion(msg, chain) {
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
                return String.format("delete-monitor-group-%s", msg.getUuid());
            }
        });
    }

    private void deleteMonitorGroup(Completion completion) {
        MonitorGroupTemplateRefVO refVO = Q.New(MonitorGroupTemplateRefVO.class)
                .eq(MonitorGroupTemplateRefVO_.groupUuid, self.getUuid())
                .find();
        if (refVO == null) {
            deleteMonitorGroupCreatedResources(new Completion(completion) {
                @Override
                public void success() {
                    SQL.New(MonitorGroupInstanceVO.class)
                            .eq(MonitorGroupInstanceVO_.groupUuid, self.getUuid())
                            .hardDelete();
                    SQL.New(MonitorGroupVO.class)
                            .eq(MonitorGroupVO_.uuid, self.getUuid())
                            .hardDelete();

                    completion.success();
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    completion.fail(errorCode);
                }
            });
            return;
        }

        RevokeMonitorTemplateFromMonitorGroupMsg msg = new RevokeMonitorTemplateFromMonitorGroupMsg();
        msg.setTemplateUuid(refVO.getTemplateUuid());
        msg.setGroupUuid(refVO.getGroupUuid());
        bus.makeLocalServiceId(msg, MonitorGroupConstants.SERVICE_ID);
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                SQL.New(MonitorGroupInstanceVO.class)
                        .eq(MonitorGroupInstanceVO_.groupUuid, self.getUuid())
                        .hardDelete();
                SQL.New(MonitorGroupVO.class)
                        .eq(MonitorGroupVO_.uuid, self.getUuid())
                        .hardDelete();
                completion.success();
            }
        });
    }

    private void handle(APIApplyMonitorTemplateToMonitorGroupMsg msg) {
        APIApplyMonitorTemplateToMonitorGroupEvent event = new APIApplyMonitorTemplateToMonitorGroupEvent(msg.getId());

        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return syncThreadName;
            }

            @Override
            public void run(SyncTaskChain chain) {
                applyMonitorTemplate(msg.getTemplateUuid(), new Completion(msg, chain) {
                    @Override
                    public void success() {
                        MonitorGroupTemplateRefVO refVO = Q.New(MonitorGroupTemplateRefVO.class)
                                .eq(MonitorGroupTemplateRefVO_.groupUuid, self.getUuid())
                                .eq(MonitorGroupTemplateRefVO_.templateUuid, msg.getTemplateUuid())
                                .find();
                        event.setInventory(MonitorGroupTemplateRefInventory.valueOf(refVO));
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
                return String.format("apply-monitorTemplate-%s-to-monitorGroup-%s", msg.getTemplateUuid(), msg.getMonitorGroupUuid());
            }
        });
    }

    private void applyMonitorTemplate(String templateUuid, Completion completion) {
        String accountUuid = accountManager.getOwnerAccountUuidOfResource(self.getUuid());

        FlowChain fchain = FlowChainBuilder.newSimpleFlowChain();
        fchain.setName(String.format("apply-monitor-template-%s-to-monitor-group-%s", templateUuid, self.getUuid()));

        fchain.then(new NoRollbackFlow() {
            String __name__ = "delete-origin-alarms";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                DeleteMonitorGroupAlarmMsg innerMsg = new DeleteMonitorGroupAlarmMsg();
                innerMsg.setGroupUuid(self.getUuid());
                bus.makeTargetServiceIdByResourceUuid(innerMsg, MonitorGroupConstants.SERVICE_ID,  self.getUuid());
                bus.send(innerMsg, new CloudBusCallBack(trigger) {
                    @Override
                    public void run(MessageReply reply) {
                        if (reply.isSuccess()) {
                            trigger.next();
                        } else {
                            trigger.fail(reply.getError());
                        }
                    }
                });
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "delete-origin-eventSubscriptions";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                DeleteMonitorGroupEventSubscriptionMsg innerMsg = new DeleteMonitorGroupEventSubscriptionMsg();
                innerMsg.setGroupUuid(self.getUuid());
                bus.makeTargetServiceIdByResourceUuid(innerMsg, MonitorGroupConstants.SERVICE_ID,  self.getUuid());
                bus.send(innerMsg, new CloudBusCallBack(trigger) {
                    @Override
                    public void run(MessageReply reply) {
                        if (reply.isSuccess()) {
                            trigger.next();
                        } else {
                            trigger.fail(reply.getError());
                        }
                    }
                });
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "delete-eventSubscriptions";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                List<String> subscriptionUuids = Q.New(MonitorGroupEventSubscriptionVO.class)
                        .select(MonitorGroupEventSubscriptionVO_.eventSubscriptionUuid)
                        .eq(MonitorGroupEventSubscriptionVO_.groupUuid, self.getUuid())
                        .listValues();
                if (subscriptionUuids.isEmpty()) {
                    trigger.next();
                    return;
                }

                List<ErrorCode> errs = new ArrayList<>();
                new While<>(subscriptionUuids).each((subscriptionUuid, compl) -> {
                    EventSubscriptionDeletionMsg eventSubscriptionDeletionMsg = new EventSubscriptionDeletionMsg();
                    eventSubscriptionDeletionMsg.setUuid(subscriptionUuid);
                    bus.makeTargetServiceIdByResourceUuid(eventSubscriptionDeletionMsg, AlarmConstants.SERVICE_ID, subscriptionUuid);
                    bus.send(eventSubscriptionDeletionMsg, new CloudBusCallBack(compl, trigger) {
                        @Override
                        public void run(MessageReply reply) {
                            if (!reply.isSuccess()) {
                                errs.add(reply.getError());
                            }
                            compl.done();
                        }
                    });
                }).run(new WhileDoneCompletion(trigger) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        if (!errs.isEmpty()) {
                            trigger.fail(errs.get(0));
                            return;
                        }

                        trigger.next();
                    }
                });
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "create-alarms";

            // ZSTAC-57023 4.7.11 of temporary solution
            private final List<String> childResourceTypes = Arrays.asList(KVMHostVO.class.getSimpleName(),
                    XDragonHostVO.class.getSimpleName()
                    );
            private final List<String> childNamespaceTypes = Arrays.asList(
                    getNamespaceName(KVMHostVO.class.getSimpleName()),
                    getNamespaceName(XDragonHostVO.class.getSimpleName())
                    );
            private final String parentResourceType = HostVO.class.getSimpleName();
            private final String parentNamespace = getNamespaceName(HostVO.class.getSimpleName());

            // ZSTAC-57023 4.7.11 of temporary solution
            private boolean convert(MetricRuleTemplateVO metric) {
                if (metric == null) return false;
                List<Metric> metricList = new ArrayList<>();
                metricList.addAll(KVMHostNamespace.kvmMetrics);
                metricList.addAll(XDragonHostNamespace.xdragonMetrics);

                return metricList.stream()
                        .anyMatch(m -> childNamespaceTypes.contains(m.getNamespace()) && m.getName().equals(metric.getMetricName()));
            }

            @Override
            public void run(FlowTrigger trigger, Map data) {
                Map<String, List<String>> instanceMap = new HashMap<>();
                List<Tuple> tuples = Q.New(MonitorGroupInstanceVO.class)
                        .select(MonitorGroupInstanceVO_.instanceUuid)
                        .select(MonitorGroupInstanceVO_.instanceResourceType)
                        .eq(MonitorGroupInstanceVO_.groupUuid, self.getUuid())
                        .listTuple();
                for (Tuple tuple : tuples) {
                    String resourceType = (String)tuple.get(1);
                    // if (childResourceTypes.contains(resourceType)) resourceType = parentResourceType;
                    List<String> instanceUuids = instanceMap.computeIfAbsent(resourceType, k -> new ArrayList<>());
                    instanceUuids.add((String)tuple.get(0));
                }
                if (instanceMap.isEmpty()) {
                    trigger.next();
                }

                Map<String, List<String>> metricMap = new HashMap<>();
                tuples = Q.New(MetricRuleTemplateVO.class)
                        .select(MetricRuleTemplateVO_.namespace)
                        .select(MetricRuleTemplateVO_.uuid)
                        .eq(MetricRuleTemplateVO_.monitorTemplateUuid, templateUuid)
                        .listTuple();
                for (Tuple tuple : tuples) {
                    String namespace = (String)tuple.get(0);
                    // if (childNamespaceTypes.contains(namespace)) namespace = parentNamespace;
                    List<String> uuids = metricMap.computeIfAbsent(namespace, k -> new ArrayList<>());
                    uuids.add((String)tuple.get(1));
                }
                if (metricMap.isEmpty()) {
                    trigger.next();
                    return;
                }

                List<CreateAlarmMsg> createAlarmMsgs = new ArrayList<>();
                Map<String, String> alarmTemplateMap = new HashMap<>();
                List<APICreateAlarmMsg.ActionParam> actions = new ArrayList<>();
                if (self.getActions() != null) {
                    actions = JSONObjectUtil.toCollection(self.getActions(), ArrayList.class, APICreateAlarmMsg.ActionParam.class);
                }

                for (String resourceType : instanceMap.keySet()) {
                    List<String> instanceUuids = instanceMap.get(resourceType);
                    String namespace = getNamespaceName(resourceType);
                    List<String> metricTempUuids = metricMap.get(namespace);
                    if (metricTempUuids == null) {
                        continue;
                    }

                    List<MetricRuleTemplateVO> templateVOS = Q.New(MetricRuleTemplateVO.class)
                            .in(MetricRuleTemplateVO_.uuid, metricTempUuids)
                            .list();
                    for (MetricRuleTemplateVO templateVO : templateVOS) {
                        CreateAlarmMsg innerMsg = new CreateAlarmMsg();
                        innerMsg.setUuid(Platform.getUuid());
                        String alarmName = templateVO.getNamespace().replace(Namespace.ZSTACK_NAMESPACE_PREFIX, "");
                        alarmName = String.format("%s-%s-%s", alarmName.replace("/", ""), templateVO.getMetricName(), innerMsg.getUuid().substring(0, 4));
                        innerMsg.setName(alarmName);
                        innerMsg.setAccountUuid(accountUuid);
                        innerMsg.setActions(actions);
                        List<Label> labels = new ArrayList<>();
                        if (!convert(templateVO) && childResourceTypes.contains(resourceType)) {
                            resourceType = parentResourceType;
                        }
                        String labelKey = ResourceVOToNamespaceMappingUtils.getRestrictLabelNames(resourceType);
                        labels.add(new Label(labelKey, Label.Operator.Regex, String.join("|", instanceUuids)));
                        if (templateVO.getLabels() != null) {
                            List<Label> templateLabes = JSONObjectUtil.toCollection(templateVO.getLabels(), ArrayList.class, Label.class);
                            labels.addAll(templateLabes);
                        }
                        innerMsg.setLabels(labels);
                        innerMsg.setComparisonOperator(templateVO.getComparisonOperator());
                        innerMsg.setPeriod(templateVO.getPeriod());
                        innerMsg.setEmergencyLevel(templateVO.getEmergencyLevel().name());
                        if (convert(templateVO)) {
                            innerMsg.setNamespace(templateVO.getNamespace());
                        } else {
                            if (childNamespaceTypes.contains(namespace)) namespace = parentNamespace;
                            innerMsg.setNamespace(namespace);
                        }
                        innerMsg.setMetricName(templateVO.getMetricName());
                        innerMsg.setThreshold(templateVO.getThreshold());
                        innerMsg.setRepeatInterval(templateVO.getRepeatInterval());
                        innerMsg.setRepeatCount(templateVO.getRepeatCount());
                        innerMsg.setType(AlarmType.Any.name());
                        innerMsg.setEnableRecovery(templateVO.isEnableRecovery());
                        bus.makeLocalServiceId(innerMsg, AlarmConstants.SERVICE_ID);
                        createAlarmMsgs.add(innerMsg);
                        alarmTemplateMap.put(innerMsg.getUuid(), templateVO.getUuid());
                    }
                }

                if (createAlarmMsgs.isEmpty()) {
                    trigger.next();
                    return;
                }

                List<ErrorCode> errs = new ArrayList<>();
                new While<>(createAlarmMsgs).each((createAlarmMsg, compl) -> {
                    bus.send(createAlarmMsg, new CloudBusCallBack(compl, trigger) {
                        @Override
                        public void run(MessageReply reply) {
                            if (!reply.isSuccess()) {
                                errs.add(reply.getError());
                                compl.done();
                                return;
                            }

                            CreateAlarmReply createAlarmReply = (CreateAlarmReply)reply;
                            MonitorGroupAlarmVO groupAlarmVO = new MonitorGroupAlarmVO();
                            groupAlarmVO.setUuid(Platform.getUuid());
                            groupAlarmVO.setGroupUuid(self.getUuid());
                            groupAlarmVO.setAlarmUuid(createAlarmReply.getInventory().getUuid());
                            groupAlarmVO.setMetricRuleTemplateUuid(alarmTemplateMap.get(groupAlarmVO.getAlarmUuid()));
                            groupAlarmVO.setAccountUuid(accountUuid);
                            groupAlarmVO.setCreateDate(new Timestamp(System.currentTimeMillis()));
                            dbf.persist(groupAlarmVO);

                            compl.done();
                        }
                    });
                }).run(new WhileDoneCompletion(trigger) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        if (!errs.isEmpty()) {
                            trigger.fail(errs.get(0));
                            return;
                        }

                        trigger.next();
                    }
                });
            }
        }).then(new NoRollbackFlow() {
            String __name = "create-event-subscription";

            private final List<String> childResourceTypes = Arrays.asList(KVMHostVO.class.getSimpleName(),
                    XDragonHostVO.class.getSimpleName()
                    );
            private final String parentResourceType = HostVO.class.getSimpleName();

            @Override
            public void run(FlowTrigger trigger, Map data) {
                Map<String, List<String>> instanceMap = new HashMap<>();
                List<Tuple> tuples = Q.New(MonitorGroupInstanceVO.class)
                        .select(MonitorGroupInstanceVO_.instanceUuid)
                        .select(MonitorGroupInstanceVO_.instanceResourceType)
                        .eq(MonitorGroupInstanceVO_.groupUuid, self.getUuid())
                        .listTuple();
                for (Tuple tuple : tuples) {
                    String resourceType = (String)tuple.get(1);
                    if (childResourceTypes.contains(resourceType)) resourceType = parentResourceType;
                    List<String> instanceUuids = instanceMap.computeIfAbsent(resourceType, k -> new ArrayList<>());
                    instanceUuids.add((String)tuple.get(0));
                }
                if (instanceMap.isEmpty()) {
                    trigger.next();
                }

                Map<String, List<String>> templateMap = new HashMap<>();
                tuples = Q.New(EventRuleTemplateVO.class)
                        .select(EventRuleTemplateVO_.namespace)
                        .select(EventRuleTemplateVO_.uuid)
                        .eq(EventRuleTemplateVO_.monitorTemplateUuid, templateUuid)
                        .listTuple();
                for (Tuple tuple : tuples) {
                    String namespace = (String)tuple.get(0);
                    List<String> uuids = templateMap.computeIfAbsent(namespace, k -> new ArrayList<>());
                    uuids.add((String)tuple.get(1));
                }
                if (templateMap.isEmpty()) {
                    trigger.next();
                    return;
                }

                List<SubscribeEventMsg> subscribeEventMsgs = new ArrayList<>();
                Map<String, String> subscriberTemplateMap = new HashMap<>();

                List<APICreateAlarmMsg.ActionParam> actions = new ArrayList<>();
                if (self.getActions() != null) {
                    actions = JSONObjectUtil.toCollection(self.getActions(), ArrayList.class, APICreateAlarmMsg.ActionParam.class);
                }

                for (String resourceType : instanceMap.keySet()) {
                    List<String> instanceUuids = instanceMap.get(resourceType);
                    String namespace = getNamespaceName(resourceType);
                    List<String> tempUuids = templateMap.get(namespace);
                    if (tempUuids == null) {
                        continue;
                    }

                    List<EventRuleTemplateVO> templateVOS = Q.New(EventRuleTemplateVO.class)
                            .in(EventRuleTemplateVO_.uuid, tempUuids)
                            .list();
                    for (EventRuleTemplateVO templateVO : templateVOS) {
                        SubscribeEventMsg innerMsg = new SubscribeEventMsg();
                        innerMsg.setResourceUuid(Platform.getUuid());
                        String subscriberName = templateVO.getNamespace().replace(Namespace.ZSTACK_NAMESPACE_PREFIX, "");
                        subscriberName = String.format("%s-%s-%s", subscriberName.replace("/", ""), templateVO.getEventName(), innerMsg.getResourceUuid().substring(0, 4));
                        innerMsg.setName(subscriberName);
                        innerMsg.setNamespace(templateVO.getNamespace());
                        innerMsg.setEventName(templateVO.getEventName());
                        innerMsg.setAccountUuid(accountUuid);
                        innerMsg.setActions(actions);
                        List<Label> labels = new ArrayList<>();
                        // String labelKey = ResourceVOToNamespaceMappingUtils.getRestrictLabelNames(resourceType);
                        labels.add(new Label(EventData.LABEL_RESOURCE_ID, Label.Operator.Regex, String.join("|", instanceUuids)));
                        if (templateVO.getLabels() != null ) {
                            List<Label> templateLabels = JSONObjectUtil.toCollection(templateVO.getLabels(), ArrayList.class, Label.class);
                            labels.addAll(templateLabels);
                        }
                        innerMsg.setLabels(labels);
                        innerMsg.setEmergencyLevel(templateVO.getEmergencyLevel().name());
                        bus.makeLocalServiceId(innerMsg, AlarmConstants.SERVICE_ID);
                        subscribeEventMsgs.add(innerMsg);
                        subscriberTemplateMap.put(innerMsg.getResourceUuid(), templateVO.getUuid());
                    }
                }

                if (subscribeEventMsgs.isEmpty()) {
                    trigger.next();
                    return;
                }

                List<ErrorCode> errs = new ArrayList<>();
                new While<>(subscribeEventMsgs).each((subscribeEventMsg, compl) -> {
                    bus.send(subscribeEventMsg, new CloudBusCallBack(compl, trigger) {
                        @Override
                        public void run(MessageReply reply) {
                            if (!reply.isSuccess()) {
                                errs.add(reply.getError());
                                compl.done();
                                return;
                            }

                            SubscribeEventReply subscribeEventReply = (SubscribeEventReply)reply;
                            MonitorGroupEventSubscriptionVO groupEventSubscriptionVO = new MonitorGroupEventSubscriptionVO();
                            groupEventSubscriptionVO.setUuid(Platform.getUuid());
                            groupEventSubscriptionVO.setGroupUuid(self.getUuid());
                            groupEventSubscriptionVO.setEventSubscriptionUuid(subscribeEventReply.getInventory().getUuid());
                            groupEventSubscriptionVO.setAccountUuid(accountUuid);
                            groupEventSubscriptionVO.setEventRuleTemplateUuid(subscriberTemplateMap.get(groupEventSubscriptionVO.getEventSubscriptionUuid()));
                            groupEventSubscriptionVO.setCreateDate(new Timestamp(System.currentTimeMillis()));
                            dbf.persist(groupEventSubscriptionVO);

                            compl.done();
                        }
                    });
                }).run(new WhileDoneCompletion(trigger) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        if (!errs.isEmpty()) {
                            trigger.fail(errs.get(0));
                            return;
                        }

                        trigger.next();
                    }
                });
            }
        }).done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                SQL.New(MonitorGroupTemplateRefVO.class)
                        .eq(MonitorGroupTemplateRefVO_.groupUuid, self.getUuid())
                        .hardDelete();

                MonitorGroupTemplateRefVO refVO = new MonitorGroupTemplateRefVO();
                refVO.setGroupUuid(self.getUuid());
                refVO.setTemplateUuid(templateUuid);
                refVO.setUuid(Platform.getUuid());
                refVO.setCreateDate(new Timestamp(System.currentTimeMillis()));
                refVO.setApplied(Boolean.TRUE);
                dbf.persist(refVO);
                completion.success();
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                completion.fail(errCode);
            }
        }).start();
    }

    private void handle(APIRevokeMonitorTemplateFromMonitorGroupMsg msg) {
        APIRevokeMonitorTemplateFromMonitorGroupEvent event = new APIRevokeMonitorTemplateFromMonitorGroupEvent(msg.getId());

        RevokeMonitorTemplateFromMonitorGroupMsg innerMsg = new RevokeMonitorTemplateFromMonitorGroupMsg();
        innerMsg.setGroupUuid(msg.getGroupUuid());
        innerMsg.setTemplateUuid(msg.getTemplateUuid());
        bus.makeLocalServiceId(innerMsg, MonitorGroupConstants.SERVICE_ID);

        bus.send(innerMsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    event.setError(reply.getError());
                }

                bus.publish(event);
            }
        });
    }

    private void handle(DeleteMonitorGroupAlarmMsg msg) {
        MessageReply reply = new MessageReply();

        List<String> alarmUuids = Q.New(MonitorGroupAlarmVO.class)
                .select(MonitorGroupAlarmVO_.alarmUuid)
                .eq(MonitorGroupAlarmVO_.groupUuid, self.getUuid())
                .listValues();

        if (alarmUuids.isEmpty()) {
            bus.reply(msg, reply);
            return;
        }

        List<String> existingAlarmUuids = Q.New(AlarmVO.class)
                .select(AlarmVO_.uuid)
                .in(AlarmVO_.uuid, alarmUuids)
                .listValues();
        List<String> deletedAlarmUuids = new ArrayList<>();
        deletedAlarmUuids.addAll(alarmUuids);
        deletedAlarmUuids.removeAll(existingAlarmUuids);
        if (!deletedAlarmUuids.isEmpty()) {
            SQL.New(MonitorGroupAlarmVO.class)
                    .in(MonitorGroupAlarmVO_.alarmUuid, deletedAlarmUuids)
                    .eq(MonitorGroupAlarmVO_.groupUuid, self.getUuid())
                    .hardDelete();
        }

        if (existingAlarmUuids.isEmpty()) {
            bus.reply(msg, reply);
            return;
        }

        List<ErrorCode> errs = new ArrayList<>();
        new While<>(alarmUuids).each((alarmUuid, compl) -> {
            AlarmDeletionMsg deleteAlarmMsg = new AlarmDeletionMsg();
            deleteAlarmMsg.setUuid(alarmUuid);
            bus.makeTargetServiceIdByResourceUuid(deleteAlarmMsg, AlarmConstants.SERVICE_ID, alarmUuid);
            bus.send(deleteAlarmMsg, new CloudBusCallBack(compl) {
                @Override
                public void run(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        errs.add(reply.getError());
                        compl.done();
                        return;
                    }

                    SQL.New(MonitorGroupAlarmVO.class)
                            .eq(MonitorGroupAlarmVO_.groupUuid, self.getUuid())
                            .eq(MonitorGroupAlarmVO_.alarmUuid, alarmUuid)
                            .hardDelete();
                    compl.done();
                }
            });
        }).run(new WhileDoneCompletion(reply){
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (!errs.isEmpty()) {
                    reply.setError(errs.get(0));
                }

                bus.reply(msg, reply);
            }
        });
    }

    private void handle(DeleteMonitorGroupEventSubscriptionMsg msg) {
        MessageReply reply = new MessageReply();

        List<String> subscriptionUuids = Q.New(MonitorGroupEventSubscriptionVO.class)
                .select(MonitorGroupEventSubscriptionVO_.eventSubscriptionUuid)
                .eq(MonitorGroupEventSubscriptionVO_.groupUuid, self.getUuid())
                .listValues();

        if (subscriptionUuids.isEmpty()) {
            bus.reply(msg, reply);
            return;
        }

        List<String> existingSubscriptionUuids = Q.New(EventSubscriptionVO.class)
                .select(EventSubscriptionVO_.uuid)
                .in(EventSubscriptionVO_.uuid, subscriptionUuids)
                .listValues();
        List<String> deletedSubscriptionUuids = new ArrayList<>();
        deletedSubscriptionUuids.addAll(subscriptionUuids);
        deletedSubscriptionUuids.removeAll(existingSubscriptionUuids);
        if (!deletedSubscriptionUuids.isEmpty()) {
            SQL.New(MonitorGroupEventSubscriptionVO.class)
                    .in(MonitorGroupEventSubscriptionVO_.eventSubscriptionUuid, deletedSubscriptionUuids)
                    .eq(MonitorGroupEventSubscriptionVO_.groupUuid, self.getUuid())
                    .hardDelete();
        }

        if (existingSubscriptionUuids.isEmpty()) {
            bus.reply(msg, reply);
            return;
        }

        List<ErrorCode> errs = new ArrayList<>();
        new While<>(subscriptionUuids).each((subscriptionUuid, compl) -> {
            EventSubscriptionDeletionMsg deleteMsg = new EventSubscriptionDeletionMsg();
            deleteMsg.setUuid(subscriptionUuid);
            bus.makeTargetServiceIdByResourceUuid(deleteMsg, AlarmConstants.SERVICE_ID, subscriptionUuid);
            bus.send(deleteMsg, new CloudBusCallBack(compl) {
                @Override
                public void run(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        errs.add(reply.getError());
                        compl.done();
                        return;
                    }

                    SQL.New(MonitorGroupEventSubscriptionVO.class)
                            .eq(MonitorGroupEventSubscriptionVO_.groupUuid, self.getUuid())
                            .eq(MonitorGroupEventSubscriptionVO_.eventSubscriptionUuid, subscriptionUuid)
                            .hardDelete();
                    compl.done();
                }
            });
        }).run(new WhileDoneCompletion(msg){
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (!errs.isEmpty()) {
                    reply.setError(errs.get(0));
                }

                bus.reply(msg, reply);
            }
        });
    }

    private void deleteMonitorGroupCreatedResources(Completion completion) {
        FlowChain fchain = FlowChainBuilder.newSimpleFlowChain();
        fchain.setName(String.format("delete-monitor-group-%s-alarms-and-eventSubscriptions", self.getUuid()));

        fchain.then(new NoRollbackFlow() {
            String __name__ = "delete-origin-alarms";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                DeleteMonitorGroupAlarmMsg innerMsg = new DeleteMonitorGroupAlarmMsg();
                innerMsg.setGroupUuid(self.getUuid());
                bus.makeTargetServiceIdByResourceUuid(innerMsg, MonitorGroupConstants.SERVICE_ID,  self.getUuid());
                bus.send(innerMsg, new CloudBusCallBack(trigger) {
                    @Override
                    public void run(MessageReply reply) {
                        if (reply.isSuccess()) {
                            trigger.next();
                        } else {
                            trigger.fail(reply.getError());
                        }
                    }
                });
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "delete-origin-eventSubscriptions";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                DeleteMonitorGroupEventSubscriptionMsg innerMsg = new DeleteMonitorGroupEventSubscriptionMsg();
                innerMsg.setGroupUuid(self.getUuid());
                bus.makeTargetServiceIdByResourceUuid(innerMsg, MonitorGroupConstants.SERVICE_ID,  self.getUuid());
                bus.send(innerMsg, new CloudBusCallBack(trigger) {
                    @Override
                    public void run(MessageReply reply) {
                        if (reply.isSuccess()) {
                            trigger.next();
                        } else {
                            trigger.fail(reply.getError());
                        }
                    }
                });
            }
        }).done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                completion.success();
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                completion.fail(errCode);
            }
        }).start();
    }

    private void handle(RevokeMonitorTemplateFromMonitorGroupMsg msg) {
        MessageReply reply = new MessageReply();

        deleteMonitorGroupCreatedResources(new Completion(msg) {
            @Override
            public void success() {
                SQL.New(MonitorGroupTemplateRefVO.class)
                        .eq(MonitorGroupTemplateRefVO_.groupUuid, msg.getGroupUuid())
                        .eq(MonitorGroupTemplateRefVO_.templateUuid, msg.getTemplateUuid())
                        .hardDelete();

                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }
}
