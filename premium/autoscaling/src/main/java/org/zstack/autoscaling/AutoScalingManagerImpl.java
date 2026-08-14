package org.zstack.autoscaling;

import org.zstack.compute.affinityGroup.DeleteAffinityGroupExtensionPoint;
import org.zstack.core.Platform;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.autoscaling.group.APICreateAutoScalingGroupEvent;
import org.zstack.autoscaling.group.APICreateAutoScalingGroupMsg;
import org.zstack.autoscaling.group.AutoScalingGroupBase;
import org.zstack.autoscaling.group.AutoScalingGroupInventory;
import org.zstack.autoscaling.group.AutoScalingGroupMessage;
import org.zstack.autoscaling.group.AutoScalingGroupState;
import org.zstack.autoscaling.group.AutoScalingGroupSystemTags;
import org.zstack.autoscaling.group.AutoScalingGroupVO;
import org.zstack.autoscaling.group.AutoScalingGroupVO_;
import org.zstack.autoscaling.group.ScalingResourceType;
import org.zstack.autoscaling.group.activity.AutoScalingGroupActivityAction;
import org.zstack.autoscaling.group.activity.AutoScalingGroupActivityCause;
import org.zstack.autoscaling.group.activity.CreateAutoScalingGroupActivityMsg;
import org.zstack.autoscaling.group.activity.action.AutoScalingGroupCreateInstancesActionMsg;
import org.zstack.autoscaling.group.activity.action.AutoScalingGroupRemoveInstancesActionMsg;
import org.zstack.autoscaling.group.instance.AutoScalingGroupInstanceVO;
import org.zstack.autoscaling.group.instance.AutoScalingGroupInstanceVO_;
import org.zstack.autoscaling.group.instance.ScalingGroupInstanceFactory;
import org.zstack.autoscaling.group.rule.APICreateAutoScalingGroupRemovalInstanceRuleMsg;
import org.zstack.autoscaling.group.rule.APICreateAutoScalingRuleEvent;
import org.zstack.autoscaling.group.rule.AdjustmentType;
import org.zstack.autoscaling.group.rule.AutoScalingRuleFactory;
import org.zstack.autoscaling.group.rule.AutoScalingRuleState;
import org.zstack.autoscaling.group.rule.AutoScalingRuleStatus;
import org.zstack.autoscaling.group.rule.AutoScalingRuleType;
import org.zstack.autoscaling.group.rule.RemovalInstanceRuleInventory;
import org.zstack.autoscaling.group.rule.RemovalInstanceRuleVO;
import org.zstack.autoscaling.group.rule.RemovalPolicy;
import org.zstack.autoscaling.group.rule.trigger.APICreateAutoScalingRuleAlarmTriggerMsg;
import org.zstack.autoscaling.group.rule.trigger.APICreateAutoScalingRuleSchedulerJobTriggerMsg;
import org.zstack.autoscaling.group.rule.trigger.APICreateAutoScalingRuleTriggerEvent;
import org.zstack.autoscaling.group.rule.trigger.AutoScalingRuleAlarmTriggerInventory;
import org.zstack.autoscaling.group.rule.trigger.AutoScalingRuleAlarmTriggerVO;
import org.zstack.autoscaling.group.rule.trigger.AutoScalingRuleSchedulerJobTriggerInventory;
import org.zstack.autoscaling.group.rule.trigger.AutoScalingRuleSchedulerJobTriggerVO;
import org.zstack.autoscaling.group.rule.trigger.AutoScalingRuleTriggerFactory;
import org.zstack.autoscaling.group.rule.trigger.AutoScalingRuleTriggerState;
import org.zstack.autoscaling.group.rule.trigger.AutoScalingRuleTriggerType;
import org.zstack.autoscaling.template.APIAttachAutoScalingTemplateToGroupEvent;
import org.zstack.autoscaling.template.APIAttachAutoScalingTemplateToGroupMsg;
import org.zstack.autoscaling.template.APICreateAutoScalingTemplateEvent;
import org.zstack.autoscaling.template.APICreateAutoScalingVmTemplateMsg;
import org.zstack.autoscaling.template.APIDeleteAutoScalingTemplateEvent;
import org.zstack.autoscaling.template.APIDeleteAutoScalingTemplateMsg;
import org.zstack.autoscaling.template.APIUpdateAutoScalingTemplateEvent;
import org.zstack.autoscaling.template.APIUpdateAutoScalingVmTemplateMsg;
import org.zstack.autoscaling.template.AutoScalingTemplateGroupRefVO;
import org.zstack.autoscaling.template.AutoScalingTemplateGroupRefVO_;
import org.zstack.autoscaling.template.AutoScalingTemplateState;
import org.zstack.autoscaling.template.AutoScalingTemplateVO;
import org.zstack.autoscaling.template.AutoScalingVmTemplateInventory;
import org.zstack.autoscaling.template.AutoScalingVmTemplateSystemTags;
import org.zstack.autoscaling.template.AutoScalingVmTemplateVO;
import org.zstack.autoscaling.template.DeleteAutoScalingTemplateMsg;
import org.zstack.autoscaling.template.DeleteAutoScalingTemplateReply;
import org.zstack.autoscaling.template.DetachAutoScalingTemplateFromGroupMsg;

import static org.zstack.core.Platform.argerr;
import static org.zstack.core.Platform.operr;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cascade.CascadeFacade;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.cloudbus.ResourceDestinationMaker;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQLBatch;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.PeriodicTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.AbstractService;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.core.workflow.FlowChain;
import org.zstack.header.core.workflow.FlowDoneHandler;
import org.zstack.header.core.workflow.FlowErrorHandler;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudConfigureFailException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.managementnode.ManagementNodeReadyExtensionPoint;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.tag.SystemTagVO;
import org.zstack.header.tag.SystemTagVO_;
import org.zstack.header.tag.SystemTagValidator;
import org.zstack.header.tag.TagType;
import org.zstack.identity.AccountManager;
import org.zstack.network.service.lb.LoadBalancerConstants;
import org.zstack.network.service.lb.LoadBalancerGetPeerL3NetworksMsg;
import org.zstack.network.service.lb.LoadBalancerGetPeerL3NetworksReply;
import org.zstack.network.service.lb.LoadBalancerListenerVO;
import org.zstack.network.service.lb.LoadBalancerListenerVO_;
import org.zstack.tag.SystemTagCreator;
import org.zstack.tag.SystemTagUtils;
import org.zstack.tag.TagManager;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;
import org.zstack.zwatch.ruleengine.RuleManager;

import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Create by weiwang at 2018/8/17
 */
public class AutoScalingManagerImpl extends AbstractService implements AutoScalingManager, ManagementNodeReadyExtensionPoint, DeleteAffinityGroupExtensionPoint {
    private static final CLogger logger = Utils.getLogger(AutoScalingManagerImpl.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private AccountManager acntMgr;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    protected CascadeFacade casf;
    @Autowired
    protected ErrorFacade errf;
    @Autowired
    private TagManager tagMgr;
    @Autowired
    private ThreadFacade thdf;
    @Autowired
    private ResourceDestinationMaker destinationMaker;
    @Autowired
    RuleManager ruleMgr;

    private Map<String, ScalingGroupInstanceFactory> scalingGroupInstanceFactories = Collections.synchronizedMap(new HashMap<>());
    public static Map<String, AutoScalingRuleTriggerFactory> scalingRuleTriggerFactories = Collections.synchronizedMap(new HashMap<String, AutoScalingRuleTriggerFactory>());

    private Map<String, AutoScalingRuleFactory> autoScalingRuleFactories = Collections.synchronizedMap(new HashMap<>());
    private Future autoScalingGroupInstanceTask;
    private Set<String> autoScalingGroupInTracking = Collections.synchronizedSet(new HashSet<String>());

    @Override
    public String getId() {
        return bus.makeLocalServiceId(AutoScalingConstants.SERVICE_ID);
    }

    // Start checking the number of instances in each auto scaling group
    private void startAutoScalingGroupInstanceTask() {
        if (autoScalingGroupInstanceTask != null) {
            autoScalingGroupInstanceTask.cancel(true);
        }

        autoScalingGroupInstanceTask = thdf.submitPeriodicTask(new PeriodicTask() {
            @Override
            public TimeUnit getTimeUnit() {
                return TimeUnit.SECONDS;
            }

            @Override
            public long getInterval() {
                return AutoScalingGlobalConfig.CHECK_THE_NUMBER_OF_INSTANCES_IN_THE_GROUP_INTERVAL.value(Long.class);
            }

            @Override
            public String getName() {
                return "check-the-number-of-instances-in-each-auto-scaling-group-task";
            }

            @Override
            public void run() {
                checkAutoScalingGroupInstance();
            }
        });
    }

    private void checkAutoScalingGroupInstance() {
        List<AutoScalingGroupVO> groups = Q.New(AutoScalingGroupVO.class)
                .eq(AutoScalingGroupVO_.state, AutoScalingGroupState.Enabled)
                .list();

        for (AutoScalingGroupVO group : groups) {
            try {
                if (!destinationMaker.isManagedByUs(group.getUuid())) {
                    continue;
                }

                if (autoScalingGroupInTracking.contains(group.getUuid())) {
                    continue;
                }
                autoScalingGroupInTracking.add(group.getUuid());

                int scalingGroupInstanceCount = Q.New(AutoScalingGroupInstanceVO.class)
                        .eq(AutoScalingGroupInstanceVO_.scalingGroupUuid, group.getUuid())
                        .count()
                        .intValue();

                boolean initializing = initAutoScalingGroupInstance(group, scalingGroupInstanceCount);
                if (initializing) {
                    continue;
                }

                if (scalingGroupInstanceCount < group.getMinResourceSize()) {
                    int size = group.getMinResourceSize() - scalingGroupInstanceCount;
                    addInstanceToScalingGroup(group, size);
                } else if (scalingGroupInstanceCount > group.getMaxResourceSize()) {
                    int size = scalingGroupInstanceCount - group.getMaxResourceSize();
                    removeInstanceFromScalingGroup(group, size);
                } else {
                    autoScalingGroupInTracking.remove(group.getUuid());
                }
            } catch (Exception e) {
                logger.warn(String.format("checking the number of instances in autoScalingGroup[%s] fail", group.getUuid()), e);
                autoScalingGroupInTracking.remove(group.getUuid());
            }
        }
    }

    private boolean initAutoScalingGroupInstance(AutoScalingGroupVO group, int scalingGroupInstanceCount) {
        String groupUuid = group.getUuid();
        if (!AutoScalingGroupSystemTags.INITIAL_INSTANCE_NUMBER.hasTag(groupUuid)) {
            return false;
        }

        if (AutoScalingGroupSystemTags.INITIALIZATION_INSTANCE_COMPLETED.hasTag(groupUuid)) {
            return false;
        }

        String initialInstanceNumberStr = AutoScalingGroupSystemTags.INITIAL_INSTANCE_NUMBER.getTokenByResourceUuid(groupUuid, AutoScalingGroupSystemTags.INITIAL_INSTANCE_NUMBER_TOKEN);
        int initialInstanceNumber = Integer.parseInt(initialInstanceNumberStr);
        if (scalingGroupInstanceCount >= initialInstanceNumber) {
            SystemTagCreator creator = AutoScalingGroupSystemTags.INITIALIZATION_INSTANCE_COMPLETED.newSystemTagCreator(groupUuid);
            creator.ignoreIfExisting = true;
            creator.inherent = true;
            creator.recreate = true;
            creator.unique = true;
            creator.setTagByTokens(map(e(AutoScalingGroupSystemTags.INITIALIZATION_INSTANCE_COMPLETED_TOKEN, true)));
            creator.create();
            return false;
        }

        if (initialInstanceNumber > group.getMaxResourceSize() || initialInstanceNumber < group.getMinResourceSize()) {
            return false;
        }

        int size = initialInstanceNumber - scalingGroupInstanceCount;
        addInstanceToScalingGroup(group, size);
        return true;
    }

    private void addInstanceToScalingGroup(AutoScalingGroupVO group, int size) {
        CreateAutoScalingGroupActivityMsg msg = new CreateAutoScalingGroupActivityMsg();

        String uuid = Platform.getUuid();
        msg.setActivityUuid(uuid);
        msg.setName("");
        msg.setScalingGroupUuid(group.getUuid());
        msg.setDescription("");
        msg.setActivityAction(AutoScalingGroupActivityAction.AddingNewInstance.toString());
        msg.setCause(AutoScalingGroupActivityCause.MaintainTheNumberOfInstances.toString());
        bus.makeLocalServiceId(msg, AutoScalingConstants.SERVICE_ID);

        AutoScalingGroupCreateInstancesActionMsg actionMsg = new AutoScalingGroupCreateInstancesActionMsg();
        actionMsg.setAddingInstanceSize(size);
        actionMsg.setAutoScalingGroupUuid(group.getUuid());
        actionMsg.setAutoScalingGroupActivityUuid(uuid);
        bus.makeLocalServiceId(actionMsg, AutoScalingConstants.SERVICE_ID);
        msg.setActionMessage(actionMsg);

        bus.send(msg, new CloudBusCallBack(null) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    logger.warn(String.format("add instance to autoScalingGroup[%s] failed, %s", group.getUuid(), reply.getError().toString()));
                }

                autoScalingGroupInTracking.remove(group.getUuid());
            }
        });
    }

    private void removeInstanceFromScalingGroup(AutoScalingGroupVO group, int size) {
        CreateAutoScalingGroupActivityMsg msg = new CreateAutoScalingGroupActivityMsg();

        String uuid = Platform.getUuid();
        msg.setActivityUuid(uuid);
        msg.setName("");
        msg.setScalingGroupUuid(group.getUuid());
        msg.setDescription("");
        msg.setActivityAction(AutoScalingGroupActivityAction.RemovalInstance.toString());
        msg.setCause(AutoScalingGroupActivityCause.MaintainTheNumberOfInstances.toString());
        bus.makeLocalServiceId(msg, AutoScalingConstants.SERVICE_ID);

        AutoScalingGroupRemoveInstancesActionMsg actionMsg = new AutoScalingGroupRemoveInstancesActionMsg();
        actionMsg.setRemovalInstanceSize(size);
        actionMsg.setAutoScalingGroupUuid(group.getUuid());
        actionMsg.setAutoScalingGroupActivityUuid(uuid);
        bus.makeLocalServiceId(actionMsg, AutoScalingConstants.SERVICE_ID);
        msg.setActionMessage(actionMsg);

        bus.send(msg, new CloudBusCallBack(null) {
            @Override
            public void run(MessageReply reply) {
                //log result
                autoScalingGroupInTracking.remove(group.getUuid());
            }
        });

    }

    @Override
    public boolean start() {
        try {
            populateExtensions();
            prepareSystemTags();
            AutoScalingGlobalConfig.CHECK_THE_NUMBER_OF_INSTANCES_IN_THE_GROUP_INTERVAL.installUpdateExtension((oldConfig, newConfig) -> startAutoScalingGroupInstanceTask());

            return true;
        } catch (Exception e) {
            throw new CloudConfigureFailException(AutoScalingGroupBase.class, e.getMessage(), e);
        }
    }

    @Override
    public void managementNodeReady() {
        startAutoScalingGroupInstanceTask();
    }

    private void prepareSystemTags() {
        AutoScalingVmTemplateSystemTags.LOAD_BALANCER_LISTENER_UUIDS.installValidator(new SystemTagValidator() {
            @Override
            public void validateSystemTag(String resourceUuid, Class resourceType, String systemTag) {
                String loadBalancerListenerUuidListStr = AutoScalingVmTemplateSystemTags.LOAD_BALANCER_LISTENER_UUIDS.getTokenByTag(systemTag,
                        AutoScalingVmTemplateSystemTags.LOAD_BALANCER_LISTENER_UUIDS_TOKEN);
                String[] listenerUuids = loadBalancerListenerUuidListStr.split(AutoScalingConstants.AutoScalingTemplate.VmInstance.SEPARATOR);
                List<String> loadBalancerUuids = Q.New(LoadBalancerListenerVO.class).select(LoadBalancerListenerVO_.loadBalancerUuid)
                        .in(LoadBalancerListenerVO_.uuid, Arrays.asList(listenerUuids))
                        .listValues();
                if (loadBalancerUuids.isEmpty()) {
                    return;
                }

                AutoScalingVmTemplateVO vmTemplateVO = dbf.findByUuid(resourceUuid, AutoScalingVmTemplateVO.class);
                DebugUtils.Assert(vmTemplateVO!=null, String.format("invalide AutoScalingVmTemplateVO Uuid: %s", resourceUuid));
                List<String> l3Uuids = Arrays.asList(vmTemplateVO.getL3NetworkUuids().split(AutoScalingConstants.AutoScalingTemplate.VmInstance.SEPARATOR));

                loadBalancerUuids.forEach( uuid -> {
                    LoadBalancerGetPeerL3NetworksMsg msg = new LoadBalancerGetPeerL3NetworksMsg();
                    msg.setLoadBalancerUuid(uuid);
                    bus.makeTargetServiceIdByResourceUuid(msg, LoadBalancerConstants.SERVICE_ID, uuid);
                    MessageReply reply = bus.call(msg);
                    if (!reply.isSuccess()) {
                        throw new OperationFailureException(reply.getError());
                    }

                    LoadBalancerGetPeerL3NetworksReply r = reply.castReply();
                    List<L3NetworkInventory> l3s = r.getInventories();
                    if (!l3s.stream().map(L3NetworkInventory::getUuid).collect(Collectors.toList()).containsAll(l3Uuids)) {
                        throw new OperationFailureException(argerr("invalid l3 network uuids[%s] for listener that belongs lb[%s], all the networks must be attached the LB service and be attached with the same vRouter with LB", l3Uuids, uuid));
                    }
                });
            }
        });
    }

    private void populateExtensions() {
        for (ScalingGroupInstanceFactory ext : pluginRgty.getExtensionList(ScalingGroupInstanceFactory.class)) {
            ScalingGroupInstanceFactory old = scalingGroupInstanceFactories.get(ext.getType().toString());
            if (old != null) {
                throw new CloudRuntimeException(String.format("duplicate ScalingGroupInstanceFactory[%s, %s] for type[%s]",
                        old.getClass().getName(), ext.getClass().getName(), ext.getType()));
            }
            scalingGroupInstanceFactories.put(ext.getType().toString(), ext);
        }

        for (AutoScalingRuleFactory ext : pluginRgty.getExtensionList(AutoScalingRuleFactory.class)) {
            AutoScalingRuleFactory old = autoScalingRuleFactories.get(ext.getType().toString());
            if (old != null) {
                throw new CloudRuntimeException(String.format("duplicate AutoScalingRuleFactory[%s, %s] for type[%s]",
                        old.getClass().getName(), ext.getClass().getName(), ext.getType()));
            }
            autoScalingRuleFactories.put(ext.getType().toString(), ext);
        }

        for (AutoScalingRuleTriggerFactory ext : pluginRgty.getExtensionList(AutoScalingRuleTriggerFactory.class)) {
            AutoScalingRuleTriggerFactory old = scalingRuleTriggerFactories.get(ext.getType().toString());
            if (old != null) {
                throw new CloudRuntimeException(String.format("duplicate factoryMap[%s, %s] for type[%s]",
                        old.getClass().getName(), ext.getClass().getName(), ext.getType()));
            }
            scalingRuleTriggerFactories.put(ext.getType().toString(), ext);
        }


    }


    @Override
    public AutoScalingRuleTriggerFactory getAutoScalingRuleTriggerFactory(AutoScalingRuleTriggerType type) {
        AutoScalingRuleTriggerFactory factory = scalingRuleTriggerFactories.get(type.toString());
        if (factory == null) {
            throw new CloudRuntimeException(String.format("No AutoScalingRuleTriggerFactory of type[%s] found", type));
        }
        return factory;
    }


    @Override
    public boolean stop() {
        if (autoScalingGroupInstanceTask != null) {
            autoScalingGroupInstanceTask.cancel(true);
        }
        return true;
    }

    @Override
    public ScalingGroupInstanceFactory getScalingGroupInstanceFactory(ScalingResourceType type) {
        ScalingGroupInstanceFactory factory = scalingGroupInstanceFactories.get(type.toString());
        if (factory == null) {
            throw new CloudRuntimeException(String.format("No ScalingGroupInstanceFactory of type[%s] found", type));
        }
        return factory;
    }

    @Override
    public AutoScalingRuleFactory getAutoScalingRuleFactory(AutoScalingRuleType type) {
        AutoScalingRuleFactory factory = autoScalingRuleFactories.get(type.toString());
        if (factory == null) {
            throw new CloudRuntimeException(String.format("No AutoScalingRuleFactory of type[%s] found", type));
        }
        return factory;
    }

    private void passThrough(AutoScalingGroupMessage msg) {
        AutoScalingGroupVO vo = dbf.findByUuid(msg.getAutoScalingGroupUuid(), AutoScalingGroupVO.class);

        if (vo == null) {
            String err = String.format("Cannot find AutoScalingGroup[uuid:%s], it may have been deleted", msg.getAutoScalingGroupUuid());
            bus.replyErrorByMessageType((Message) msg, err);
            return;
        }

        AutoScalingGroupBase group = new AutoScalingGroupBase(vo);
        group.handleMessage((Message) msg);
    }

    @Override
    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof AutoScalingGroupMessage) {
            passThrough((AutoScalingGroupMessage) msg);
        } else if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage) msg);
        } else {
            handleLocalMessage(msg);
        }
    }

    private void handleLocalMessage(Message msg) {
        if (msg instanceof DeleteAutoScalingTemplateMsg) {
            handle((DeleteAutoScalingTemplateMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APICreateAutoScalingGroupRemovalInstanceRuleMsg) {
            handle((APICreateAutoScalingGroupRemovalInstanceRuleMsg) msg);
        } else if (msg instanceof APICreateAutoScalingGroupMsg) {
            handle((APICreateAutoScalingGroupMsg) msg);
        } else if (msg instanceof APICreateAutoScalingVmTemplateMsg) {
            handle((APICreateAutoScalingVmTemplateMsg) msg);
        } else if (msg instanceof APIDeleteAutoScalingTemplateMsg) {
            handle((APIDeleteAutoScalingTemplateMsg) msg);
        } else if (msg instanceof APIAttachAutoScalingTemplateToGroupMsg) {
            handle((APIAttachAutoScalingTemplateToGroupMsg) msg);
        } else if (msg instanceof APIUpdateAutoScalingVmTemplateMsg) {
            handle((APIUpdateAutoScalingVmTemplateMsg) msg);
        } else if (msg instanceof APICreateAutoScalingRuleAlarmTriggerMsg) {
            handle((APICreateAutoScalingRuleAlarmTriggerMsg) msg);
        } else if (msg instanceof APICreateAutoScalingRuleSchedulerJobTriggerMsg) {
            handle((APICreateAutoScalingRuleSchedulerJobTriggerMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(APICreateAutoScalingRuleSchedulerJobTriggerMsg msg) {
        APICreateAutoScalingRuleTriggerEvent event = new APICreateAutoScalingRuleTriggerEvent(msg.getId());

        AutoScalingRuleSchedulerJobTriggerVO triggerVO = new AutoScalingRuleSchedulerJobTriggerVO();
        triggerVO.setUuid(msg.getResourceUuid() == null ? Platform.getUuid() : msg.getResourceUuid());
        triggerVO.setName(msg.getName());
        triggerVO.setType(AutoScalingRuleTriggerType.valueOf(msg.getTriggerType()));
        triggerVO.setSchedulerJobUuid(msg.getSchedulerJobUuid());
        triggerVO.setRuleUuid(msg.getRuleUuid());
        triggerVO.setDescription(msg.getDescription());
        triggerVO.setCreateDate(new Timestamp(System.currentTimeMillis()));
        triggerVO.setAccountUuid(msg.getSession().getAccountUuid());
        triggerVO.setState(AutoScalingRuleTriggerState.Enabled);
        triggerVO = dbf.persistAndRefresh(triggerVO);

        AutoScalingRuleSchedulerJobTriggerInventory inventory = AutoScalingRuleSchedulerJobTriggerInventory.valueOf(triggerVO);
        event.setInventory(inventory);
        bus.publish(event);
    }

    private void handle(APICreateAutoScalingGroupRemovalInstanceRuleMsg msg) {
        APICreateAutoScalingRuleEvent event = new APICreateAutoScalingRuleEvent(msg.getId());

        RemovalInstanceRuleVO vo = new RemovalInstanceRuleVO();
        vo.setUuid(msg.getResourceUuid() == null ? Platform.getUuid() : msg.getResourceUuid());
        vo.setName(msg.getName());
        vo.setDescription(msg.getDescription());
        vo.setType(AutoScalingRuleType.valueOf(msg.getType()));
        vo.setCooldown(msg.getCooldown());
        vo.setRemovalPolicy(RemovalPolicy.valueOf(msg.getRemovalPolicy()));
        vo.setAdjustmentValue(msg.getAdjustmentValue());
        vo.setAdjustmentType(AdjustmentType.valueOf(msg.getAdjustmentType()));
        vo.setState(AutoScalingRuleState.Enabled);
        vo.setStatus(AutoScalingRuleStatus.Created);
        vo.setCreateDate(new Timestamp(System.currentTimeMillis()));
        vo = dbf.persistAndRefresh(vo);

        tagMgr.createTagsFromAPICreateMessage(msg, vo.getUuid(), RemovalInstanceRuleVO.class.getSimpleName());

        event.setInventory(RemovalInstanceRuleInventory.valueOf(vo));
        bus.publish(event);
    }

    private void handle(APICreateAutoScalingGroupMsg msg) {
        APICreateAutoScalingGroupEvent event = new APICreateAutoScalingGroupEvent(msg.getId());

        AutoScalingGroupVO vo = new AutoScalingGroupVO();
        vo.setUuid(msg.getResourceUuid() == null ? Platform.getUuid() : msg.getResourceUuid());
        vo.setName(msg.getName());
        vo.setDescription(msg.getDescription());
        vo.setScalingResourceType(ScalingResourceType.valueOf(msg.getScalingResourceType()));
        vo.setCreateDate(new Timestamp(System.currentTimeMillis()));
        vo.setRemovalPolicy(RemovalPolicy.valueOf(msg.getRemovalPolicy()));
        vo.setState(msg.isDefaultEnable() ? AutoScalingGroupState.Enabled : AutoScalingGroupState.Disabled);
        vo.setMaxResourceSize(msg.getMaxResourceSize());
        vo.setMinResourceSize(msg.getMinResourceSize());
        vo.setDefaultCooldown(msg.getDefaultCooldown());
        vo.setAccountUuid(msg.getSession().getAccountUuid());
        vo = dbf.persistAndRefresh(vo);

        tagMgr.createTagsFromAPICreateMessage(msg, vo.getUuid(), AutoScalingGroupVO.class.getSimpleName());

        event.setInventory(AutoScalingGroupInventory.valueOf(vo));
        bus.publish(event);
    }

    private void handle(APICreateAutoScalingVmTemplateMsg msg) {
        APICreateAutoScalingTemplateEvent event = new APICreateAutoScalingTemplateEvent(msg.getId());

        AutoScalingVmTemplateVO vo = new AutoScalingVmTemplateVO();
        vo.setUuid(msg.getResourceUuid() == null ? Platform.getUuid() : msg.getResourceUuid());
        vo.setName(msg.getName());
        vo.setType(AutoScalingConstants.AutoScalingTemplate.VmInstance.TYPE);
        vo.setVmInstanceName(msg.getVmInstanceName());
        vo.setVmInstanceType(msg.getVmInstanceType());
        vo.setDescription(msg.getDescription());
        vo.setVmInstanceDescription(msg.getVmInstanceDescription());
        vo.setVmInstanceOfferingUuid(msg.getVmInstanceOfferingUuid());
        vo.setImageUuid(msg.getImageUuid());
        if (msg.getL3NetworkUuids() != null) {
            vo.setL3NetworkUuids(StringUtils.join(msg.getL3NetworkUuids(), AutoScalingConstants.AutoScalingTemplate.VmInstance.SEPARATOR));
        }
        vo.setType(msg.getVmInstanceType());
        vo.setRootDiskOfferingUuid(msg.getRootDiskOfferingUuid());
        if (msg.getDataDiskOfferingUuids() != null && !msg.getDataDiskOfferingUuids().isEmpty()) {
            vo.setDataDiskOfferingUuids(StringUtils.join(msg.getDataDiskOfferingUuids(), AutoScalingConstants.AutoScalingTemplate.VmInstance.SEPARATOR));
        }
        vo.setVmInstanceZoneUuid(msg.getVmInstanceZoneUuid());
        vo.setVmInstanceClusterUuid(msg.getVmInstanceClusterUuid());
        vo.setHostUuid(msg.getHostUuid());
        vo.setPrimaryStorageUuidForRootVolume(msg.getPrimaryStorageUuidForRootVolume());
        vo.setDefaultL3NetworkUuid(msg.getDefaultL3NetworkUuid());
        vo.setStrategy(msg.getStrategy());
        vo.setCreateDate(new Timestamp(System.currentTimeMillis()));
        vo.setState(AutoScalingTemplateState.Enabled);
        vo.setAccountUuid(msg.getSession().getAccountUuid());
        vo = dbf.persistAndRefresh(vo);

        tagMgr.createTagsFromAPICreateMessage(msg, vo.getUuid(), AutoScalingVmTemplateVO.class.getSimpleName());

        event.setInventory(AutoScalingVmTemplateInventory.valueOf(vo));
        bus.publish(event);
    }

    private void handle(APIAttachAutoScalingTemplateToGroupMsg msg) {
        APIAttachAutoScalingTemplateToGroupEvent event = new APIAttachAutoScalingTemplateToGroupEvent(msg.getId());

        AutoScalingTemplateGroupRefVO refVO = Q.New(AutoScalingTemplateGroupRefVO.class)
                .eq(AutoScalingTemplateGroupRefVO_.templateUuid, msg.getUuid())
                .eq(AutoScalingTemplateGroupRefVO_.groupUuid, msg.getGroupUuid())
                .find();

        if (refVO == null) {
            refVO = new AutoScalingTemplateGroupRefVO();
            refVO.setGroupUuid(msg.getGroupUuid());
            refVO.setTemplateUuid(msg.getUuid());
            refVO.setCreateDate(new Timestamp(System.currentTimeMillis()));
            dbf.persistAndRefresh(refVO);
        }

        AutoScalingGroupVO vo = Q.New(AutoScalingGroupVO.class)
                .eq(AutoScalingGroupVO_.uuid, msg.getGroupUuid()).find();
        event.setInventory(AutoScalingGroupInventory.valueOf(vo));
        bus.publish(event);
    }

    private void handle(APICreateAutoScalingRuleAlarmTriggerMsg msg) {
        APICreateAutoScalingRuleTriggerEvent event = new APICreateAutoScalingRuleTriggerEvent(msg.getId());

        AutoScalingRuleAlarmTriggerVO triggerVO = new AutoScalingRuleAlarmTriggerVO();
        triggerVO.setUuid(msg.getResourceUuid() == null ? Platform.getUuid() : msg.getResourceUuid());
        triggerVO.setName(msg.getName());
        triggerVO.setType(AutoScalingRuleTriggerType.valueOf(msg.getTriggerType()));
        triggerVO.setAlarmUuid(msg.getAlarmUuid());
        triggerVO.setRuleUuid(msg.getRuleUuid());
        triggerVO.setDescription(msg.getDescription());
        triggerVO.setCreateDate(new Timestamp(System.currentTimeMillis()));
        triggerVO.setAccountUuid(msg.getSession().getAccountUuid());
        triggerVO.setState(AutoScalingRuleTriggerState.Enabled);
        triggerVO = dbf.persistAndRefresh(triggerVO);

        AutoScalingRuleAlarmTriggerInventory inventory = AutoScalingRuleAlarmTriggerInventory.valueOf(triggerVO);
        event.setInventory(inventory);
        bus.publish(event);
    }

    private void handle(APIDeleteAutoScalingTemplateMsg msg) {
        APIDeleteAutoScalingTemplateEvent event = new APIDeleteAutoScalingTemplateEvent(msg.getId());

        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return String.format("autoScalingTemplate-%s", msg.getUuid());
            }

            @Override
            public void run(SyncTaskChain chain) {
                DeleteAutoScalingTemplateMsg deleteMsg = new DeleteAutoScalingTemplateMsg();
                deleteMsg.setTemplateUuid(msg.getUuid());
                bus.makeLocalServiceId(deleteMsg, AutoScalingConstants.SERVICE_ID);

                bus.send(deleteMsg, new CloudBusCallBack(msg, chain) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            event.setError(reply.getError());
                        }

                        bus.publish(event);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return String.format("delete-autoScalingTemplate-%s", msg.getUuid());
            }
        });
    }

    private void handle(DeleteAutoScalingTemplateMsg msg) {
        DeleteAutoScalingTemplateReply reply = new DeleteAutoScalingTemplateReply();
        String templateUuid = msg.getTemplateUuid();

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("delete-autoScalingTemplate-%s", msg.getTemplateUuid()));
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                List<ErrorCode> errors = Collections.synchronizedList(new LinkedList<ErrorCode>());

                flow(new NoRollbackFlow() {
                    String __name__ = "detach-template-from-groups";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        List<AutoScalingTemplateGroupRefVO> refVOS = Q.New(AutoScalingTemplateGroupRefVO.class)
                                .eq(AutoScalingTemplateGroupRefVO_.templateUuid, msg.getTemplateUuid())
                                .list();
                        if (refVOS == null || refVOS.isEmpty()) {
                            trigger.next();
                            return;
                        }

                        List<DetachAutoScalingTemplateFromGroupMsg> detachMsgs = new ArrayList<>();
                        for (AutoScalingTemplateGroupRefVO refVO : refVOS) {
                            DetachAutoScalingTemplateFromGroupMsg detachMsg = new DetachAutoScalingTemplateFromGroupMsg();
                            detachMsg.setTemplateUuid(templateUuid);
                            detachMsg.setAutoScalingGroupUuid(refVO.getGroupUuid());
                            bus.makeLocalServiceId(detachMsg, AutoScalingConstants.SERVICE_ID);
                            detachMsgs.add(detachMsg);
                        }

                        new While<>(detachMsgs).step((detachMsg, completion) -> {
                            bus.send(detachMsg, new CloudBusCallBack(completion) {
                                @Override
                                public void run(MessageReply rly) {
                                    if (!rly.isSuccess()) {
                                        errors.add(rly.getError());
                                    }
                                    completion.done();
                                }
                            });
                        }, 10).run(new WhileDoneCompletion(trigger) {
                            @Override
                            public void done(ErrorCodeList errorCodeList) {
                                if (errors.size() != 0) {
                                    trigger.fail(operr("detach autoScalingTemplate[%s] from AutoScalingGroup failed, errors are %s"
                                            ,msg.getTemplateUuid(), JSONObjectUtil.toJsonString(errors)));
                                    return;
                                }
                                trigger.next();
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "delete-auto-scaling-template";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        new SQLBatch() {
                            @Override
                            protected void scripts() {
                                AutoScalingTemplateVO templateVO = findByUuid(templateUuid, AutoScalingTemplateVO.class);

                                sql("update AutoScalingGroupInstanceVO set templateUuid = null " +
                                        " where templateUuid = :templateUuid")
                                        .param("templateUuid", templateUuid)
                                        .execute();

                                remove(templateVO);
                            }
                        }.execute();

                        trigger.next();
                    }
                });

                done(new FlowDoneHandler(msg) {
                    @Override
                    public void handle(Map data) {
                        bus.reply(msg, reply);
                    }
                });

                error(new FlowErrorHandler(msg) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        reply.setError(errCode);
                        bus.reply(msg, reply);
                    }
                });
            }

        }).start();
    }

    private void handle(APIUpdateAutoScalingVmTemplateMsg msg) {
        APIUpdateAutoScalingTemplateEvent event = new APIUpdateAutoScalingTemplateEvent(msg.getId());

        boolean flag = false;

        AutoScalingVmTemplateVO templateVO = dbf.findByUuid(msg.getUuid(), AutoScalingVmTemplateVO.class);
        if (msg.getName() != null) {
            templateVO.setName(msg.getName());
            flag = true;
        }

        if (msg.getDescription() != null) {
            templateVO.setDescription(msg.getDescription());
            flag = true;
        }

        if (msg.getVmInstanceName() != null) {
            templateVO.setVmInstanceName(msg.getVmInstanceName());
            flag = true;
        }

        if (msg.getVmInstanceDescription() != null) {
            templateVO.setVmInstanceDescription(msg.getVmInstanceDescription());
            flag = true;
        }

        if (msg.getImageUuid() != null) {
            templateVO.setImageUuid(msg.getImageUuid());
            flag = true;
        }

        if (msg.getVmInstanceOfferingUuid() != null) {
            templateVO.setVmInstanceOfferingUuid(msg.getVmInstanceOfferingUuid());
            flag = true;
        }

        if (msg.getVmInstanceClusterUuid() != null) {
            templateVO.setVmInstanceClusterUuid(msg.getVmInstanceClusterUuid());
            flag = true;
        }

        if (msg.getHostUuid() != null) {
            templateVO.setHostUuid(msg.getHostUuid());
            flag = true;
        }

        if (flag) {
            templateVO = dbf.updateAndRefresh(templateVO);
        }

        List<String> tags = msg.getSystemTags();
        if (tags != null && !tags.isEmpty()) {
            String userdata = SystemTagUtils.findTagValue(tags, AutoScalingVmTemplateSystemTags.USERDATA, AutoScalingVmTemplateSystemTags.USERDATA_TOKEN);
            if (userdata != null) {
                SystemTagCreator creator = AutoScalingVmTemplateSystemTags.USERDATA.newSystemTagCreator(templateVO.getUuid());
                creator.setTagByTokens(map(e(AutoScalingVmTemplateSystemTags.USERDATA_TOKEN, userdata)));
                creator.inherent = false;
                creator.recreate = true;
                creator.create();
            }

            String affinityGroup = SystemTagUtils.findTagValue(tags, AutoScalingVmTemplateSystemTags.AFFINITY_GROUP_UUID, AutoScalingVmTemplateSystemTags.AFFINITY_GROUP_UUID_TOKEN);
            if (affinityGroup != null) {
                SystemTagCreator creator = AutoScalingVmTemplateSystemTags.AFFINITY_GROUP_UUID.newSystemTagCreator(templateVO.getUuid());
                creator.setTagByTokens(map(e(AutoScalingVmTemplateSystemTags.AFFINITY_GROUP_UUID_TOKEN, affinityGroup)));
                creator.inherent = false;
                creator.recreate = true;
                creator.create();
            }
        }

        event.setInventory(AutoScalingVmTemplateInventory.valueOf(templateVO));
        bus.publish(event);
    }

    @Override
    public void beforeDeleteAffinityGroup(String affinityGroupUuid) {
        List<SystemTagVO> systemTagVOS = Q.New(SystemTagVO.class)
                .eq(SystemTagVO_.resourceType, AutoScalingVmTemplateVO.class.getSimpleName())
                .like(SystemTagVO_.tag, AutoScalingVmTemplateSystemTags.AFFINITY_GROUP_UUID.instantiateTag(
                        map(e(AutoScalingVmTemplateSystemTags.AFFINITY_GROUP_UUID_TOKEN, String.format("%%%s%%", affinityGroupUuid)))
                )).eq(SystemTagVO_.type, TagType.System)
                .list();
        dbf.removeCollection(systemTagVOS, SystemTagVO.class);

        List<SystemTagVO> vmSchedulingTagVOS = Q.New(SystemTagVO.class)
                .eq(SystemTagVO_.resourceType, AutoScalingVmTemplateVO.class.getSimpleName())
                .like(SystemTagVO_.tag, AutoScalingVmTemplateSystemTags.VM_SCHEDULING_RULE_GROUP_UUID.instantiateTag(
                        map(e(AutoScalingVmTemplateSystemTags.VM_SCHEDULING_RULE_GROUP_UUID_TOKEN, String.format("%%%s%%", affinityGroupUuid)))
                )).eq(SystemTagVO_.type, TagType.System)
                .list();
        dbf.removeCollection(vmSchedulingTagVOS, SystemTagVO.class);
    }

    @Override
    public void afterDeleteAffinityGroup(String affinityGroupUuid) {

    }
}
