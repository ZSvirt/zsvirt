package org.zstack.autoscaling.group;

import org.apache.commons.collections.ListUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.autoscaling.AutoScalingConstants;
import org.zstack.autoscaling.AutoScalingGlobalConfig;
import org.zstack.autoscaling.AutoScalingGroup;
import org.zstack.autoscaling.AutoScalingManager;
import org.zstack.autoscaling.group.activity.*;
import org.zstack.autoscaling.group.activity.action.*;
import org.zstack.autoscaling.group.instance.*;
import org.zstack.autoscaling.group.instance.vm.GetRemoveTargetVmInstanceListMsg;
import org.zstack.autoscaling.group.rule.*;
import org.zstack.autoscaling.group.rule.trigger.*;
import org.zstack.autoscaling.template.*;
import org.zstack.compute.vm.DeleteVmGC;
import org.zstack.compute.vm.VmSystemTags;
import org.zstack.core.Platform;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cascade.CascadeConstant;
import org.zstack.core.cascade.CascadeFacade;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.db.SQLBatch;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.configuration.DiskOfferingState;
import org.zstack.header.configuration.DiskOfferingVO;
import org.zstack.header.configuration.InstanceOfferingState;
import org.zstack.header.configuration.InstanceOfferingVO;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NopeCompletion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.image.ImageConstant;
import org.zstack.header.image.ImageState;
import org.zstack.header.image.ImageStatus;
import org.zstack.header.image.ImageVO;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.network.l3.L3NetworkState;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.vm.*;
import org.zstack.header.volume.*;
import org.zstack.identity.AccountManager;
import org.zstack.network.securitygroup.SecurityGroupConstant;
import org.zstack.network.securitygroup.SecurityGroupVO;
import org.zstack.network.service.lb.AddVmNicToLoadBalancerMsg;
import org.zstack.network.service.lb.LoadBalancerConstants;
import org.zstack.network.service.lb.LoadBalancerListenerVO;
import org.zstack.network.service.lb.LoadBalancerListenerVO_;
import org.zstack.storage.volume.VolumeSystemTags;
import org.zstack.tag.TagManager;
import org.zstack.utils.ObjectUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;
import org.zstack.zwatch.ZWatchConstants;
import org.zstack.zwatch.api.GetMetricDataMsg;
import org.zstack.zwatch.api.GetMetricDataReply;
import org.zstack.zwatch.datatype.Datapoint;
import org.zstack.zwatch.datatype.Label;
import org.zstack.zwatch.namespace.VmNamespace;

import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

import static org.zstack.autoscaling.AutoScalingConstants.AutoScalingGroupInstance.PROTECTION_STRATEGY_PROTECTED;
import static org.zstack.autoscaling.template.AutoScalingVmTemplateSystemTags.LOAD_BALANCER_LISTENER_UUIDS_TOKEN;
import static org.zstack.core.Platform.*;
import static org.zstack.utils.CollectionDSL.*;
import static org.zstack.utils.CollectionUtils.transform;

/**
 * Created by lining on 2018/9/13.
 */

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class AutoScalingGroupBase implements AutoScalingGroup {
    protected static final CLogger logger = Utils.getLogger(AutoScalingGroupBase.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ThreadFacade thdf;
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private AutoScalingManager autoScalingManager;
    @Autowired
    private transient AccountManager acntMgr;
    @Autowired
    private TagManager tagMgr;
    @Autowired
    protected CascadeFacade casf;

    protected AutoScalingGroupVO self;
    protected AutoScalingGroupVO originalCopy;
    private String syncThreadName;
    private String ruleSyncThreadName = "autoScalingRule";
    private String ruleTriggerSyncThreadName = "autoScalingRuleTrigger";

    protected AutoScalingGroupVO getSelf() {
        return self;
    }

    protected AutoScalingGroupInventory getSelfInventory() {
        return AutoScalingGroupInventory.valueOf(self);
    }

    public AutoScalingGroupBase(AutoScalingGroupVO vo) {
        this.self = vo;
        this.syncThreadName = "autoScalingGroup-" + vo.getUuid();
        this.originalCopy = ObjectUtils.newAndCopy(vo, vo.getClass());
    }

    @Override
    @MessageSafe
    public void handleMessage(final Message msg) {
        if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage) msg);
        } else {
            handleLocalMessage(msg);
        }
    }

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APICreateAutoScalingGroupAddingNewInstanceRuleMsg) {
            handle((APICreateAutoScalingGroupAddingNewInstanceRuleMsg) msg);
        } else if (msg instanceof APICreateAutoScalingGroupRemovalInstanceRuleMsg) {
            handle((APICreateAutoScalingGroupRemovalInstanceRuleMsg) msg);
        } else if (msg instanceof APIUpdateAutoScalingGroupRemovalInstanceRuleMsg) {
            handle((APIUpdateAutoScalingGroupRemovalInstanceRuleMsg) msg);
        } else if (msg instanceof APIUpdateAutoScalingGroupAddingNewInstanceRuleMsg) {
            handle((APIUpdateAutoScalingGroupAddingNewInstanceRuleMsg) msg);
        } else if (msg instanceof APIUpdateAutoScalingRuleMsg) {
            handle((APIUpdateAutoScalingRuleMsg) msg);
        } else if (msg instanceof APIUpdateAutoScalingGroupMsg) {
            handle((APIUpdateAutoScalingGroupMsg) msg);
        } else if (msg instanceof APIDetachAutoScalingTemplateFromGroupMsg) {
            handle((APIDetachAutoScalingTemplateFromGroupMsg) msg);
        } else if (msg instanceof APIDeleteAutoScalingRuleMsg) {
            handle((APIDeleteAutoScalingRuleMsg) msg);
        } else if (msg instanceof APIDeleteAutoScalingRuleTriggerMsg) {
            handle((APIDeleteAutoScalingRuleTriggerMsg) msg);
        } else if (msg instanceof APIDeleteAutoScalingGroupInstanceMsg) {
            handle((APIDeleteAutoScalingGroupInstanceMsg) msg);
        } else if (msg instanceof APIChangeAutoScalingGroupStateMsg) {
            handle((APIChangeAutoScalingGroupStateMsg) msg);
        } else if (msg instanceof APIDeleteAutoScalingGroupMsg) {
            handle((APIDeleteAutoScalingGroupMsg) msg);
        } else if (msg instanceof APIExecuteAutoScalingRuleMsg) {
            handle((APIExecuteAutoScalingRuleMsg) msg);
        } else if (msg instanceof APIUpdateAutoScalingGroupInstanceMsg) {
            handle((APIUpdateAutoScalingGroupInstanceMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handleLocalMessage(Message msg) {
        if (msg instanceof CreateAutoScalingGroupActivityMsg) {
            handle((CreateAutoScalingGroupActivityMsg) msg);
        } else if (msg instanceof AutoScalingGroupCreateInstancesActionMsg) {
            handle((AutoScalingGroupCreateInstancesActionMsg) msg);
        } else if (msg instanceof AutoScalingGroupCreateVmInstancesMsg) {
            handle((AutoScalingGroupCreateVmInstancesMsg) msg);
        } else if (msg instanceof AutoScalingGroupRemoveInstancesActionMsg) {
            handle((AutoScalingGroupRemoveInstancesActionMsg) msg);
        } else if (msg instanceof AutoScalingGroupRemoveVmInstancesMsg) {
            handle((AutoScalingGroupRemoveVmInstancesMsg) msg);
        } else if (msg instanceof TriggerAutoScalingGroupRuleMsg) {
            handle((TriggerAutoScalingGroupRuleMsg) msg);
        } else if (msg instanceof DeleteAutoScalingGroupInstanceMsg) {
            handle((DeleteAutoScalingGroupInstanceMsg) msg);
        } else if (msg instanceof DeleteAutoScalingRuleTriggerMsg) {
            handle((DeleteAutoScalingRuleTriggerMsg) msg);
        } else if (msg instanceof DeleteAutoScalingRuleMsg) {
            handle((DeleteAutoScalingRuleMsg) msg);
        } else if (msg instanceof DetachAutoScalingTemplateFromGroupMsg) {
            handle((DetachAutoScalingTemplateFromGroupMsg) msg);
        } else if (msg instanceof DeleteAutoScalingGroupActivityMsg) {
            handle((DeleteAutoScalingGroupActivityMsg) msg);
        } else if (msg instanceof DeleteAutoScalingGroupMsg) {
            handle((DeleteAutoScalingGroupMsg) msg);
        } else if (msg instanceof GetRemoveTargetVmInstanceListMsg) {
            handle((GetRemoveTargetVmInstanceListMsg) msg);
        } else if (msg instanceof GetRemoveTargetInstanceListMsg) {
            handle((GetRemoveTargetInstanceListMsg) msg);
        } else if (msg instanceof ExecuteAutoScalingGroupRuleMsg) {
            handle((ExecuteAutoScalingGroupRuleMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(APICreateAutoScalingGroupAddingNewInstanceRuleMsg msg) {
        APICreateAutoScalingRuleEvent event = new APICreateAutoScalingRuleEvent(msg.getId());

        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getName() {
                return String.format("add-rule-to-autoScalingGroup-%s", msg.getAutoScalingGroupUuid());
            }

            @Override
            public String getSyncSignature() {
                return syncThreadName;
            }

            @Override
            public void run(SyncTaskChain chain) {
                AddingNewInstanceRuleVO vo = new AddingNewInstanceRuleVO();
                vo.setUuid(msg.getResourceUuid() == null ? Platform.getUuid() : msg.getResourceUuid());
                vo.setScalingGroupUuid(self.getUuid());
                vo.setName(msg.getName());
                vo.setDescription(msg.getDescription());
                vo.setType(AutoScalingRuleType.valueOf(msg.getType()));
                vo.setCooldown(msg.getCooldown());
                vo.setAdjustmentType(AdjustmentType.valueOf(msg.getAdjustmentType()));
                vo.setAdjustmentValue(msg.getAdjustmentValue());
                vo.setState(AutoScalingRuleState.Enabled);
                vo.setStatus(AutoScalingRuleStatus.Created);
                vo.setCreateDate(new Timestamp(System.currentTimeMillis()));
                vo = dbf.persistAndRefresh(vo);

                event.setInventory(AddingNewVmRuleInventory.valueOf(vo));
                bus.publish(event);
                chain.next();
            }
        });
    }

    private void handle(APICreateAutoScalingGroupRemovalInstanceRuleMsg msg) {
        APICreateAutoScalingRuleEvent event = new APICreateAutoScalingRuleEvent(msg.getId());

        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getName() {
                return String.format("add-rule-to-autoScalingGroup-%s", msg.getAutoScalingGroupUuid());
            }

            @Override
            public String getSyncSignature() {
                return syncThreadName;
            }

            @Override
            public void run(SyncTaskChain chain) {
                RemovalInstanceRuleVO vo = new RemovalInstanceRuleVO();
                vo.setUuid(msg.getResourceUuid() == null ? Platform.getUuid() : msg.getResourceUuid());
                vo.setScalingGroupUuid(self.getUuid());
                vo.setName(msg.getName());
                vo.setDescription(msg.getDescription());
                vo.setType(AutoScalingRuleType.valueOf(msg.getType()));
                vo.setCooldown(msg.getCooldown());
                vo.setAdjustmentType(AdjustmentType.valueOf(msg.getAdjustmentType()));
                vo.setAdjustmentValue(msg.getAdjustmentValue());
                vo.setState(AutoScalingRuleState.Enabled);
                vo.setStatus(AutoScalingRuleStatus.Created);
                vo.setRemovalPolicy(RemovalPolicy.valueOf(msg.getRemovalPolicy()));
                vo.setCreateDate(new Timestamp(System.currentTimeMillis()));
                vo = dbf.persistAndRefresh(vo);

                event.setInventory(RemovalInstanceRuleInventory.valueOf(vo));
                bus.publish(event);
                chain.next();
            }
        });
    }

    private void handle(APIUpdateAutoScalingGroupMsg msg) {
        APIUpdateAutoScalingGroupEvent event = new APIUpdateAutoScalingGroupEvent(msg.getId());

        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getName() {
                return String.format("update-autoScalingGroup-%s", msg.getUuid());
            }

            @Override
            public String getSyncSignature() {
                return syncThreadName;
            }

            @Override
            public void run(SyncTaskChain chain) {
                AutoScalingGroupVO vo = Q.New(AutoScalingGroupVO.class)
                        .eq(AutoScalingGroupVO_.uuid, msg.getUuid()).find();

                if (msg.getName() != null) {
                    vo.setName(msg.getName());
                }

                if (msg.getDescription() != null) {
                    vo.setDescription(msg.getDescription());
                }

                if (msg.getMinResourceSize() != null) {
                    vo.setMinResourceSize(msg.getMinResourceSize());
                }

                if (msg.getMaxResourceSize() != null) {
                    vo.setMaxResourceSize(msg.getMaxResourceSize());
                }

                if (msg.getRemovalPolicy() != null) {
                    vo.setRemovalPolicy(RemovalPolicy.valueOf(msg.getRemovalPolicy()));
                }

                vo = dbf.updateAndRefresh(vo);
                event.setInventory(AutoScalingGroupInventory.valueOf(vo));

                bus.publish(event);
                chain.next();
            }
        });
    }

    private void handle(CreateAutoScalingGroupActivityMsg msg) {
        fixActivityRetention(msg.getAutoScalingGroupUuid());

        checkScalingGroupState();
        CreateAutoScalingGroupActivityReply reply = new CreateAutoScalingGroupActivityReply();
        reply.setAutoScalingGroupActivityUuid(msg.getActivityUuid());

        AutoScalingGroupActivityVO activityVO = new AutoScalingGroupActivityVO();
        activityVO.setUuid(msg.getActivityUuid());
        activityVO.setName(msg.getName());
        activityVO.setActivityAction(AutoScalingGroupActivityAction.valueOf(msg.getActivityAction()));
        activityVO.setScalingGroupUuid(msg.getScalingGroupUuid());
        activityVO.setDescription(msg.getDescription());
        activityVO.setCause(AutoScalingGroupActivityCause.valueOf(msg.getCause()));
        activityVO.setScalingGroupRuleUuid(msg.getScalingGroupRuleUuid());
        activityVO.setStatus(AutoScalingGroupActivityStatus.Created);
        activityVO = dbf.persistAndRefresh(activityVO);

        // Concurrency control
        if (!AutoScalingGroupActivityConcurrencyControl.addToken(self.getUuid())) {
            ErrorCode errorCode = operr("Only one scaling activity can be executed in the same scaling group at the same time.");

            SQL.New(AutoScalingGroupActivityVO.class)
                    .set(AutoScalingGroupActivityVO_.status, AutoScalingGroupActivityStatus.Rejected)
                    .set(AutoScalingGroupActivityVO_.activityActionResultMessage, errorCode.toString())
                    .set(AutoScalingGroupActivityVO_.endDate, new Timestamp(System.currentTimeMillis()))
                    .eq(AutoScalingGroupActivityVO_.uuid, activityVO.getUuid())
                    .update();

            reply.setError(errorCode);
            bus.reply(msg, reply);
            return;
        }

        final String activityUuid = activityVO.getUuid();
        AutoScalingGroupActivityActionMessage actionMessage = (AutoScalingGroupActivityActionMessage) msg.getActionMessage();
        bus.send(actionMessage, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply actionReply) {
                AutoScalingGroupActivityConcurrencyControl.removeToken(self.getUuid());

                if (!actionReply.isSuccess() && actionReply.getError() != null) {
                    SQL.New(AutoScalingGroupActivityVO.class)
                            .set(AutoScalingGroupActivityVO_.status, AutoScalingGroupActivityStatus.Failed)
                            .set(AutoScalingGroupActivityVO_.activityActionResultMessage, actionReply.getError().toString())
                            .set(AutoScalingGroupActivityVO_.endDate, new Timestamp(System.currentTimeMillis()))
                            .eq(AutoScalingGroupActivityVO_.uuid, activityUuid)
                            .update();
                }

                reply.setActionReply(actionReply);
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(AutoScalingGroupCreateInstancesActionMsg msg) {
        String groupUuid = msg.getAutoScalingGroupUuid();

        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getName() {
                return String.format("add-new-instance-to-scalingGroup-%s", groupUuid);
            }

            @Override
            public String getSyncSignature() {
                return syncThreadName + "-activity-action";
            }

            @Override
            public void run(SyncTaskChain chain) {
                AutoScalingCreateInstancesActionReply reply = new AutoScalingCreateInstancesActionReply();

                doCreateInstancesAction(msg, new ReturnValueCompletion<CreateInstancesResult>(chain, msg) {
                    @Override
                    public void success(CreateInstancesResult result) {
                        if (result.getErrorCodes() == null || result.getErrorCodes().size() == 0) {
                            SQL.New(AutoScalingGroupActivityVO.class).
                                    set(AutoScalingGroupActivityVO_.status, AutoScalingGroupActivityStatus.Successful).
                                    set(AutoScalingGroupActivityVO_.endDate, new Timestamp(System.currentTimeMillis())).
                                    set(AutoScalingGroupActivityVO_.instanceUuids, StringUtils.join(result.getInstanceUuids(), ","))
                                    .eq(AutoScalingGroupActivityVO_.uuid, msg.getAutoScalingGroupActivityUuid()).
                                    update();
                        } else {
                            String resultMessage = JSONObjectUtil.toJsonString(result.getErrorCodes());
                            SQL.New(AutoScalingGroupActivityVO.class).
                                    set(AutoScalingGroupActivityVO_.status, AutoScalingGroupActivityStatus.Warning).
                                    set(AutoScalingGroupActivityVO_.activityActionResultMessage, resultMessage).
                                    set(AutoScalingGroupActivityVO_.endDate, new Timestamp(System.currentTimeMillis()))
                                    .eq(AutoScalingGroupActivityVO_.uuid, msg.getAutoScalingGroupActivityUuid()).
                                    update();
                        }

                        reply.setResult(result);
                        bus.reply(msg, reply);

                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        reply.setError(errorCode);
                        bus.reply(msg, reply);

                        chain.next();
                    }
                });
            }
        });
    }

    private void doCreateInstancesAction(final AutoScalingGroupCreateInstancesActionMsg msg, final ReturnValueCompletion<CreateInstancesResult> completion) {
        checkScalingGroupState();
        checkScalingGroupVmTemplate();
        checkScalingGroupNumberOfInstances(msg);

        if (msg.getAddingInstanceSize() <= 0) {
            completion.fail(operr("The number of instances exceeds the limit"));
            return;
        }

        for (AutoScalingCreateInstancesActionExtensionPoint ext : pluginRgty.getExtensionList(AutoScalingCreateInstancesActionExtensionPoint.class)) {
            ext.beforeCreateInstances(msg);
        }

        SQL.New(AutoScalingGroupActivityVO.class).
                set(AutoScalingGroupActivityVO_.status, AutoScalingGroupActivityStatus.InProgress).
                eq(AutoScalingGroupActivityVO_.uuid, msg.getAutoScalingGroupActivityUuid()).
                update();

        List<String> instanceUuids = new ArrayList<>();
        int size = msg.getAddingInstanceSize();
        while (size-- > 0) {
            instanceUuids.add(Platform.getUuid());
        }

        ScalingGroupInstanceFactory factory = autoScalingManager.getScalingGroupInstanceFactory(self.getScalingResourceType());
        AutoScalingGroupCreateInstancesMsg createInstancesMsg = factory.getAutoScalingCreateInstanceMsg(self.getUuid(), msg.getAddingInstanceSize());
        createInstancesMsg.setInstanceUuids(instanceUuids);
        bus.makeTargetServiceIdByResourceUuid(createInstancesMsg, AutoScalingConstants.SERVICE_ID, self.getUuid());

        for (AutoScalingCreateInstancesActionExtensionPoint ext : pluginRgty.getExtensionList(AutoScalingCreateInstancesActionExtensionPoint.class)) {
            ext.preCreateInstances(msg);
        }

        addInstancesToScalingGroup(msg.getAutoScalingGroupActivityUuid(), instanceUuids);

        bus.send(createInstancesMsg, new CloudBusCallBack(msg, completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    handleVmsThatFailedToCreate(instanceUuids);
                    completion.fail(reply.getError());
                    return;
                }

                AutoScalingCreateInstancesReply r = (AutoScalingCreateInstancesReply) reply;
                CreateInstancesResult result = r.getResult();

                if (result.getInstanceUuids() != null && result.getInstanceUuids().size() > 0) {
                    SQL.New(AutoScalingGroupInstanceVO.class)
                            .eq(AutoScalingGroupInstanceVO_.scalingGroupUuid, self.getUuid())
                            .in(AutoScalingGroupInstanceVO_.instanceUuid, result.getInstanceUuids())
                            .set(AutoScalingGroupInstanceVO_.status, AutoScalingGroupInstanceStatus.InService)
                            .update();
                }

                List<String> failedInstanceUuids = new ArrayList<>(instanceUuids);
                failedInstanceUuids.removeAll(result.getInstanceUuids());
                if (!failedInstanceUuids.isEmpty()) {
                    handleVmsThatFailedToCreate(failedInstanceUuids);
                }

                for (AutoScalingCreateInstancesActionExtensionPoint ext : pluginRgty.getExtensionList(AutoScalingCreateInstancesActionExtensionPoint.class)) {
                    ext.afterCreateInstancesSuccess(msg, result);
                }

                completion.success(result);
            }
        });
    }

    private void handleVmsThatFailedToCreate(List<String> instanceUuids) {
        List<String> vmInstancesInDb = Q.New(VmInstanceVO.class)
                .select(VmInstanceVO_.uuid)
                .in(VmInstanceVO_.uuid, instanceUuids)
                .listValues();

        List<String> needToDeleteInstanceFromAutoScalingGroup = new ArrayList<>(instanceUuids);
        needToDeleteInstanceFromAutoScalingGroup.removeAll(vmInstancesInDb);

        if (!needToDeleteInstanceFromAutoScalingGroup.isEmpty()) {
            logger.info(String.format("Delete vms%s that failed to create", instanceUuids.toString()));
            SQL.New(AutoScalingGroupInstanceVO.class)
                    .eq(AutoScalingGroupInstanceVO_.scalingGroupUuid, self.getUuid())
                    .in(AutoScalingGroupInstanceVO_.instanceUuid, needToDeleteInstanceFromAutoScalingGroup)
                    .delete();
        }

        if (!vmInstancesInDb.isEmpty()) {
            logger.warn(String.format("Update vms%s still in the creation to be unhealthy", vmInstancesInDb));
            SQL.New(AutoScalingGroupInstanceVO.class)
                    .in(AutoScalingGroupInstanceVO_.instanceUuid, vmInstancesInDb)
                    .eq(AutoScalingGroupInstanceVO_.scalingGroupUuid, self.getUuid())
                    .set(AutoScalingGroupInstanceVO_.healthStatus, AutoScalingGroupInstanceHealthStatus.Unhealthy)
                    .update();
        }
    }

    private void handle(AutoScalingGroupCreateVmInstancesMsg msg) {
        AutoScalingCreateInstancesReply reply = new AutoScalingCreateInstancesReply();

        final List<CreateVmInstanceMsg> createVmInstanceMsgs = makeCreateVmInstanceMsgList(msg.getAddingInstanceSize(), msg.getInstanceUuids());

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName("create-vm-instances-and-add-vm-nics-To-load-balancer");
        chain.then(new ShareFlow() {

            ErrorCodeList errors;

            @Override
            public void setup() {
                List<VmInstanceInventory> newVmInventories = Collections.synchronizedList(new LinkedList<VmInstanceInventory>());
                List<ErrorCode> addVmNicToLoadBalancerErrorCodes = Collections.synchronizedList(new LinkedList<ErrorCode>());
                List<ErrorCode> addVmNicToSecurityGroupErrorCodes = Collections.synchronizedList(new LinkedList<ErrorCode>());
                Set<String> needRollbackVms = Collections.synchronizedSet(new HashSet<>());
                int parallelism = 10;

                flow(new Flow() {
                    String __name__ = "create-vm-instances";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        new While<>(createVmInstanceMsgs).step((createVmInstanceMsg, completion) -> {
                            bus.send(createVmInstanceMsg, new CloudBusCallBack(completion) {
                                @Override
                                public void run(MessageReply rly) {
                                    if (rly.isSuccess()) {
                                        CreateVmInstanceReply r = (CreateVmInstanceReply) rly;
                                        newVmInventories.add(r.getInventory());
                                    } else {
                                        trigger.setError(rly.getError());
                                    }

                                    completion.done();
                                }
                            });
                        }, parallelism).run(new WhileDoneCompletion(trigger) {
                            @Override
                            public void done(ErrorCodeList errorCodeList) {
                                errors = errorCodeList;

                                if (errorCodeList.getCauses().size() == createVmInstanceMsgs.size()) {
                                    trigger.fail(multiErr(errorCodeList,
                                            "autoScalingGroup[%s] create vms failed completely",
                                            msg.getAutoScalingGroupUuid()));
                                    return;
                                }
                                trigger.next();
                            }
                        });
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        rollbackNewlyCreatedVmInstances(new ArrayList<>(needRollbackVms), new Completion(trigger) {
                            @Override
                            public void success() {
                                trigger.rollback();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.rollback();
                            }
                        });
                    }
                });

                flow(new Flow() {
                    String __name__ = "add-vm-nics-to-load-balancer";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        AutoScalingTemplateGroupRefVO templateGroupRefVO = Q.New(AutoScalingTemplateGroupRefVO.class)
                                .eq(AutoScalingTemplateGroupRefVO_.groupUuid, self.getUuid())
                                .find();
                        String vmTemplateUuid = templateGroupRefVO.getTemplateUuid();

                        if (!AutoScalingVmTemplateSystemTags.LOAD_BALANCER_LISTENER_UUIDS.hasTag(vmTemplateUuid)) {
                            trigger.next();
                            return;
                        }

                        String loadBalancerListenerUuidListStr = AutoScalingVmTemplateSystemTags.LOAD_BALANCER_LISTENER_UUIDS.getTokenByResourceUuid(vmTemplateUuid, LOAD_BALANCER_LISTENER_UUIDS_TOKEN);
                        String[] listenerUuids = loadBalancerListenerUuidListStr.split(AutoScalingConstants.AutoScalingTemplate.VmInstance.SEPARATOR);
                        List<LoadBalancerListenerVO> loadBalancerListeners = Q.New(LoadBalancerListenerVO.class)
                                .in(LoadBalancerListenerVO_.uuid, Arrays.asList(listenerUuids))
                                .list();
                        if (listenerUuids.length != loadBalancerListeners.size()) {
                            List<String> vmUuids = newVmInventories.stream().map(VmInstanceInventory::getUuid).collect(Collectors.toList());
                            needRollbackVms.addAll(vmUuids);
                            trigger.fail(operr("add vm nic to loadBalancer failed, No loadBalancer[uuids=%s] can be found.", loadBalancerListenerUuidListStr));
                            return;
                        }

                        List<AddVmNicToLoadBalancerMsg> addVmNicToLoadBalancerMsgs = new ArrayList<>();
                        for (VmInstanceInventory vm : newVmInventories) {
                            for (LoadBalancerListenerVO listenerVO : loadBalancerListeners) {
                                AddVmNicToLoadBalancerMsg addVmNicToLoadBalancerMsg = new AddVmNicToLoadBalancerMsg();
                                addVmNicToLoadBalancerMsg.setLoadBalancerUuid(listenerVO.getLoadBalancerUuid());
                                addVmNicToLoadBalancerMsg.setListenerUuid(listenerVO.getUuid());
                                addVmNicToLoadBalancerMsg.setVmNicUuids(vm.getVmNics().stream().map(VmNicInventory::getUuid).collect(Collectors.toList()));
                                bus.makeLocalServiceId(addVmNicToLoadBalancerMsg, LoadBalancerConstants.SERVICE_ID);
                                addVmNicToLoadBalancerMsgs.add(addVmNicToLoadBalancerMsg);
                            }
                        }

                        new While<>(addVmNicToLoadBalancerMsgs).step((addVmNicToLoadBalancerMsg, completion) -> {
                            bus.send(addVmNicToLoadBalancerMsg, new CloudBusCallBack(completion) {
                                @Override
                                public void run(MessageReply rly) {
                                    if (rly.isSuccess()) {
                                        completion.done();
                                        return;
                                    }

                                    addVmNicToLoadBalancerErrorCodes.add(rly.getError());
                                    for (VmInstanceInventory vm : newVmInventories) {
                                        if (vm.getVmNics().stream().map(VmNicInventory::getUuid).collect(Collectors.toList()).containsAll(addVmNicToLoadBalancerMsg.getVmNicUuids())) {
                                            needRollbackVms.add(vm.getUuid());
                                            break;
                                        }
                                    }
                                    completion.done();
                                }
                            });
                        }, parallelism).run(new WhileDoneCompletion(trigger) {
                            @Override
                            public void done(ErrorCodeList errorCodeList) {
                                if (newVmInventories.size() == needRollbackVms.size()) {
                                    trigger.fail(operr("autoScalingGroup[%s] add newly created vm to loadBalancer failed completely, errors are %s"
                                            ,msg.getAutoScalingGroupUuid(), JSONObjectUtil.toJsonString(addVmNicToLoadBalancerErrorCodes)));
                                    return;
                                }
                                trigger.next();
                            }
                        });
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        trigger.rollback();
                    }
                });

                flow(new Flow() {
                    String __name__ = "add-vm-nics-to-security-group";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        AutoScalingTemplateGroupRefVO templateGroupRefVO = Q.New(AutoScalingTemplateGroupRefVO.class)
                                .eq(AutoScalingTemplateGroupRefVO_.groupUuid, self.getUuid())
                                .find();
                        String vmTemplateUuid = templateGroupRefVO.getTemplateUuid();

                        if (!AutoScalingVmTemplateSystemTags.SECURITY_GROUP_UUID.hasTag(vmTemplateUuid)) {
                            trigger.next();
                            return;
                        }

                        String securityGroupUuid = AutoScalingVmTemplateSystemTags.SECURITY_GROUP_UUID.getTokenByResourceUuid(vmTemplateUuid, AutoScalingVmTemplateSystemTags.SECURITY_GROUP_UUID_TOKEN);
                        SecurityGroupVO securityGroupVO = dbf.findByUuid(securityGroupUuid, SecurityGroupVO.class);
                        if (securityGroupVO == null) {
                            List<String> vmUuids = newVmInventories.stream().map(VmInstanceInventory::getUuid).collect(Collectors.toList());
                            needRollbackVms.addAll(vmUuids);
                            trigger.fail(operr("add vm nic to securityGroup failed, No securityGroup[uuid=%s] can be found.", securityGroupUuid));
                            return;
                        }

                        List<AddVmNicToSecurityGroupMsg> addVmNicToSecurityGroupMsgs = new ArrayList<>();
                        for (VmInstanceInventory vm : newVmInventories) {
                            AddVmNicToSecurityGroupMsg addVmNicToSecurityGroupMsg = new AddVmNicToSecurityGroupMsg();
                            addVmNicToSecurityGroupMsg.setSecurityGroupUuid(securityGroupUuid);
                            addVmNicToSecurityGroupMsg.setVmNicUuids(vm.getVmNics().stream().map(VmNicInventory::getUuid).collect(Collectors.toList()));
                            bus.makeTargetServiceIdByResourceUuid(addVmNicToSecurityGroupMsg, SecurityGroupConstant.SERVICE_ID, securityGroupUuid);
                            addVmNicToSecurityGroupMsgs.add(addVmNicToSecurityGroupMsg);
                        }

                        new While<>(addVmNicToSecurityGroupMsgs).step((addVmNicToSecurityGroupMsg, completion) -> {
                            bus.send(addVmNicToSecurityGroupMsg, new CloudBusCallBack(completion) {
                                @Override
                                public void run(MessageReply rly) {
                                    if (rly.isSuccess()) {
                                        completion.done();
                                        return;
                                    }

                                    addVmNicToSecurityGroupErrorCodes.add(rly.getError());
                                    for (VmInstanceInventory vm : newVmInventories) {
                                        if (vm.getVmNics().stream().map(VmNicInventory::getUuid).collect(Collectors.toList()).containsAll(addVmNicToSecurityGroupMsg.getVmNicUuids())) {
                                            needRollbackVms.add(vm.getUuid());
                                            break;
                                        }
                                    }
                                    completion.done();
                                }
                            });
                        }, parallelism).run(new WhileDoneCompletion(trigger) {
                            @Override
                            public void done(ErrorCodeList errorCodeList) {
                                if (newVmInventories.size() == needRollbackVms.size()) {
                                    trigger.fail(operr("autoScalingGroup[%s] add newly created vm to securityGroup failed completely, errors are %s"
                                            ,msg.getAutoScalingGroupUuid(), JSONObjectUtil.toJsonString(addVmNicToSecurityGroupErrorCodes)));
                                    return;
                                }
                                trigger.next();
                            }
                        });
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        trigger.rollback();
                    }
                });

                flow(new Flow() {
                    String __name__ = "clean-need-rollback-vms";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        if (needRollbackVms.isEmpty()) {
                            trigger.next();
                            return;
                        }

                        rollbackNewlyCreatedVmInstances(new ArrayList<>(needRollbackVms), new Completion(trigger) {
                            @Override
                            public void success() {
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        });
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        trigger.rollback();
                    }
                });

                done(new FlowDoneHandler(msg) {
                    @Override
                    public void handle(Map data) {
                        CreateInstancesResult result = new CreateInstancesResult();
                        result.setErrorCodes(errors.getCauses());

                        List<String> instanceUuids = newVmInventories.stream().map(VmInstanceInventory::getUuid).collect(Collectors.toList());
                        instanceUuids.removeAll(needRollbackVms);
                        result.setInstanceUuids(instanceUuids);

                        reply.setResult(result);
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

    private void rollbackNewlyCreatedVmInstances(List<String> vmUuidList, Completion completion) {
        if (vmUuidList.isEmpty()) {
            completion.success();
            return;
        }

        destroyVmInstanceWithDataVolume(vmUuidList, new ReturnValueCompletion<RemoveInstancesResult>(completion) {
            @Override
            public void success(RemoveInstancesResult result) {
                if (vmUuidList.size() != result.getInstanceUuids().size()) {
                    List<String> failedVmUuids = new ArrayList<>();
                    failedVmUuids.addAll(vmUuidList);
                    failedVmUuids.removeAll(result.getInstanceUuids());
                    String error = JSONObjectUtil.toJsonString(result.getErrorCodes());
                    logger.warn(String.format("autoScalingGroup[%s] rollback newly created vm[uuid=%s] failed, %s",
                            self.getUuid(), failedVmUuids, error));
                }
                completion.success();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                logger.warn(String.format("autoScalingGroup[%s] rollback newly created vm[uuids=%s] failed:%n%s",
                        self.getUuid(), vmUuidList, errorCode.getReadableDetails()));
                completion.success();
            }
        });
    }

    private List<CreateVmInstanceMsg> makeCreateVmInstanceMsgList(int instanceSize, List<String> vmInstanceUuids) {
        assert instanceSize == vmInstanceUuids.size();
        List<CreateVmInstanceMsg> createVmInstanceMsgs = new ArrayList<>();

        AutoScalingTemplateGroupRefVO templateGroupRefVO = Q.New(AutoScalingTemplateGroupRefVO.class)
                .eq(AutoScalingTemplateGroupRefVO_.groupUuid, self.getUuid())
                .find();

        AutoScalingVmTemplateVO vmTemplateVO = dbf.findByUuid(templateGroupRefVO.getTemplateUuid(), AutoScalingVmTemplateVO.class);
        if (vmTemplateVO == null) {
            throw new OperationFailureException(operr("A resource can not be found, details: resource[uuid:%s, type:AutoScalingVmTemplateVO] not found", templateGroupRefVO.getTemplateUuid()));
        }

        InstanceOfferingVO instanceOfferingVO = dbf.findByUuid(vmTemplateVO.getVmInstanceOfferingUuid(), InstanceOfferingVO.class);
        if (instanceOfferingVO == null) {
            throw new OperationFailureException(operr("A resource can not be found, details: resource[uuid:%s, type:InstanceOfferingVO] not found", vmTemplateVO.getVmInstanceOfferingUuid()));
        }
        if (instanceOfferingVO.getState() == InstanceOfferingState.Disabled) {
            throw new OperationFailureException(operr("instance offering[uuid:%s] is Disabled, can't create vm from it", instanceOfferingVO.getUuid()));
        }
        if (!instanceOfferingVO.getType().equals(VmInstanceConstant.USER_VM_TYPE)){
            throw new OperationFailureException(operr("instance offering[uuid:%s, type:%s] is not UserVm type, can't create vm from it", instanceOfferingVO.getUuid(), instanceOfferingVO.getType()));
        }

        ImageVO imageVO = dbf.findByUuid(vmTemplateVO.getImageUuid(), ImageVO.class);
        if (imageVO == null) {
            throw new OperationFailureException(operr("A resource can not be found, details: resource[uuid:%s, type:ImageVO] not found", vmTemplateVO.getImageUuid()));
        }
        if (imageVO.getState() == ImageState.Disabled) {
            throw new OperationFailureException(operr("image[uuid:%s] is Disabled, can't create vm from it", imageVO.getUuid()));
        }
        if (imageVO.getStatus() != ImageStatus.Ready) {
            throw new OperationFailureException(operr("image[uuid:%s] is not ready yet, can't create vm from it", imageVO.getUuid()));
        }
        if (imageVO.getMediaType() != ImageConstant.ImageMediaType.RootVolumeTemplate && imageVO.getMediaType() != ImageConstant.ImageMediaType.ISO) {
            throw new OperationFailureException(argerr("image[uuid:%s] is of mediaType: %s, only RootVolumeTemplate and ISO can be used to create vm", imageVO.getUuid(), imageVO.getMediaType()));
        }
        if (imageVO.getMediaType() == ImageConstant.ImageMediaType.ISO && vmTemplateVO.getRootDiskOfferingUuid() == null) {
            throw new OperationFailureException(argerr("rootDiskOfferingUuid cannot be null when image mediaType is ISO"));
        }
        boolean isSystemImage = imageVO.isSystem();
        if (isSystemImage) {
            throw new OperationFailureException(argerr("image[uuid:%s] is system image, can't be used to create user vm", imageVO.getUuid()));
        }

        if (vmTemplateVO.getDataDiskOfferingUuids() != null) {
            String[] diskOfferings = vmTemplateVO.getDataDiskOfferingUuids().split(AutoScalingConstants.AutoScalingTemplate.VmInstance.SEPARATOR);
            for (String diskOffering : diskOfferings) {
                DiskOfferingVO diskOfferingVO = dbf.findByUuid(diskOffering, DiskOfferingVO.class);
                if (diskOfferingVO == null) {
                    throw new OperationFailureException(operr("A resource can not be found, details: resource[uuid:%s, type:DiskOfferingVO] not found", diskOffering));
                }

                if (diskOfferingVO.getState() == DiskOfferingState.Disabled) {
                    throw new OperationFailureException(operr("disk offerings[uuids:%s] are Disabled, can not create vm from it", diskOfferingVO.getUuid()));
                }
            }
        }

        List<String> l3Uuids = Arrays.asList(vmTemplateVO.getL3NetworkUuids().split(AutoScalingConstants.AutoScalingTemplate.VmInstance.SEPARATOR));
        for (String l3Uuid : l3Uuids) {
            L3NetworkVO l3NetworkVO = dbf.findByUuid(l3Uuid, L3NetworkVO.class);
            if (l3NetworkVO == null) {
                throw new OperationFailureException(operr("A resource can not be found, details: resource[uuid:%s, type:L3NetworkVO] not found", l3Uuid));
            }
            Boolean system = l3NetworkVO.isSystem();
            L3NetworkState state = l3NetworkVO.getState();
            if (state != L3NetworkState.Enabled) {
                throw new OperationFailureException(operr("l3Network[uuid:%s] is Disabled, can not create vm on it", l3Uuid));
            }
            if (system) {
                throw new OperationFailureException(operr("l3Network[uuid:%s] is system network, can not create user vm on it", l3Uuid));
            }
        }
        if (vmTemplateVO.getDefaultL3NetworkUuid() == null && l3Uuids.size() != 1) {
            throw new OperationFailureException(argerr("there are more than one L3 network specified in l3NetworkUuids, but defaultL3NetworkUuid is null"));
        } else if (vmTemplateVO.getDefaultL3NetworkUuid() == null && l3Uuids.size() == 1) {
            vmTemplateVO.setDefaultL3NetworkUuid(l3Uuids.get(0));
        } else if (vmTemplateVO.getDefaultL3NetworkUuid() != null && !l3Uuids.contains(vmTemplateVO.getDefaultL3NetworkUuid())) {
            throw new OperationFailureException(argerr("defaultL3NetworkUuid[uuid:%s] is not in l3NetworkUuids %s", vmTemplateVO.getDefaultL3NetworkUuid(), l3Uuids));
        }

        List<String> vmTemplateSystemTags = new ArrayList<>();
        if (AutoScalingVmTemplateSystemTags.HOSTNAME.hasTag(vmTemplateVO.getUuid())) {
            vmTemplateSystemTags.add(AutoScalingVmTemplateSystemTags.HOSTNAME.getTag(vmTemplateVO.getUuid()));
        }
        if (AutoScalingVmTemplateSystemTags.WINDOWS_VOLUME_ON_VIRTIO.hasTag(vmTemplateVO.getUuid())) {
            vmTemplateSystemTags.add(AutoScalingVmTemplateSystemTags.WINDOWS_VOLUME_ON_VIRTIO.getTag(vmTemplateVO.getUuid()));
        }
        if (AutoScalingVmTemplateSystemTags.USERDATA.hasTag(vmTemplateVO.getUuid())) {
            vmTemplateSystemTags.add(AutoScalingVmTemplateSystemTags.USERDATA.getTag(vmTemplateVO.getUuid()));
        }
        if (AutoScalingVmTemplateSystemTags.SSHKEY.hasTag(vmTemplateVO.getUuid())) {
            vmTemplateSystemTags.add(AutoScalingVmTemplateSystemTags.SSHKEY.getTag(vmTemplateVO.getUuid()));
        }
        if (AutoScalingVmTemplateSystemTags.ROOT_PASSWORD.hasTag(vmTemplateVO.getUuid())) {
            vmTemplateSystemTags.add(AutoScalingVmTemplateSystemTags.ROOT_PASSWORD.getTag(vmTemplateVO.getUuid()));
        }
        if (AutoScalingVmTemplateSystemTags.BOOT_ORDER.hasTag(vmTemplateVO.getUuid())) {
            vmTemplateSystemTags.add(AutoScalingVmTemplateSystemTags.BOOT_ORDER.getTag(vmTemplateVO.getUuid()));
        }
        if (AutoScalingVmTemplateSystemTags.CDROM_BOOT_ONCE.hasTag(vmTemplateVO.getUuid())) {
            vmTemplateSystemTags.add(AutoScalingVmTemplateSystemTags.CDROM_BOOT_ONCE.getTag(vmTemplateVO.getUuid()));
        }
        if (AutoScalingVmTemplateSystemTags.CONSOLE_PASSWORD.hasTag(vmTemplateVO.getUuid())) {
            vmTemplateSystemTags.add(AutoScalingVmTemplateSystemTags.CONSOLE_PASSWORD.getTag(vmTemplateVO.getUuid()));
        }
        if (AutoScalingVmTemplateSystemTags.AFFINITY_GROUP_UUID.hasTag(vmTemplateVO.getUuid())) {
            vmTemplateSystemTags.add(AutoScalingVmTemplateSystemTags.AFFINITY_GROUP_UUID.getTag(vmTemplateVO.getUuid()));
        }

        if (AutoScalingVmTemplateSystemTags.VM_SCHEDULING_RULE_GROUP_UUID.hasTag(vmTemplateVO.getUuid())) {
            vmTemplateSystemTags.add(AutoScalingVmTemplateSystemTags.VM_SCHEDULING_RULE_GROUP_UUID.getTag(vmTemplateVO.getUuid()));
        }

        vmTemplateSystemTags.add(VmSystemTags.AUTO_SCALING_GROUP_UUID.instantiateTag(
                map(e(VmSystemTags.AUTO_SCALING_GROUP_UUID, self.getUuid()))));

        while (instanceSize-- > 0) {
            CreateVmInstanceMsg msg = new CreateVmInstanceMsg();
            String vmUuid = vmInstanceUuids.get(instanceSize);
            msg.setResourceUuid(vmUuid);
            msg.setName(String.format("asg-%s-%s-%s", self.getName(), vmTemplateVO.getVmInstanceName(), vmUuid.substring(0, 5)));
            msg.setImageUuid(vmTemplateVO.getImageUuid());
            msg.setAllocatorStrategy(instanceOfferingVO.getAllocatorStrategy());
            msg.setAccountUuid(acntMgr.getOwnerAccountUuidOfResource(self.getUuid()));
            List<VmNicSpec> nicSpecs = new ArrayList<>();
            for (String uuid : l3Uuids) {
                L3NetworkInventory l3Invs = L3NetworkInventory.valueOf(dbf.findByUuid(uuid, L3NetworkVO.class));
                nicSpecs.add(new VmNicSpec(l3Invs));
            }
            msg.setL3NetworkSpecs(nicSpecs);
            msg.setDefaultL3NetworkUuid(vmTemplateVO.getDefaultL3NetworkUuid());
            msg.setType(vmTemplateVO.getVmInstanceType());
            msg.setZoneUuid(vmTemplateVO.getVmInstanceZoneUuid());
            msg.setInstanceOfferingUuid(vmTemplateVO.getVmInstanceOfferingUuid());
            msg.setMemorySize(instanceOfferingVO.getMemorySize());
            msg.setCpuNum(instanceOfferingVO.getCpuNum());
            msg.setDescription(vmTemplateVO.getVmInstanceDescription());
            msg.setPrimaryStorageUuidForRootVolume(vmTemplateVO.getPrimaryStorageUuidForRootVolume());

            if (vmTemplateVO.getRootDiskOfferingUuid() != null) {
                final DiskAO rootDisk = DiskAO.rootDisk().withImage(vmTemplateVO.getImageUuid());
                rootDisk.setDiskOfferingUuid(vmTemplateVO.getRootDiskOfferingUuid());
                msg.setRootDisk(rootDisk);
            }

            if (!vmTemplateSystemTags.isEmpty()) {
                msg.setSystemTags(vmTemplateSystemTags);
            }
            if (vmTemplateVO.getDataDiskOfferingUuids() != null) {
                final String[] uuidList = vmTemplateVO.getDataDiskOfferingUuids()
                        .split(AutoScalingConstants.AutoScalingTemplate.VmInstance.SEPARATOR);

                List<DiskAO> deprecatedDiskSpecs = new ArrayList<>();
                for (String uuid : uuidList) {
                    DiskAO disk = DiskAO.nonRootDisk();
                    disk.setDiskOfferingUuid(uuid);
                    disk.setSystemTags(Arrays.asList(VolumeSystemTags.AUTO_SCALING_GROUP_UUID.instantiateTag(
                                map(e(VolumeSystemTags.AUTO_SCALING_GROUP_UUID_TOKEN, self.getUuid())))));
                    deprecatedDiskSpecs.add(disk);
                }
                msg.setDeprecatedDataVolumeSpecs(deprecatedDiskSpecs);
            }
            msg.setStrategy(VmCreationStrategy.InstantStart.toString());
            msg.setClusterUuid(vmTemplateVO.getVmInstanceClusterUuid());
            msg.setHostUuid(vmTemplateVO.getHostUuid());
            bus.makeLocalServiceId(msg, VmInstanceConstant.SERVICE_ID);
            createVmInstanceMsgs.add(msg);
        }

        return createVmInstanceMsgs;
    }

    private void checkScalingGroupState() {
        if (self.getState() != AutoScalingGroupState.Enabled) {
            throw new OperationFailureException(operr(
                    "the auto scaling group[%s] state error, expected: %s state", self.getUuid(), AutoScalingGroupState.Enabled.toString())
            );
        }
    }

    private void fixActivityRetention(String autoScalingGroupUuid) {
        Long activityCount = Q.New(AutoScalingGroupActivityVO.class)
                .eq(AutoScalingGroupInstanceVO_.scalingGroupUuid, autoScalingGroupUuid)
                .limit( AutoScalingConstants.ACTIVITY_RETENTION_BUFFER)
                .count();

        if (activityCount.intValue() - AutoScalingGlobalConfig.AutoScalingGroup_Activity_Retention_Amount.value(Long.class).intValue() < AutoScalingConstants.ACTIVITY_RETENTION_BUFFER){
            return;
        } else{
            List<String> uuids = Q.New(AutoScalingGroupActivityVO.class)
                    .select(AutoScalingGroupInstanceVO_.uuid)
                    .eq(AutoScalingGroupInstanceVO_.scalingGroupUuid, autoScalingGroupUuid)
                    .limit( AutoScalingConstants.ACTIVITY_RETENTION_BUFFER)
                    .start(AutoScalingGlobalConfig.AutoScalingGroup_Activity_Retention_Amount.value(Long.class).intValue())
                    .listValues();

            logger.debug(String.format("AutoScalingGroupActivityRetention-Retention[%s]-Delete[%s]",
                        AutoScalingGlobalConfig.AutoScalingGroup_Activity_Retention_Amount.value(Long.class).toString(), uuids.size()));

            SQL.New(AutoScalingGroupActivityVO.class)
                    .in(AutoScalingGroupActivityVO_.uuid, uuids)
                    .delete();
        }
    }

    private void checkScalingGroupVmTemplate() {
        boolean attachedVmTemplate = Q.New(AutoScalingTemplateGroupRefVO.class)
                .eq(AutoScalingTemplateGroupRefVO_.groupUuid, self.getUuid())
                .isExists();

        if (!attachedVmTemplate) {
            throw new OperationFailureException(operr(
                    "The autoScalingGroup[%s] not attach any vm template", self.getUuid())
            );
        }
    }

    private void destroyVmInstanceWithDataVolume(List<String> vmInstanceUuids, ReturnValueCompletion<RemoveInstancesResult> completion) {
        List<String> deleteSuccessfulInstanceUuids = Collections.synchronizedList(new LinkedList<String>());

        List<String> dataVolumeUuids = Q.New(VolumeVO.class)
                .select(VolumeVO_.uuid)
                .eq(VolumeVO_.type, VolumeType.Data)
                .in(VolumeVO_.vmInstanceUuid, vmInstanceUuids)
                .listValues();

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("destroy-vms-%s-with-data-volumes", vmInstanceUuids));
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                List<ErrorCode> errors = Collections.synchronizedList(new LinkedList<ErrorCode>());

                flow(new NoRollbackFlow() {
                    String __name__ = "destroy-vms";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        List<DestroyVmInstanceMsg> destroyVmInstanceMsgs = new ArrayList<>();
                        for (String uuid : vmInstanceUuids) {
                            DestroyVmInstanceMsg destroyVmInstanceMsg = new DestroyVmInstanceMsg();
                            destroyVmInstanceMsg.setVmInstanceUuid(uuid);
                            destroyVmInstanceMsg.setDeletionPolicy(VmInstanceDeletionPolicyManager.VmInstanceDeletionPolicy.Direct);
                            bus.makeLocalServiceId(destroyVmInstanceMsg, VmInstanceConstant.SERVICE_ID);
                            destroyVmInstanceMsgs.add(destroyVmInstanceMsg);
                        }

                        List<ErrorCode> errors = Collections.synchronizedList(new LinkedList<ErrorCode>());
                        new While<>(destroyVmInstanceMsgs).step((destroyVmInstanceMsg, whileCompletion) -> {
                            bus.send(destroyVmInstanceMsg, new CloudBusCallBack(whileCompletion) {
                                @Override
                                public void run(MessageReply rly) {
                                    if (rly.isSuccess()) {
                                        deleteSuccessfulInstanceUuids.add(destroyVmInstanceMsg.getVmInstanceUuid());

                                        VmInstanceVO vmInstanceVO = Q.New(VmInstanceVO.class)
                                                .eq(VmInstanceVO_.uuid, destroyVmInstanceMsg.getVmInstanceUuid())
                                                .find();
                                        if (vmInstanceVO != null && vmInstanceVO.getState() != VmInstanceState.Destroyed) {
                                            DeleteVmGC gc = new DeleteVmGC();
                                            String hostUuid = vmInstanceVO.getHostUuid() != null ?
                                                    vmInstanceVO.getHostUuid() : vmInstanceVO.getLastHostUuid();
                                            gc.NAME = String.format("gc-vm-%s-on-host-%s", vmInstanceVO.getUuid(), hostUuid);
                                            gc.hostUuid = hostUuid;
                                            gc.inventory = VmInstanceInventory.valueOf(vmInstanceVO);
                                            gc.submit();
                                        }

                                    } else {
                                        errors.add(rly.getError());
                                    }

                                    whileCompletion.done();
                                }
                            });
                        }, 10).run(new WhileDoneCompletion(trigger) {
                            @Override
                            public void done(ErrorCodeList errorCodeList) {
                                if (errors.size() == vmInstanceUuids.size()) {
                                    trigger.fail(operr("autoScalingGroup[%s] destroy vms[%s] failed completely, errors are %s"
                                            ,self.getUuid(), vmInstanceUuids, JSONObjectUtil.toJsonString(errors)));
                                    return;
                                }

                                trigger.next();
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "destroy-vm-data-volumes";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        List<String> failedVmUuids = new ArrayList<>();
                        failedVmUuids.addAll(vmInstanceUuids);
                        failedVmUuids.removeAll(deleteSuccessfulInstanceUuids);

                        if (!failedVmUuids.isEmpty()) {
                            List<String> excludeDataVolumeUuids = Q.New(VolumeVO.class)
                                    .select(VolumeVO_.uuid)
                                    .eq(VolumeVO_.type, VolumeType.Data)
                                    .in(VolumeVO_.vmInstanceUuid, failedVmUuids)
                                    .listValues();
                            dataVolumeUuids.removeAll(excludeDataVolumeUuids);
                        }

                        if (dataVolumeUuids.isEmpty()) {
                            trigger.next();
                            return;
                        }

                        List<VolumeVO> volumeVOS = Q.New(VolumeVO.class)
                                .in(VolumeVO_.uuid, dataVolumeUuids)
                                .list();
                        List<VolumeInventory> volumeInventories = VolumeInventory.valueOf(volumeVOS);

                        List<VolumeDeletionStruct> ctx = transform(volumeInventories, arg -> {
                            VolumeDeletionStruct s = new VolumeDeletionStruct();
                            s.setInventory(arg);
                            s.setDeletionPolicy(VmInstanceDeletionPolicyManager.VmInstanceDeletionPolicy.Direct.toString());
                            return s;
                        });

                        final String issuer = VolumeVO.class.getSimpleName();
                        casf.asyncCascade(CascadeConstant.DELETION_DELETE_CODE, issuer, ctx, new Completion(trigger) {
                            @Override
                            public void success() {
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                logger.warn(String.format("autoScalingGroup[uuid=%s] delete vm dataVolumes failed:%n%s",
                                        self.getUuid(), errorCode.getReadableDetails()));
                                trigger.next();
                            }
                        });
                    }
                });

                done(new FlowDoneHandler(completion) {
                    @Override
                    public void handle(Map data) {
                        RemoveInstancesResult result = new RemoveInstancesResult();
                        result.setInstanceUuids(deleteSuccessfulInstanceUuids);
                        result.setErrorCodes(errors);
                        completion.success(result);
                    }
                });

                error(new FlowErrorHandler(completion) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        completion.fail(errCode);
                    }
                });
            }
        }).start();
    }

    private void checkScalingGroupNumberOfInstances(AutoScalingGroupCreateInstancesActionMsg msg) {
        if (msg.ignoreInstanceSizeLimit()) {
            return;
        }

        int current = Q.New(AutoScalingGroupInstanceVO.class)
                .eq(AutoScalingGroupInstanceVO_.scalingGroupUuid, self.getUuid())
                .count()
                .intValue();

        if (current >= self.getMaxResourceSize()) {
            msg.setAddingInstanceSize(0);
        }

        if (current + msg.getAddingInstanceSize() > self.getMaxResourceSize()) {
            msg.setAddingInstanceSize(self.getMaxResourceSize() - current);
        }
    }

    private void addInstancesToScalingGroup(String scalingGroupActivityUuid, List<String> instanceUuids) {
        AutoScalingTemplateGroupRefVO templateGroupRefVO = Q.New(AutoScalingTemplateGroupRefVO.class)
                .eq(AutoScalingTemplateGroupRefVO_.groupUuid, self.getUuid())
                .find();

        List<AutoScalingGroupInstanceVO> vos = new ArrayList<>();
        for (String uuid : instanceUuids) {
            AutoScalingGroupInstanceVO vo = new AutoScalingGroupInstanceVO();
            vo.setInstanceUuid(uuid);
            vo.setUuid(Platform.getUuid());
            vo.setStatus(AutoScalingGroupInstanceStatus.Creating);
            vo.setHealthStatus(AutoScalingGroupInstanceHealthStatus.Healthy);
            vo.setScalingGroupActivityUuid(scalingGroupActivityUuid);
            vo.setScalingGroupUuid(self.getUuid());
            vo.setTemplateUuid(templateGroupRefVO.getTemplateUuid());
            vo.setCreateDate(new Timestamp(System.currentTimeMillis()));
            vo.setProtectionStrategy(AutoScalingConstants.AutoScalingGroupInstance.PROTECTION_STRATEGY_UNPROTECTED);
            vo.setAccountUuid(acntMgr.getOwnerAccountUuidOfResource(self.getUuid()));
            vos.add(vo);
        }

        dbf.persistCollection(vos);
    }

    private void deleteInstancesFromScalingGroup(List<String> instanceUuids) {
        assert instanceUuids.size() > 0;
        SQL.New("delete from AutoScalingGroupInstanceVO vo where vo.instanceUuid in (:instanceUuids)")
                .param("instanceUuids", instanceUuids)
                .execute();
    }

    private void handle(AutoScalingGroupRemoveInstancesActionMsg msg) {
        String groupUuid = msg.getAutoScalingGroupUuid();

        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getName() {
                return String.format("remove-instance-from-scalingGroup-%s", groupUuid);
            }

            @Override
            public String getSyncSignature() {
                return syncThreadName + "-activity-action";
            }

            @Override
            public void run(SyncTaskChain chain) {
                AutoScalingGroupRemoveInstancesActionReply reply = new AutoScalingGroupRemoveInstancesActionReply();

                doRemoveInstancesAction(msg, new ReturnValueCompletion<RemoveInstancesResult>(chain, msg) {
                    @Override
                    public void success(RemoveInstancesResult result) {
                        if (result.getErrorCodes() == null || result.getErrorCodes().size() == 0) {
                            SQL.New(AutoScalingGroupActivityVO.class).
                                    set(AutoScalingGroupActivityVO_.status, AutoScalingGroupActivityStatus.Successful).
                                    set(AutoScalingGroupActivityVO_.instanceUuids, StringUtils.join(result.getInstanceUuids(), ",")).
                                    eq(AutoScalingGroupActivityVO_.uuid, msg.getAutoScalingGroupActivityUuid()).
                                    update();
                        } else {
                            String resultMessage = JSONObjectUtil.toJsonString(result.getErrorCodes());
                            SQL.New(AutoScalingGroupActivityVO.class).
                                    set(AutoScalingGroupActivityVO_.status, AutoScalingGroupActivityStatus.Warning).
                                    set(AutoScalingGroupActivityVO_.activityActionResultMessage, resultMessage).
                                    eq(AutoScalingGroupActivityVO_.uuid, msg.getAutoScalingGroupActivityUuid()).
                                    update();
                        }

                        reply.setResult(result);
                        bus.reply(msg, reply);

                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        reply.setError(errorCode);
                        bus.reply(msg, reply);

                        chain.next();
                    }
                });
            }
        });
    }

    private void doRemoveInstancesAction(final AutoScalingGroupRemoveInstancesActionMsg msg, final ReturnValueCompletion<RemoveInstancesResult> completion) {
        checkScalingGroupState();
        checkScalingGroupNumberOfInstances(msg);

        if (msg.getRemovalInstanceSize() <= 0) {
            completion.fail(operr("The number of instances exceeds the limit"));
            return;
        }

        SQL.New(AutoScalingGroupActivityVO.class).
                set(AutoScalingGroupActivityVO_.status, AutoScalingGroupActivityStatus.InProgress).
                eq(AutoScalingGroupActivityVO_.uuid, msg.getAutoScalingGroupActivityUuid()).
                update();

        getRemoveInstanceUuids(msg, new ReturnValueCompletion<List<String>>(completion) {
            @Override
            public void success(List<String> targetInstanceUuids) {
                if (targetInstanceUuids == null || targetInstanceUuids.isEmpty()) {
                    completion.fail(operr("Cannot find deleted target instance list"));
                    return;
                }

                for (AutoScalingGroupRemoveInstancesActionExtensionPoint ext : pluginRgty.getExtensionList(AutoScalingGroupRemoveInstancesActionExtensionPoint.class)) {
                    ext.beforeRemoveInstances(self.getUuid(), msg.getInstanceUuids());
                }

                ScalingGroupInstanceFactory factory = autoScalingManager.getScalingGroupInstanceFactory(self.getScalingResourceType());
                AutoScalingGroupRemoveInstancesMsg removeInstancesMsg = factory.getAutoScalingRemoveInstanceMsg(self.getUuid(), targetInstanceUuids);
                bus.makeLocalServiceId(removeInstancesMsg, AutoScalingConstants.SERVICE_ID);

                for (AutoScalingGroupRemoveInstancesActionExtensionPoint ext : pluginRgty.getExtensionList(AutoScalingGroupRemoveInstancesActionExtensionPoint.class)) {
                    ext.preRemoveInstances(self.getUuid(), msg.getInstanceUuids());
                }

                bus.send(removeInstancesMsg, new CloudBusCallBack(msg, completion) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            completion.fail(reply.getError());
                            return;
                        }

                        AutoScalingRemoveInstancesReply r = (AutoScalingRemoveInstancesReply) reply;
                        RemoveInstancesResult result = r.getResult();

                        if (result.getInstanceUuids() != null && result.getInstanceUuids().size() > 0) {
                            deleteInstancesFromScalingGroup(result.getInstanceUuids());
                        }

                        for (AutoScalingGroupRemoveInstancesActionExtensionPoint ext : pluginRgty.getExtensionList(AutoScalingGroupRemoveInstancesActionExtensionPoint.class)) {
                            ext.afterRemoveInstancesSuccess(self.getUuid(), result);
                        }

                        completion.success(result);
                    }
                });
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    private void getRemoveInstanceUuids(AutoScalingGroupRemoveInstancesActionMsg msg, ReturnValueCompletion<List<String>> completion) {
        List<String> result = new ArrayList<>();

        int removalInstanceSize = msg.getRemovalInstanceSize();
        List<String> instanceUuids = msg.getInstanceUuids();

        if (instanceUuids == null) {
            AutoScalingGroupActivityVO activityVO = dbf.findByUuid(msg.getAutoScalingGroupActivityUuid(), AutoScalingGroupActivityVO.class);
            RemovalPolicy policy = self.getRemovalPolicy();
            if (activityVO.getScalingGroupRuleUuid() != null) {
                RemovalInstanceRuleVO ruleVO = dbf.findByUuid(activityVO.getScalingGroupRuleUuid(), RemovalInstanceRuleVO.class);
                policy = ruleVO.getRemovalPolicy();
            }
            ScalingGroupInstanceFactory factory = autoScalingManager.getScalingGroupInstanceFactory(self.getScalingResourceType());
            GetRemoveTargetInstanceListMsg getRemoveTargetInstanceListMsg = factory.getRemoveTargetInstanceListMsg(self.getUuid(), removalInstanceSize, policy);
            bus.makeLocalServiceId(getRemoveTargetInstanceListMsg, AutoScalingConstants.SERVICE_ID);
            bus.send(getRemoveTargetInstanceListMsg, new CloudBusCallBack(completion) {
                @Override
                public void run(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        completion.fail(reply.getError());
                        return;
                    }

                    GetRemoveTargetInstanceListReply rly = (GetRemoveTargetInstanceListReply) reply;
                    completion.success(rly.getResult());
                }
            });
            return;
        }

        if (instanceUuids.size() > removalInstanceSize) {
            result.addAll(instanceUuids.subList(0, removalInstanceSize));
            completion.success(result);
            return;
        }

        completion.success(instanceUuids);
    }

    private List<String> getRemoveInstanceUuids(int size, RemovalPolicy policy) {
        List<String> result = new ArrayList<>();

        List<String> protectedInstanceUuids = getAllProtectedInstanceUuids();
        int limit = size + protectedInstanceUuids.size();

        if (policy == RemovalPolicy.NewestInstance) {
            result = SQL.New("select instanceUuid from AutoScalingGroupInstanceVO where scalingGroupUuid = :groupUuid order by createDate desc")
                    .param("groupUuid", self.getUuid())
                    .offset(0)
                    .limit(limit)
                    .list();
        }

        if (policy == RemovalPolicy.OldestInstance) {
            result = SQL.New("select instanceUuid from AutoScalingGroupInstanceVO where scalingGroupUuid = :groupUuid order by createDate")
                    .param("groupUuid", self.getUuid())
                    .offset(0)
                    .limit(limit)
                    .list();
        }

        if (policy == RemovalPolicy.OldestScalingConfiguration) {
            List<String> uuids = SQL.New("select t0.uuid from AutoScalingGroupInstanceVO t0, AutoScalingGroupActivityVO t1" +
                    " where t0.scalingGroupUuid = :groupUuid" +
                    " and t0.scalingGroupActivityUuid = t1.uuid" +
                    " and t1.cause in (:causes) " +
                    " order by t0.createDate")
                    .param("groupUuid", self.getUuid())
                    .param("causes", Arrays.asList(AutoScalingGroupActivityCause.MaintainTheNumberOfInstances, AutoScalingGroupActivityCause.RuleTakesEffect))
                    .offset(0)
                    .limit(limit)
                    .list();
            if (uuids == null || uuids.isEmpty()) {
                result = SQL.New("select instanceUuid from AutoScalingGroupInstanceVO where scalingGroupUuid = :groupUuid order by createDate")
                        .param("groupUuid", self.getUuid())
                        .offset(0)
                        .limit(limit)
                        .list();
            } else if(uuids.size() < limit) {
                result = SQL.New("select instanceUuid from AutoScalingGroupInstanceVO" +
                        " where scalingGroupUuid = :groupUuid " +
                        " and instanceUuid not in (:uuids) order by createDate")
                        .param("groupUuid", self.getUuid())
                        .param("uuids", uuids)
                        .offset(0)
                        .limit(limit - uuids.size())
                        .list();
                result.addAll(uuids);
            }
        }

        result.removeAll(protectedInstanceUuids);
        return result;
    }

    private List<String> getAllProtectedInstanceUuids() {
        List<String> instanceUuids = Q.New(AutoScalingGroupInstanceVO.class)
                .select(AutoScalingGroupInstanceVO_.instanceUuid)
                .eq(AutoScalingGroupInstanceVO_.scalingGroupUuid, self.getUuid())
                .eq(AutoScalingGroupInstanceVO_.protectionStrategy, PROTECTION_STRATEGY_PROTECTED)
                .listValues();
        return instanceUuids;
    }

    private void checkScalingGroupNumberOfInstances(AutoScalingGroupRemoveInstancesActionMsg msg) {
        int current = Q.New(AutoScalingGroupInstanceVO.class)
                .eq(AutoScalingGroupInstanceVO_.scalingGroupUuid, self.getUuid())
                .count()
                .intValue();

        if (msg.ignoreInstanceSizeLimit()) {
            if (msg.getRemovalInstanceSize() > current) {
                msg.setRemovalInstanceSize(current);
            }
            return;
        }

        if (current < self.getMinResourceSize()) {
            msg.setRemovalInstanceSize(0);
        }

        if (current - msg.getRemovalInstanceSize() < self.getMinResourceSize()) {
            msg.setRemovalInstanceSize(current - self.getMinResourceSize());
        }
    }

    private void handle(AutoScalingGroupRemoveVmInstancesMsg msg) {
        AutoScalingRemoveInstancesReply msgReply = new AutoScalingRemoveInstancesReply();
        List<String> instanceUuids = msg.getInstanceUuids();
        assert instanceUuids.size() > 0;

        List<String> deleteSuccessfulInstanceUuids = Collections.synchronizedList(new LinkedList<String>());

        long count = Q.New(AutoScalingGroupInstanceVO.class)
                .in(AutoScalingGroupInstanceVO_.instanceUuid, instanceUuids)
                .eq(AutoScalingGroupInstanceVO_.scalingGroupUuid, msg.getAutoScalingGroupUuid())
                .count();
        assert count == instanceUuids.size() :
                String.format("instanceUuids size[%d] is not equal to count[%d]", instanceUuids.size(), count);

        List<String> vmInstanceUuids = Q.New(VmInstanceVO.class)
                .select(VmInstanceVO_.uuid)
                .in(VmInstanceVO_.uuid, instanceUuids)
                .listValues();
        List<String> nonExistVmUuids = ListUtils.subtract(instanceUuids, vmInstanceUuids);
        deleteSuccessfulInstanceUuids.addAll(nonExistVmUuids);

        if (vmInstanceUuids.isEmpty()) {
            RemoveInstancesResult result = new RemoveInstancesResult();
            result.setInstanceUuids(deleteSuccessfulInstanceUuids);
            msgReply.setResult(result);
            bus.reply(msg, msgReply);
            return;
        }

        destroyVmInstanceWithDataVolume(vmInstanceUuids, new ReturnValueCompletion<RemoveInstancesResult>(msg) {
            @Override
            public void success(RemoveInstancesResult returnValue) {
                msgReply.setResult(returnValue);
                bus.reply(msg, msgReply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                msgReply.setError(errorCode);
                bus.reply(msg, msgReply);
            }
        });
    }

    private void handle(TriggerAutoScalingGroupRuleMsg msg) {
        TriggerAutoScalingGroupRuleReply reply = new TriggerAutoScalingGroupRuleReply();

        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getName() {
                return String.format("rigger-autoScalingRule-%s", msg.getAutoScalingRuleUuid());
            }

            @Override
            public String getSyncSignature() {
                return String.format("%s-%s", ruleSyncThreadName, msg.getAutoScalingRuleUuid());
            }

            @Override
            public void run(SyncTaskChain chain) {
                String ruleUuid = msg.getAutoScalingRuleUuid();
                AutoScalingRuleVO ruleVO = dbf.findByUuid(ruleUuid, AutoScalingRuleVO.class);
                AutoScalingRuleFactory factory = autoScalingManager.getAutoScalingRuleFactory(ruleVO.getType());

                boolean skip = factory.skipAutoScalingGroupActivity(ruleUuid);
                if (skip) {
                    reply.setError(operr("need skip autoScalingGroup activity"));
                    bus.reply(msg, reply);
                    chain.next();
                    return;
                }

                CreateAutoScalingGroupActivityMsg createAutoScalingGroupGroupActivityMsg = factory.makeAutoScalingGroupActivity(ruleVO.getUuid());
                bus.send(createAutoScalingGroupGroupActivityMsg, new CloudBusCallBack(msg, chain) {
                    @Override
                    public void run(MessageReply rly) {
                        if (!rly.isSuccess()) {
                            reply.setError(rly.getError());
                            bus.reply(msg, reply);
                            chain.next();
                            return;
                        }

                        CreateAutoScalingGroupActivityReply r = (CreateAutoScalingGroupActivityReply) rly;
                        reply.setAutoScalingGroupActivityUuid(r.getAutoScalingGroupActivityUuid());
                        bus.reply(msg, reply);
                        chain.next();
                    }
                });
            }
        });
    }

    private void handle(DeleteAutoScalingGroupInstanceMsg msg) {
        DeleteAutoScalingGroupInstanceReply reply = new DeleteAutoScalingGroupInstanceReply();

        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getName() {
                return String.format("delete-auto-scaling-group-%s-instance-%s", self.getUuid(), msg.getInstanceUuid());
            }

            @Override
            public String getSyncSignature() {
                return String.format("%s-instance-%s", syncThreadName, msg.getInstanceUuid());
            }

            @Override
            public void run(SyncTaskChain chain) {
                doDeleteAutoScalingGroupInstance(msg.getInstanceUuid(), msg.isForceDelete(), new Completion(chain) {
                    @Override
                    public void success() {
                        bus.reply(msg, reply);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        reply.setError(errorCode);
                        bus.reply(msg, reply);
                        chain.next();
                    }
                });
            }
        });
    }

    private void doDeleteAutoScalingGroupInstance(String instanceUuid, boolean forceDelete, Completion completion) {
        if (forceDelete) {
            deleteInstanceDirectly(instanceUuid, completion);
            return;
        }

        CreateAutoScalingGroupActivityMsg msg = new CreateAutoScalingGroupActivityMsg();
        String uuid = Platform.getUuid();
        msg.setActivityUuid(uuid);
        msg.setName("");
        msg.setScalingGroupUuid(self.getUuid());
        msg.setDescription("");
        msg.setActivityAction(AutoScalingGroupActivityAction.RemovalInstance.toString());
        msg.setCause(AutoScalingGroupActivityCause.ManualOperation.toString());
        bus.makeLocalServiceId(msg, AutoScalingConstants.SERVICE_ID);

        AutoScalingGroupRemoveInstancesActionMsg actionMsg = new AutoScalingGroupRemoveInstancesActionMsg();
        actionMsg.setInstanceUuids(Arrays.asList(instanceUuid));
        actionMsg.setAutoScalingGroupUuid(self.getUuid());
        actionMsg.setAutoScalingGroupActivityUuid(uuid);
        actionMsg.setIgnoreInstanceSizeLimit(true);
        bus.makeLocalServiceId(actionMsg, AutoScalingConstants.SERVICE_ID);
        msg.setActionMessage(actionMsg);

        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    completion.success();
                } else {
                    completion.fail(reply.getError());
                }
            }
        });
    }

    private void deleteInstanceDirectly(String instanceUuid, final Completion completion) {
        for (AutoScalingGroupRemoveInstancesActionExtensionPoint ext : pluginRgty.getExtensionList(AutoScalingGroupRemoveInstancesActionExtensionPoint.class)) {
            ext.beforeRemoveInstances(self.getUuid(), Arrays.asList(instanceUuid));
        }

        ScalingGroupInstanceFactory factory = autoScalingManager.getScalingGroupInstanceFactory(self.getScalingResourceType());
        AutoScalingGroupRemoveInstancesMsg removeInstancesMsg = factory.getAutoScalingRemoveInstanceMsg(self.getUuid(), Arrays.asList(instanceUuid));
        bus.makeLocalServiceId(removeInstancesMsg, AutoScalingConstants.SERVICE_ID);

        for (AutoScalingGroupRemoveInstancesActionExtensionPoint ext : pluginRgty.getExtensionList(AutoScalingGroupRemoveInstancesActionExtensionPoint.class)) {
            ext.preRemoveInstances(self.getUuid(), Arrays.asList(instanceUuid));
        }

        bus.send(removeInstancesMsg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                AutoScalingRemoveInstancesReply r = (AutoScalingRemoveInstancesReply) reply;
                RemoveInstancesResult result = r.getResult();

                if (result.getInstanceUuids() != null && result.getInstanceUuids().size() > 0) {
                    deleteInstancesFromScalingGroup(result.getInstanceUuids());
                }

                for (AutoScalingGroupRemoveInstancesActionExtensionPoint ext : pluginRgty.getExtensionList(AutoScalingGroupRemoveInstancesActionExtensionPoint.class)) {
                    ext.afterRemoveInstancesSuccess(self.getUuid(), result);
                }

                completion.success();
            }
        });
    }

    private void handle(DeleteAutoScalingRuleTriggerMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getName() {
                return String.format("delete-auto-scaling-group-%s-rule-trigger-%s", self.getUuid(), msg.getTriggerUuid());
            }

            @Override
            public String getSyncSignature() {
                return String.format("%s-trigger-%s", syncThreadName, msg.getTriggerUuid());
            }

            @Override
            public void run(SyncTaskChain chain) {
                MessageReply reply = new MessageReply();
                doDeleteAutoScalingGroupRuleTrigger(msg.getTriggerUuid(), msg.getAutoScalingGroupUuid(), new Completion(chain) {
                    @Override
                    public void success() {
                        bus.reply(msg, reply);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        reply.setError(errorCode);
                        bus.reply(msg, reply);
                        chain.next();
                    }
                });
            }
        });
    }

    private void doDeleteAutoScalingGroupRuleTrigger(String triggerUuid, String scalingGroupUid, Completion completion) {

        AutoScalingRuleTriggerVO triggerVO = dbf.findByUuid(triggerUuid, AutoScalingRuleTriggerVO.class);

        AutoScalingRuleTriggerFactory factory = autoScalingManager.getAutoScalingRuleTriggerFactory(triggerVO.getType());
        String resourceUuid = factory.getResourceUuid(triggerUuid);
        dbf.remove(triggerVO);
        factory.cleanResource(resourceUuid, new Completion(completion) {
            @Override
            public void success() {
                completion.success();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });

    }

    private void handle(DeleteAutoScalingRuleMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getName() {
                return String.format("delete-auto-scaling-group-%s-rule-%s", self.getUuid(), msg.getRuleUuid());
            }

            @Override
            public String getSyncSignature() {
                return String.format("%s-rule-%s", syncThreadName, msg.getRuleUuid());
            }

            @Override
            public void run(SyncTaskChain chain) {
                MessageReply reply = new MessageReply();
                doDeleteAutoScalingRule(msg.getRuleUuid(), msg.getAutoScalingGroupUuid(), new Completion(chain) {
                    @Override
                    public void success() {
                        bus.reply(msg, reply);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        reply.setError(errorCode);
                        bus.reply(msg, reply);
                        chain.next();
                    }
                });
            }
        });
    }

    private void doDeleteAutoScalingRule(String ruleUuid, String groupUuid, Completion completion) {
        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("delete-auto-scaling-rule-%s", ruleUuid));
        chain.then(new ShareFlow() {
            @Override
            public void setup() {

                List<ErrorCode> errors = Collections.synchronizedList(new LinkedList<ErrorCode>());

                flow(new NoRollbackFlow() {
                    String __name__ = "delete-auto-scaling-rule-triggers";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        List<AutoScalingRuleTriggerVO> triggerVOS = Q.New(AutoScalingRuleTriggerVO.class)
                                .eq(AutoScalingRuleTriggerVO_.ruleUuid, ruleUuid)
                                .list();
                        if (triggerVOS == null || triggerVOS.isEmpty()) {
                            trigger.next();
                            return;
                        }

                        List<DeleteAutoScalingRuleTriggerMsg> deleteAutoScalingGroupRuleTriggerMsgs = new ArrayList<>();
                        for (AutoScalingRuleTriggerVO triggerVO : triggerVOS) {
                            DeleteAutoScalingRuleTriggerMsg deleteAutoScalingGroupRuleTriggerMsg = new DeleteAutoScalingRuleTriggerMsg();
                            deleteAutoScalingGroupRuleTriggerMsg.setAutoScalingGroupUuid(groupUuid);
                            deleteAutoScalingGroupRuleTriggerMsg.setTriggerUuid(triggerVO.getUuid());
                            bus.makeLocalServiceId(deleteAutoScalingGroupRuleTriggerMsg, AutoScalingConstants.SERVICE_ID);
                            deleteAutoScalingGroupRuleTriggerMsgs.add(deleteAutoScalingGroupRuleTriggerMsg);
                        }

                        new While<>(deleteAutoScalingGroupRuleTriggerMsgs).step((deleteRuleTriggerMsg, whileCompletion) -> {
                            bus.send(deleteRuleTriggerMsg, new CloudBusCallBack(whileCompletion) {
                                @Override
                                public void run(MessageReply rly) {
                                    if (!rly.isSuccess()) {
                                        errors.add(rly.getError());
                                    }
                                    whileCompletion.done();
                                }
                            });
                        }, 10).run(new WhileDoneCompletion(trigger) {
                            @Override
                            public void done(ErrorCodeList errorCodeList) {
                                if (errors.size() != 0) {
                                    trigger.fail(operr("delete autoScalingRule[%s] triggers failed, errors are %s"
                                            ,ruleUuid, JSONObjectUtil.toJsonString(errors)));
                                    return;
                                }
                                trigger.next();
                            }
                        });
                    }
                });
                flow(new NoRollbackFlow() {
                    String __name__ = "delete-auto-scaling-rule";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        new SQLBatch() {
                            @Override
                            protected void scripts() {
                                AutoScalingRuleVO ruleVO = findByUuid(ruleUuid, AutoScalingRuleVO.class);

                                sql("update AutoScalingGroupActivityVO set scalingGroupRuleUuid = null " +
                                        " where scalingGroupRuleUuid = :ruleUuid" +
                                        " and scalingGroupUuid = :groupUuid")
                                        .param("ruleUuid", ruleUuid)
                                        .param("groupUuid", groupUuid)
                                        .execute();

                                remove(ruleVO);
                            }
                        }.execute();

                        trigger.next();
                    }
                });

                done(new FlowDoneHandler(completion) {
                    @Override
                    public void handle(Map data) {
                        completion.success();
                    }
                });

                error(new FlowErrorHandler(completion) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        completion.fail(errCode);
                    }
                });
            }

        }).start();
    }

    private void handle(APIDetachAutoScalingTemplateFromGroupMsg msg) {
        APIDetachAutoScalingTemplateFromGroupEvent event = new APIDetachAutoScalingTemplateFromGroupEvent(msg.getId());

        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return syncThreadName;
            }

            @Override
            public void run(SyncTaskChain chain) {
                DetachAutoScalingTemplateFromGroupMsg detachMsg = new DetachAutoScalingTemplateFromGroupMsg();
                detachMsg.setAutoScalingGroupUuid(msg.getAutoScalingGroupUuid());
                detachMsg.setTemplateUuid(msg.getTemplateUuid());

                bus.send(detachMsg, new CloudBusCallBack(msg) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            event.setError(reply.getError());
                            bus.publish(event);
                            chain.next();
                            return;
                        }

                        AutoScalingGroupVO vo = dbf.findByUuid(msg.getGroupUuid(), AutoScalingGroupVO.class);
                        event.setInventory(AutoScalingGroupInventory.valueOf(vo));
                        bus.publish(event);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return String.format("detach-auto-scaling-template-%s-from-group-%s", msg.getTemplateUuid(), msg.getAutoScalingGroupUuid());
            }
        });
    }

    private void handle(DetachAutoScalingTemplateFromGroupMsg msg) {
        MessageReply reply = new MessageReply();

        AutoScalingTemplateGroupRefVO refVO = Q.New(AutoScalingTemplateGroupRefVO.class)
                .eq(AutoScalingTemplateGroupRefVO_.templateUuid, msg.getTemplateUuid())
                .eq(AutoScalingTemplateGroupRefVO_.groupUuid, msg.getAutoScalingGroupUuid())
                .find();

        if (refVO != null) {
            dbf.remove(refVO);
        }

        bus.reply(msg, reply);
    }

    private void handle(APIDeleteAutoScalingRuleMsg msg) {
        APIDeleteAutoScalingRuleEvent event = new APIDeleteAutoScalingRuleEvent(msg.getId());

        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return String.format("%s-%s", ruleSyncThreadName, msg.getUuid());
            }

            @Override
            public void run(SyncTaskChain chain) {
                DeleteAutoScalingRuleMsg deleteMsg = new DeleteAutoScalingRuleMsg();
                deleteMsg.setAutoScalingGroupUuid(msg.getAutoScalingGroupUuid());
                deleteMsg.setRuleUuid(msg.getUuid());
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
                return String.format("delete-auto-scaling-rule-%s", msg.getUuid());
            }
        });
    }

    private void handle(APIDeleteAutoScalingRuleTriggerMsg msg) {
        APIDeleteAutoScalingRuleTriggerEvent event = new APIDeleteAutoScalingRuleTriggerEvent(msg.getId());

        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return String.format("%s-%s", ruleTriggerSyncThreadName, msg.getUuid());
            }

            @Override
            public void run(SyncTaskChain chain) {
                DeleteAutoScalingRuleTriggerMsg deleteMsg = new DeleteAutoScalingRuleTriggerMsg();
                deleteMsg.setAutoScalingGroupUuid(msg.getAutoScalingGroupUuid());
                deleteMsg.setTriggerUuid(msg.getUuid());
                bus.makeLocalServiceId(deleteMsg, AutoScalingConstants.SERVICE_ID);

                bus.send(deleteMsg, new CloudBusCallBack(chain, msg) {
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
                return String.format("delete-auto-scaling-rule-trigger-%s", msg.getUuid());
            }
        });
    }

    private void handle(APIDeleteAutoScalingGroupInstanceMsg msg) {
        APIDeleteAutoScalingGroupInstanceEvent event = new APIDeleteAutoScalingGroupInstanceEvent(msg.getId());

        DeleteAutoScalingGroupInstanceMsg deleteMsg = new DeleteAutoScalingGroupInstanceMsg();
        deleteMsg.setInstanceUuid(msg.getInstanceUuid());
        deleteMsg.setAutoScalingGroupUuid(msg.getAutoScalingGroupUuid());
        bus.makeTargetServiceIdByResourceUuid(deleteMsg, AutoScalingConstants.SERVICE_ID, msg.getAutoScalingGroupUuid());
        bus.send(deleteMsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    event.setError(reply.getError());
                }

                bus.publish(event);
            }
        });
    }

    private void handle(APIChangeAutoScalingGroupStateMsg msg) {
        APIChangeAutoScalingGroupStateEvent event = new APIChangeAutoScalingGroupStateEvent(msg.getId());

        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return syncThreadName;
            }

            @Override
            public void run(SyncTaskChain chain) {
                AutoScalingGroupState state = null;
                if (AutoScalingGroupStateEvent.enable.toString().equals(msg.getStateEvent())) {
                    state = AutoScalingGroupState.Enabled;
                } else if (AutoScalingGroupStateEvent.disable.toString().equals(msg.getStateEvent())) {
                    state = AutoScalingGroupState.Disabled;
                }
                assert state != null;

                SQL.New("update AutoScalingGroupVO vo set vo.state = :state where vo.uuid = :uuid")
                        .param("state", state)
                        .param("uuid", msg.getUuid())
                        .execute();

                AutoScalingGroupVO groupVO = dbf.findByUuid(msg.getUuid(), AutoScalingGroupVO.class);
                AutoScalingGroupInventory inventory = AutoScalingGroupInventory.valueOf(groupVO);
                event.setInventory(inventory);

                for (AutoScalingGroupStateChangedExtensionPoint ext : pluginRgty.getExtensionList(AutoScalingGroupStateChangedExtensionPoint.class)) {
                    ext.afterToggleAutoScalingGroupState(msg.getAutoScalingGroupUuid(), msg.getStateEvent());
                }

                bus.publish(event);
                chain.next();
            }

            @Override
            public String getName() {
                return String.format("change-auto-scaling-group-%s-state", msg.getUuid());
            }
        });
    }

    private void handle(DeleteAutoScalingGroupActivityMsg msg) {
        DeleteAutoScalingGroupActivityReply reply = new DeleteAutoScalingGroupActivityReply();

        long count = Q.New(AutoScalingGroupActivityVO.class)
                .eq(AutoScalingGroupActivityVO_.scalingGroupUuid, msg.getAutoScalingGroupUuid())
                .count();
        while (count > 0) {
            List<String> uuids = Q.New(AutoScalingGroupActivityVO.class)
                    .select(AutoScalingGroupActivityVO_.uuid)
                    .eq(AutoScalingGroupActivityVO_.scalingGroupUuid, msg.getAutoScalingGroupUuid())
                    .limit(1000)
                    .listValues();

            SQL.New(AutoScalingGroupActivityVO.class)
                    .eq(AutoScalingGroupActivityVO_.scalingGroupUuid, msg.getAutoScalingGroupUuid())
                    .in(AutoScalingGroupActivityVO_.uuid, uuids)
                    .delete();

            count = Q.New(AutoScalingGroupActivityVO.class)
                    .eq(AutoScalingGroupActivityVO_.scalingGroupUuid, msg.getAutoScalingGroupUuid())
                    .count();
        }
        
        bus.reply(msg, reply);
    }

    private void handle(DeleteAutoScalingGroupMsg msg) {
        DeleteAutoScalingGroupReply reply = new DeleteAutoScalingGroupReply();

        new SQLBatch() {
            @Override
            protected void scripts() {
                sql("delete from AutoScalingTemplateGroupRefVO where groupUuid = :scalingGroupUuid")
                        .param("scalingGroupUuid", msg.getAutoScalingGroupUuid())
                        .execute();

                sql("delete from AutoScalingGroupVO where uuid = :scalingGroupUuid")
                        .param("scalingGroupUuid", msg.getAutoScalingGroupUuid())
                        .execute();
            }
        }.execute();

        bus.reply(msg, reply);
    }

    private void handle(APIDeleteAutoScalingGroupMsg msg) {
        APIDeleteAutoScalingGroupEvent event = new APIDeleteAutoScalingGroupEvent(msg.getId());

        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return syncThreadName;
            }

            @Override
            public void run(SyncTaskChain chain) {
                doDeleteAutoScalingGroup(msg.getUuid(), msg.getDeletionMode(), new Completion(chain) {
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
                return String.format("delete-auto-scaling-group-%s", msg.getUuid());
            }
        });
    }

    private void doDeleteAutoScalingGroup(String groupUuid, APIDeleteMessage.DeletionMode deletionMode, Completion completion) {
        AutoScalingGroupVO groupVO = dbf.findByUuid(groupUuid, AutoScalingGroupVO.class);
        groupVO.setState(AutoScalingGroupState.Deleting);
        self = dbf.updateAndRefresh(groupVO);

        final String issuer = AutoScalingGroupVO.class.getSimpleName();
        List<AutoScalingGroupInventory> ctx = Arrays.asList(AutoScalingGroupInventory.valueOf(self));

        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("delete-auto-scaling-group-%s", groupUuid));
        if (deletionMode == APIDeleteMessage.DeletionMode.Permissive) {
            chain.then(new NoRollbackFlow() {
                @Override
                public void run(final FlowTrigger trigger, Map data) {
                    casf.asyncCascade(CascadeConstant.DELETION_CHECK_CODE, issuer, ctx, new Completion(trigger) {
                        @Override
                        public void success() {
                            trigger.next();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            trigger.fail(errorCode);
                        }
                    });
                }
            }).then(new NoRollbackFlow() {
                @Override
                public void run(final FlowTrigger trigger, Map data) {
                    casf.asyncCascade(CascadeConstant.DELETION_DELETE_CODE, issuer, ctx, new Completion(trigger) {
                        @Override
                        public void success() {
                            trigger.next();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            trigger.fail(errorCode);
                        }
                    });
                }
            });
        } else {
            chain.then(new NoRollbackFlow() {
                @Override
                public void run(final FlowTrigger trigger, Map data) {
                    casf.asyncCascade(CascadeConstant.DELETION_FORCE_DELETE_CODE, issuer, ctx, new Completion(trigger) {
                        @Override
                        public void success() {
                            trigger.next();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            trigger.fail(errorCode);
                        }
                    });
                }
            });
        }

        chain.done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                casf.asyncCascadeFull(CascadeConstant.DELETION_CLEANUP_CODE, issuer, ctx, new NopeCompletion());
                completion.success();
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                completion.fail(err(SysErrors.DELETE_RESOURCE_ERROR, errCode.getDetails()).withCause(errCode));
            }
        }).start();
    }

    private void handle(APIUpdateAutoScalingRuleMsg msg) {
        APIUpdateAutoScalingRuleEvent event = new APIUpdateAutoScalingRuleEvent(msg.getId());

        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return String.format("%s-%s", ruleSyncThreadName, msg.getUuid());
            }

            @Override
            public void run(SyncTaskChain chain) {
                AutoScalingRuleVO vo = dbf.findByUuid(msg.getUuid(), AutoScalingRuleVO.class);

                if (msg.getName() != null) {
                    vo.setName(msg.getName());
                }

                if (msg.getDescription() != null) {
                    vo.setDescription(msg.getDescription());
                }

                if (msg.getCooldown() != null) {
                    vo.setCooldown(msg.getCooldown());
                }

                vo = dbf.updateAndRefresh(vo);
                event.setInventory(AutoScalingRuleInventory.valueOf(vo));
                bus.publish(event);
                chain.next();
            }

            @Override
            public String getName() {
                return String.format("update-auto-scaling-rule-%s", msg.getUuid());
            }
        });
    }

    private void handle(APIUpdateAutoScalingGroupAddingNewInstanceRuleMsg msg) {
        APIUpdateAutoScalingRuleEvent event = new APIUpdateAutoScalingRuleEvent(msg.getId());

        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return String.format("%s-%s", ruleSyncThreadName, msg.getUuid());
            }

            @Override
            public void run(SyncTaskChain chain) {
                AddingNewInstanceRuleVO vo = dbf.findByUuid(msg.getUuid(), AddingNewInstanceRuleVO.class);

                if (msg.getName() != null) {
                    vo.setName(msg.getName());
                }

                if (msg.getDescription() != null) {
                    vo.setDescription(msg.getDescription());
                }

                if (msg.getCooldown() != null) {
                    vo.setCooldown(msg.getCooldown());
                }

                if (msg.getAdjustmentType() != null) {
                    vo.setAdjustmentType(AdjustmentType.valueOf(msg.getAdjustmentType()));
                }

                if (msg.getAdjustmentValue() != null) {
                    vo.setAdjustmentValue(msg.getAdjustmentValue());
                }

                vo = dbf.updateAndRefresh(vo);
                event.setInventory(AddingNewVmRuleInventory.valueOf(vo));
                bus.publish(event);
                chain.next();
            }

            @Override
            public String getName() {
                return String.format("update-auto-scaling-rule-%s", msg.getUuid());
            }
        });
    }

    private void handle(APIUpdateAutoScalingGroupRemovalInstanceRuleMsg msg) {
        APIUpdateAutoScalingRuleEvent event = new APIUpdateAutoScalingRuleEvent(msg.getId());

        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return String.format("%s-%s", ruleSyncThreadName, msg.getUuid());
            }

            @Override
            public void run(SyncTaskChain chain) {
                RemovalInstanceRuleVO vo = dbf.findByUuid(msg.getUuid(), RemovalInstanceRuleVO.class);

                if (msg.getName() != null) {
                    vo.setName(msg.getName());
                }

                if (msg.getDescription() != null) {
                    vo.setDescription(msg.getDescription());
                }

                if (msg.getCooldown() != null) {
                    vo.setCooldown(msg.getCooldown());
                }

                if (msg.getAdjustmentType() != null) {
                    vo.setAdjustmentType(AdjustmentType.valueOf(msg.getAdjustmentType()));
                }

                if (msg.getAdjustmentValue() != null) {
                    vo.setAdjustmentValue(msg.getAdjustmentValue());
                }

                if (msg.getRemovalPolicy() != null) {
                    vo.setRemovalPolicy(RemovalPolicy.valueOf(msg.getRemovalPolicy()));
                }

                vo = dbf.updateAndRefresh(vo);
                event.setInventory(RemovalInstanceRuleInventory.valueOf(vo));
                bus.publish(event);
                chain.next();
            }

            @Override
            public String getName() {
                return String.format("update-auto-scaling-rule-%s", msg.getUuid());
            }
        });
    }

    private void handle(GetRemoveTargetInstanceListMsg msg) {
        GetRemoveTargetInstanceListReply reply = new GetRemoveTargetInstanceListReply();
        List<String> result = getRemoveInstanceUuids(msg.getSize(), msg.getPolicy());

        reply.setResult(result);
        bus.reply(msg, reply);
    }

    private void handle(GetRemoveTargetVmInstanceListMsg msg) {
        GetRemoveTargetInstanceListReply reply = new GetRemoveTargetInstanceListReply();

        RemovalPolicy removalPolicy = msg.getPolicy();
        if (Arrays.asList(RemovalPolicy.NewestInstance, RemovalPolicy.OldestInstance, RemovalPolicy.OldestScalingConfiguration).contains(removalPolicy)) {
            List<String> result = getRemoveInstanceUuids(msg.getSize(), msg.getPolicy());
            reply.setResult(result);
            bus.reply(msg, reply);
            return;
        }

        if (Arrays.asList(RemovalPolicy.MinimumCPUUsageInstance, RemovalPolicy.MinimumMemoryUsageInstance).contains(removalPolicy)) {
            String namespace = String.format("ZStack/%s", VmNamespace.NAME);
            String metricName = removalPolicy == RemovalPolicy.MinimumCPUUsageInstance ? VmNamespace.CPUAverageUsedUtilization.getName() : VmNamespace.MemoryUsedInPercent.getName();

            List<String> vmInstanceUuids = Q.New(AutoScalingGroupInstanceVO.class)
                    .select(AutoScalingGroupInstanceVO_.instanceUuid)
                    .eq(AutoScalingGroupInstanceVO_.scalingGroupUuid, self.getUuid())
                    .listValues();
            assert vmInstanceUuids.size() >= msg.getSize();
            String vmUuids = String.join("|" , vmInstanceUuids);
            String label = String.format("%s%s%s", VmNamespace.LabelNames.VMUuid.toString(), Label.Operator.Regex.toString(), vmUuids);
            String function = String.format("low(num=%s)", vmInstanceUuids.size());

            GetMetricDataMsg getMetricDataMsg = new GetMetricDataMsg();
            getMetricDataMsg.setNamespace(namespace);
            getMetricDataMsg.setMetricName(metricName);
            getMetricDataMsg.setOffsetAheadOfCurrentTime(1L);
            getMetricDataMsg.setLabels(Arrays.asList(label));
            getMetricDataMsg.setFunctions(Arrays.asList(function));
            bus.makeLocalServiceId(getMetricDataMsg, ZWatchConstants.SERVICE_ID);

            bus.send(getMetricDataMsg, new CloudBusCallBack(msg) {
                @Override
                public void run(MessageReply r) {
                    if (!r.isSuccess()) {
                        reply.setError(r.getError());
                        bus.reply(msg, reply);
                        return;
                    }

                    GetMetricDataReply rly = (GetMetricDataReply) r;
                    List<Datapoint> datapoints = rly.getDatas();
                    if (datapoints == null || datapoints.isEmpty()) {
                        logger.warn(String.format("The vmInstance load data in the autoScalingGroup[uuid=%s] was not found, and the deleted instance was randomly selected.", self.getUuid()));
                        reply.setResult(vmInstanceUuids.subList(0, msg.getSize()));
                        bus.reply(msg, reply);
                        return;
                    }

                    Set<String> result = new HashSet<>();
                    for (Datapoint datapoint : datapoints) {
                        String vmUuid = datapoint.getLabels().get(VmNamespace.LabelNames.VMUuid.toString());
                        if (vmInstanceUuids.contains(vmUuid)) {
                            result.add(vmUuid);
                        }

                        if (result.size() == msg.getSize()) {
                            break;
                        }
                    }

                    if (result.size() < msg.getSize()) {
                        for(String vmUuid : vmInstanceUuids) {
                            logger.warn(String.format("No load data for each instance in the autoScalingGroup[uuid=%s] was found, and the deleted instance was randomly selected.", self.getUuid()));
                            result.add(vmUuid);

                            if (result.size() == msg.getSize()) {
                                break;
                            }
                        }
                    }

                    reply.setResult(new ArrayList<>(result));
                    bus.reply(msg, reply);
                }
            });
            return;
        }

        reply.setError(operr("Unsupported RemovalPolicy[%s] type", removalPolicy.toString()));
        bus.reply(msg, reply);
    }

    private void handle(APIExecuteAutoScalingRuleMsg msg) {
        APIExecuteAutoScalingRuleEvent event = new APIExecuteAutoScalingRuleEvent(msg.getId());

        ExecuteAutoScalingGroupRuleMsg executeRuleMsg = new ExecuteAutoScalingGroupRuleMsg();
        executeRuleMsg.setAutoScalingGroupUuid(msg.getAutoScalingGroupUuid());
        executeRuleMsg.setAutoScalingRuleUuid(msg.getUuid());
        bus.makeTargetServiceIdByResourceUuid(executeRuleMsg, AutoScalingConstants.SERVICE_ID, msg.getAutoScalingGroupUuid());

        bus.send(executeRuleMsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    event.setError(reply.getError());
                    bus.publish(event);
                    return;
                }

                ExecuteAutoScalingGroupRuleReply rly = (ExecuteAutoScalingGroupRuleReply)reply;
                event.setScalingActivityUuid(rly.getAutoScalingGroupActivityUuid());
                bus.publish(event);
            }
        });
    }

    private void handle(ExecuteAutoScalingGroupRuleMsg msg) {
        ExecuteAutoScalingGroupRuleReply reply = new ExecuteAutoScalingGroupRuleReply();

        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getName() {
                return String.format("execute-autoScalingRule-%s", msg.getAutoScalingRuleUuid());
            }

            @Override
            public String getSyncSignature() {
                return String.format("%s-%s", ruleSyncThreadName, msg.getAutoScalingRuleUuid());
            }

            @Override
            public void run(SyncTaskChain chain) {
                String ruleUuid = msg.getAutoScalingRuleUuid();
                AutoScalingRuleVO ruleVO = dbf.findByUuid(ruleUuid, AutoScalingRuleVO.class);
                AutoScalingRuleFactory factory = autoScalingManager.getAutoScalingRuleFactory(ruleVO.getType());

                CreateAutoScalingGroupActivityMsg createAutoScalingGroupGroupActivityMsg = factory.makeAutoScalingGroupActivity(ruleVO.getUuid());
                createAutoScalingGroupGroupActivityMsg.setCause(AutoScalingGroupActivityCause.ManualOperation.toString());
                bus.send(createAutoScalingGroupGroupActivityMsg, new CloudBusCallBack(msg, chain) {
                    @Override
                    public void run(MessageReply rly) {
                        if (!rly.isSuccess()) {
                            reply.setError(rly.getError());
                            bus.reply(msg, reply);
                            chain.next();
                            return;
                        }

                        CreateAutoScalingGroupActivityReply r = (CreateAutoScalingGroupActivityReply) rly;
                        reply.setAutoScalingGroupActivityUuid(r.getAutoScalingGroupActivityUuid());
                        bus.reply(msg, reply);
                        chain.next();
                    }
                });
            }
        });
    }

    private void handle(APIUpdateAutoScalingGroupInstanceMsg msg) {
        APIUpdateAutoScalingGroupInstanceEvent event = new APIUpdateAutoScalingGroupInstanceEvent(msg.getId());

        if (msg.getProtectionStrategy() != null) {
            SQL.New(AutoScalingGroupInstanceVO.class)
                    .set(AutoScalingGroupInstanceVO_.protectionStrategy, msg.getProtectionStrategy())
                    .eq(AutoScalingGroupInstanceVO_.instanceUuid, msg.getInstanceUuid())
                    .eq(AutoScalingGroupInstanceVO_.scalingGroupUuid, msg.getAutoScalingGroupUuid())
                    .update();
        }

        AutoScalingGroupInstanceVO instanceVO = Q.New(AutoScalingGroupInstanceVO.class)
                .eq(AutoScalingGroupInstanceVO_.instanceUuid, msg.getInstanceUuid())
                .eq(AutoScalingGroupInstanceVO_.scalingGroupUuid, msg.getAutoScalingGroupUuid())
                .find();
        event.setInventory(AutoScalingGroupInstanceInventory.valueOf(instanceVO));

        bus.publish(event);
    }
}
