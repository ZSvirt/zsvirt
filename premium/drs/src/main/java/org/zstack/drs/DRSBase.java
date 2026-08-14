package org.zstack.drs;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.Platform;
import org.zstack.core.asyncbatch.While;
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
import org.zstack.drs.algorithm.DefaultBalanceAlgorithm;
import org.zstack.drs.api.*;
import org.zstack.drs.data.HostLoad;
import org.zstack.drs.data.*;
import org.zstack.drs.entity.*;
import org.zstack.drs.header.DRSErrors;
import org.zstack.drs.message.*;
import org.zstack.drs.util.DRSActivityConcurrencyControl;
import org.zstack.drs.util.DRSUtils;
import org.zstack.header.allocator.HostCapacityVO;
import org.zstack.header.core.Completion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.host.HostState;
import org.zstack.header.host.HostStatus;
import org.zstack.header.host.HostVO;
import org.zstack.header.host.HostVO_;
import org.zstack.header.identity.AccountConstant;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.vm.*;
import org.zstack.identity.AccountManager;
import org.zstack.resourceconfig.ResourceConfigFacade;
import org.zstack.tag.SystemTagCreator;
import org.zstack.tag.TagManager;
import org.zstack.utils.ObjectUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;
import org.zstack.zwatch.datatype.Label;
import org.zstack.zwatch.namespace.HostNamespace;
import org.zstack.zwatch.ruleengine.AnyAlgorithm;
import org.zstack.zwatch.ruleengine.ComparisonOperator;
import org.zstack.zwatch.ruleengine.MetricRule;
import org.zstack.zwatch.ruleengine.RuleEvaluationResult;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.*;
import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;
import static org.zstack.utils.CollectionUtils.*;

/**
 * Created by lining on 2019/12/12.
 */

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class DRSBase {
    protected static final CLogger logger = Utils.getLogger(DRSBase.class);

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
    private transient AccountManager acntMgr;
    @Autowired
    private TagManager tagMgr;
    @Autowired
    protected CascadeFacade casf;
    @Autowired
    private ResourceConfigFacade rcf;

    protected ClusterDRSVO self;
    protected ClusterDRSVO originalCopy;
    private String syncThreadName;
    private String migratingVmFlag;
    private String schedulingFlag;

    protected ClusterDRSVO getSelf() {
        return self;
    }

    protected ClusterDRSInventory getSelfInventory() {
        return ClusterDRSInventory.valueOf(self);
    }

    public DRSBase(ClusterDRSVO vo) {
        this.self = vo;
        this.syncThreadName = "drs-" + vo.getUuid();
        this.migratingVmFlag = String.format("drs-%s-migrating-vm", vo.getUuid());
        this.schedulingFlag = String.format("drs-%s-scheduling", vo.getUuid());
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
        if (msg instanceof APIUpdateClusterDRSMsg) {
            handle((APIUpdateClusterDRSMsg) msg);
        } else if (msg instanceof APIExecuteDRSSchedulingMsg) {
            handle((APIExecuteDRSSchedulingMsg) msg);
        } else if (msg instanceof APIApplyDRSAdviceMsg) {
            handle((APIApplyDRSAdviceMsg) msg);
        } else if (msg instanceof APIGetClusterDRSStatusMsg) {
            handle((APIGetClusterDRSStatusMsg) msg);
        } else if (msg instanceof APIDeleteClusterDRSMsg) {
            handle((APIDeleteClusterDRSMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handleLocalMessage(Message msg) {
        if (msg instanceof ExecuteDRSSchedulingMsg) {
            handle((ExecuteDRSSchedulingMsg) msg);
        } else if (msg instanceof CreateDRSVmMigrationActivityMsg) {
            handle((CreateDRSVmMigrationActivityMsg) msg);
        } else if (msg instanceof DeleteClusterDRSMsg) {
            handle((DeleteClusterDRSMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(APIUpdateClusterDRSMsg msg) {
        APIUpdateClusterDRSEvent event = new APIUpdateClusterDRSEvent(msg.getId());

        ClusterDRSVO vo = dbf.findByUuid(msg.getUuid(), ClusterDRSVO.class);
        boolean setLastAdviceGroupToNull = false;

        if (msg.getName() != null) {
            vo.setName(msg.getName());
        }

        if (msg.getDescription() != null) {
            vo.setDescription(msg.getDescription());
        }

        if (msg.getState() != null) {
            vo.setState(DRSState.valueOf(msg.getState()));
        }

        if (msg.getThresholds() != null) {
            vo.setThresholds(JSONObjectUtil.toJsonString(msg.getThresholds()));
            setLastAdviceGroupToNull = true;
        }

        if (msg.getThresholdDuration() != null) {
            vo.setThresholdDuration(msg.getThresholdDuration());
            setLastAdviceGroupToNull = true;
        }

        if (msg.getAutomationLevel() != null) {
            vo.setAutomationLevel(DRSAutomationLevel.valueOf(msg.getAutomationLevel()));
        }

        if (setLastAdviceGroupToNull) {
            vo.setLastAdviceGroupUuid(null);
            vo.setBalancedState(BalancedState.Unknown);
        }

        vo = dbf.updateAndRefresh(vo);
        event.setInventory(ClusterDRSInventory.valueOf(vo));
        bus.publish(event);
    }

    private void handle(APIExecuteDRSSchedulingMsg msg) {
        APIExecuteDRSSchedulingEvent event = new APIExecuteDRSSchedulingEvent(msg.getId());

        ExecuteDRSSchedulingMsg localMsg = new ExecuteDRSSchedulingMsg();
        localMsg.setDrsUuid(msg.getDRSUuid());
        bus.makeTargetServiceIdByResourceUuid(localMsg, DRSConstants.SERVICE_ID, msg.getDRSUuid());
        bus.send(localMsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    event.setError(reply.getError());
                    bus.publish(event);
                    return;
                }

                bus.publish(event);
            }
        });
    }

    private void handle(APIApplyDRSAdviceMsg msg) {
        APIApplyDRSAdviceEvent event = new APIApplyDRSAdviceEvent(msg.getId());

        if (DRSActivityConcurrencyControl.isExists(schedulingFlag)) {
            event.setError(err(DRSErrors.DRS_EXECUTE_FAILURE, "Advice not allowed while scheduling"));
            bus.publish(event);
            return;
        }

        DRSActivityConcurrencyControl.addToken(migratingVmFlag);
        DRSAdviceVO adviceVO = dbf.findByUuid(msg.getAdviceUuid(), DRSAdviceVO.class);

        CreateDRSVmMigrationActivityMsg localMsg = new CreateDRSVmMigrationActivityMsg();
        localMsg.setActivityUuid(Platform.getUuid());
        localMsg.setDrsUuid(self.getUuid());
        localMsg.setReason(adviceVO.getReason());
        localMsg.setVmSourceHostUuid(adviceVO.getVmSourceHostUuid());
        localMsg.setVmTargetHostUuid(adviceVO.getVmTargetHostUuid());
        localMsg.setVmUuid(adviceVO.getVmUuid());
        localMsg.setAdviceUuid(msg.getAdviceUuid());
        bus.makeTargetServiceIdByResourceUuid(localMsg, DRSConstants.SERVICE_ID, self.getUuid());

        bus.send(localMsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                DRSActivityConcurrencyControl.removeToken(migratingVmFlag);

                if (!reply.isSuccess()) {
                    event.setError(reply.getError());
                    bus.publish(event);
                    return;
                }

                CreateDRSVmMigrationActivityReply rly = (CreateDRSVmMigrationActivityReply) reply;
                event.setVmMigrationActivityUuid(rly.getActivityUuid());
                bus.publish(event);
            }
        });
    }

    private void handle(APIGetClusterDRSStatusMsg msg) {
        APIGetClusterDRSStatusReply reply = new APIGetClusterDRSStatusReply();

        boolean flag = DRSSystemTags.HOST_LOAD_OVER_THRESHOLD.hasTag(self.getUuid());
        if (!flag) {
            bus.reply(msg, reply);
            return;
        }

        String hostLoadOverThresholdStr = DRSSystemTags.HOST_LOAD_OVER_THRESHOLD.getTokenByResourceUuid(self.getUuid(), DRSSystemTags.HOST_LOAD_OVER_THRESHOLD_TOKEN);
        List<org.zstack.drs.api.HostLoad> hostLoadOverThreshold = JSONObjectUtil.toCollection(hostLoadOverThresholdStr, ArrayList.class, org.zstack.drs.api.HostLoad.class);
        reply.setHostLoadOverThreshold(hostLoadOverThreshold);
        bus.reply(msg, reply);
    }

    private void doDeleteClusterDRS(String clusterDRSUuid, Completion completion) {
        thdf.chainSubmit(new ChainTask(completion) {
            @Override
            public String getSyncSignature() {
                return syncThreadName;
            }

            @Override
            public void run(SyncTaskChain chain) {
                if (isVmBeingMigrated()) {
                    completion.fail(err(DRSErrors.GENERAL_ERROR, "delete DRS is not allowed while the vm is being migrated"));
                    chain.next();
                    return;
                }

                new SQLBatch() {
                    @Override
                    protected void scripts() {
                        sql(DRSVmMigrationActivityVO.class)
                                .eq(DRSVmMigrationActivityVO_.drsUuid, clusterDRSUuid)
                                .hardDelete();

                        sql(DRSAdviceVO.class)
                                .eq(DRSAdviceVO_.drsUuid, clusterDRSUuid)
                                .hardDelete();

                        sql(ClusterDRSVO.class)
                                .eq(ClusterDRSVO_.uuid, clusterDRSUuid)
                                .hardDelete();
                    }
                }.execute();

                completion.success();
                chain.next();
            }

            @Override
            public String getName() {
                return String.format("delete-drs-%s", clusterDRSUuid);
            }
        });
    }

    private void handle(APIDeleteClusterDRSMsg msg) {
        APIDeleteClusterDRSEvent event = new APIDeleteClusterDRSEvent(msg.getId());

        doDeleteClusterDRS(msg.getDRSUuid(), new Completion(msg) {
            @Override
            public void success() {
                bus.publish(event);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                event.setError(errorCode);
                bus.publish(event);
            }
        });
    }

    private void handle(DeleteClusterDRSMsg msg) {
        DeleteClusterDRSReply reply = new DeleteClusterDRSReply();

        doDeleteClusterDRS(msg.getDRSUuid(), new Completion(msg) {
            @Override
            public void success() {
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(ExecuteDRSSchedulingMsg msg) {
        ExecuteDRSSchedulingReply reply = new ExecuteDRSSchedulingReply();

        String clusterUuid = Q.New(ClusterDRSVO.class)
                .select(ClusterDRSVO_.clusterUuid)
                .eq(ClusterDRSVO_.uuid, self.getUuid())
                .findValue();
        List<String> notSupportedReasons = DRSUtils.validateSupportDRS(clusterUuid);
        if (!notSupportedReasons.isEmpty()) {
            String reasons = String.join(";", notSupportedReasons);
            reply.setError(err(DRSErrors.DRS_NOT_SUPPORT, reasons));
            bus.reply(msg, reply);
            return;
        }

        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return syncThreadName;
            }

            @Override
            public void run(SyncTaskChain chain) {
                DRSActivityConcurrencyControl.addToken(schedulingFlag);

                executeDRSScheduling(new Completion(chain) {
                    @Override
                    public void success() {
                        DRSActivityConcurrencyControl.removeToken(schedulingFlag);
                        bus.reply(msg, reply);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        DRSActivityConcurrencyControl.removeToken(schedulingFlag);
                        reply.setError(errorCode);
                        bus.reply(msg, reply);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return String.format("execute-DRS-%s-scheduling", self.getUuid());
            }
        });
    }

    @SuppressWarnings("rawtypes")
    private void executeDRSScheduling(Completion completion) {
        if (isVmBeingMigrated()) {
            completion.fail(err(DRSErrors.DRS_EXECUTE_FAILURE, "Scheduling is not allowed while the vm is being migrated"));
            return;
        }

        List<String> hostUuids = Q.New(HostVO.class)
                .select(HostVO_.uuid)
                .eq(HostVO_.clusterUuid, self.getClusterUuid())
                .eq(HostVO_.status, HostStatus.Connected)
                .eq(HostVO_.state, HostState.Enabled)
                .listValues();
        if (hostUuids.isEmpty()) {
            completion.fail(err(DRSErrors.DRS_EXECUTE_FAILURE, "DRS[%s] skip scheduling, no enabled and connected host in cluster", self.getUuid()));
            return;
        }

        final String adviceGroupUuid = Platform.getUuid();
        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("execute-DRS-%s-scheduling-with-advice-group-%s", self.getUuid(), adviceGroupUuid));

        logger.debug(String.format("lastAdviceGroupUuid for ClusterDRS[uuid=%s] update to %s", self.getUuid(), adviceGroupUuid));
        SQL.New(ClusterDRSVO.class)
                .eq(ClusterDRSVO_.uuid, self.getUuid())
                .set(ClusterDRSVO_.lastAdviceGroupUuid, adviceGroupUuid)
                .update();

        final DRSSchedulingFlowData flowData = new DRSSchedulingFlowData();
        flowData.setHostUuids(hostUuids);

        chain.then(new NoRollbackFlow() {
            String __name__ = "obtain-cluster-load";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                GetClusterHostsLoadMsg getClusterHostsLoadMsg = new GetClusterHostsLoadMsg();
                List<String> vmUuids = Q.New(VmInstanceVO.class)
                        .select(VmInstanceVO_.uuid)
                        .in(VmInstanceVO_.hostUuid, flowData.getHostUuids())
                        .eq(VmInstanceVO_.state, VmInstanceState.Running)
                        .eq(VmInstanceVO_.type, VmInstanceConstant.USER_VM_TYPE)
                        .listValues();

                getClusterHostsLoadMsg.setClusterUuid(self.getClusterUuid());
                getClusterHostsLoadMsg.setHostUuids(flowData.getHostUuids());
                getClusterHostsLoadMsg.setVmUuids(vmUuids);
                bus.makeTargetServiceIdByResourceUuid(getClusterHostsLoadMsg, DRSConstants.SERVICE_ID, self.getUuid());
                bus.send(getClusterHostsLoadMsg, new CloudBusCallBack(trigger) {
                    @Override
                    public void run(MessageReply rly) {
                        if (!rly.isSuccess()) {
                            SQL.New(ClusterDRSVO.class)
                                    .eq(ClusterDRSVO_.uuid, self.getUuid())
                                    .set(ClusterDRSVO_.balancedState, BalancedState.Unknown)
                                    .update();
                            trigger.fail(rly.getError());
                            return;
                        }

                        GetClusterHostsLoadReply r = (GetClusterHostsLoadReply) rly;
                        flowData.setHostLoads(r.getHostLoads());
                        trigger.next();
                    }
                });
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "calculate-DRS-cluster-load-data";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                List<String> hostUuids = flowData.getHostUuids();

                if (isHostLoadOverThreshold(hostUuids)) {
                    SQL.New(ClusterDRSVO.class)
                            .eq(ClusterDRSVO_.uuid, self.getUuid())
                            .set(ClusterDRSVO_.balancedState, BalancedState.Unbalanced)
                            .update();
                    flowData.setHostLoadOverThreshold(true);
                } else {
                    SQL.New(ClusterDRSVO.class)
                            .eq(ClusterDRSVO_.uuid, self.getUuid())
                            .set(ClusterDRSVO_.balancedState, BalancedState.Balanced)
                            .update();
                    if (DRSSystemTags.HOST_LOAD_OVER_THRESHOLD.hasTag(self.getUuid())) {
                        DRSSystemTags.HOST_LOAD_OVER_THRESHOLD.delete(self.getUuid());
                    }
                    flowData.setHostLoadOverThreshold(false);
                }

                logger.debug(String.format("hostLoadOverThreshold is %s in DRS cluster[uuid=%s]",
                        flowData.getHostLoadOverThreshold(), self.getUuid()));
                trigger.next();
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "determine-VM-migration-tasks";

            @Override
            public boolean skip(Map data) {
                return !flowData.getHostLoadOverThreshold();
            }

            @Override
            public void run(FlowTrigger trigger, Map data) {
                List<HostLoad> hostLoads = filter(flowData.getHostLoads(),
                        hostLoad -> hostLoad.getUsedCPUPercent() != null && hostLoad.getUsedPhysicalMemoryBit() != null);

                if (hostLoads.isEmpty()) {
                    SQL.New(ClusterDRSVO.class)
                            .eq(ClusterDRSVO_.uuid, self.getUuid())
                            .set(ClusterDRSVO_.balancedState, BalancedState.Unknown)
                            .update();
                    trigger.fail(err(DRSErrors.GENERAL_ERROR, "Lack of host CPU, memory monitoring data"));
                    return;
                }

                Float cpuUsedPercentThreshold = DRSUtils.getCPUUsedPercentThreshold(self.getUuid());
                Float memUsedPercentThreshold = DRSUtils.getMemUsedPercentThreshold(self.getUuid());

                // filter out base on vm metric data
                buildHostVmList(hostLoads, cpuUsedPercentThreshold, memUsedPercentThreshold);
                List<HostNode> hostNodes = buildHostNodes(hostLoads);
                saveHostLoadOverThreshold(hostNodes, cpuUsedPercentThreshold, memUsedPercentThreshold);

                DefaultBalanceAlgorithm balancer = new DefaultBalanceAlgorithm();
                List<MigrateTask> migrateTasks = balancer.makeTasks(hostNodes, cpuUsedPercentThreshold, memUsedPercentThreshold);
                if (migrateTasks.isEmpty()) {
                    logger.debug("skip executeDRSScheduling, migrateTasks is empty");
                    trigger.next();
                    return;
                }

                if (DRSAutomationLevel.Automatic == self.getAutomationLevel()) {
                    makeMigrationActivities(migrateTasks, hostNodes);
                } else if (DRSAutomationLevel.Manual == self.getAutomationLevel()) {
                    makeAdvice(adviceGroupUuid, migrateTasks, hostNodes);
                } else {
                    throw new CloudRuntimeException(String.format("unknown level[%s]", self.getAutomationLevel()));
                }

                trigger.next();
            }
        });

        chain.done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                completion.success();
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errorCode, Map data) {
                completion.fail(errorCode);
            }
        }).start();
    }

    private void buildHostVmList(List<HostLoad> hostLoads, Float cpuUsedPercentThreshold, Float memUsedPercentThreshold) {
        if (cpuUsedPercentThreshold != null && memUsedPercentThreshold == null) {
            for (HostLoad hostLoad : hostLoads) {
                List<HostLoadOccupiedByVm> vmList = hostLoad.getVmList().stream()
                        .filter(vm -> vm.getUsedHostCPUPercent() != null)
                        .collect(Collectors.toList());
                hostLoad.setVmList(vmList);
            }
        } else if (cpuUsedPercentThreshold == null && memUsedPercentThreshold != null) {
            for (HostLoad hostLoad : hostLoads) {
                List<HostLoadOccupiedByVm> vmList = hostLoad.getVmList().stream()
                        .filter(vm -> vm.getUsedHostPhysicalMemoryBit() != null)
                        .collect(Collectors.toList());
                hostLoad.setVmList(vmList);
            }
        } else if (cpuUsedPercentThreshold != null) {
            for (HostLoad hostLoad : hostLoads) {
                List<HostLoadOccupiedByVm> vmList = hostLoad.getVmList().stream()
                        .filter(vm -> vm.getUsedHostCPUPercent() != null && vm.getUsedHostPhysicalMemoryBit() != null)
                        .collect(Collectors.toList());
                hostLoad.setVmList(vmList);
            }
        }
    }

    private boolean isHostLoadOverThreshold(List<String> hostUuids) {
        Float cpuUsedPercentThreshold = DRSUtils.getCPUUsedPercentThreshold(self.getUuid());
        Float memUsedPercentThreshold = DRSUtils.getMemUsedPercentThreshold(self.getUuid());

        MetricRule rule = new MetricRule(self.getUuid());
        rule.setComparisonOperator(ComparisonOperator.GreaterThanOrEqualTo);
        List<Label> labels = new ArrayList<>();
        Label label = new Label(Label.Operator.Regex.toString());
        label.setKey(HostNamespace.LabelNames.HostUuid.toString());
        label.setValue(String.join("|", hostUuids));
        labels.add(label);
        rule.setLabels(labels);
        rule.setDuration(self.getThresholdDuration());
        rule.setNamespaceName(String.format("ZStack/%s", HostNamespace.NAME));
        rule.setAccountUuid(AccountConstant.INITIAL_SYSTEM_ADMIN_UUID);
        rule.setState(RuleEvaluationResult.RuleEvaluationState.OK);
        rule.setAlgorithm(new AnyAlgorithm());

        if (cpuUsedPercentThreshold != null) {
            boolean flag = isHostCPUUsedOverThreshold(rule, cpuUsedPercentThreshold);
            if (flag) {
                return true;
            }
        }

        if (memUsedPercentThreshold != null) {
            boolean flag = isHostMemoryUsedOverThreshold(rule, memUsedPercentThreshold);
            if (flag) {
                return true;
            }
        }

        return false;
    }

    private boolean isHostCPUUsedOverThreshold(MetricRule rule, Float threshold) {
        rule.setThreshold(threshold);
        rule.setMetricName(HostNamespace.CPUAllUsedUtilization.getName());
        List<RuleEvaluationResult> ruleEvaluationResults = rule.eval();

        for (RuleEvaluationResult result : ruleEvaluationResults) {
            if (result.getCurrentState() == RuleEvaluationResult.RuleEvaluationState.Problem) {
                return true;
            }
        }

        return false;
    }

    private boolean isHostMemoryUsedOverThreshold(MetricRule rule, Float threshold) {
        rule.setThreshold(threshold);
        rule.setMetricName(HostNamespace.MemoryUsedInPercent.getName());
        List<RuleEvaluationResult> ruleEvaluationResults = rule.eval();

        for (RuleEvaluationResult result : ruleEvaluationResults) {
            if (result.getCurrentState() == RuleEvaluationResult.RuleEvaluationState.Problem) {
                return true;
            }
        }

        return false;
    }

    private boolean isVmBeingMigrated() {
        return DRSActivityConcurrencyControl.isExists(migratingVmFlag);
    }

    private void saveHostLoadOverThreshold(List<HostNode> hostNodes, Float cpuUsedPercentThreshold, Float memUsedPercentThreshold) {
        if (hostNodes.isEmpty()) {
            return;
        }

        List<org.zstack.drs.api.HostLoad> result = new ArrayList<>();

        for (HostNode h : hostNodes) {
            boolean flag = false;

            if (cpuUsedPercentThreshold != null && h.getUsedCPUPercent() >= cpuUsedPercentThreshold) {
                flag = true;
            }

            if (memUsedPercentThreshold != null && h.getUsedMemoryPercent() >= memUsedPercentThreshold) {
                flag = true;
            }

            if (!flag) {
                continue;
            }

            org.zstack.drs.api.HostLoad hostLoad = new org.zstack.drs.api.HostLoad();
            hostLoad.setHostUuid(h.getUuid());
            hostLoad.setUsedCPUPercent(h.getUsedCPUPercent());
            hostLoad.setUsedMemoryPercent(h.getUsedMemoryPercent());
            result.add(hostLoad);
        }

        if (result.isEmpty()) {
            if (DRSSystemTags.HOST_LOAD_OVER_THRESHOLD.hasTag(self.getUuid())) {
                DRSSystemTags.HOST_LOAD_OVER_THRESHOLD.delete(self.getUuid());
            }
            return;
        }

        String hostLoadOverThresholdStr = JSONObjectUtil.toJsonString(result);
        SystemTagCreator creator = DRSSystemTags.HOST_LOAD_OVER_THRESHOLD.newSystemTagCreator(self.getUuid());
        creator.inherent = false;
        creator.recreate = true;
        creator.unique = true;
        creator.setTagByTokens(map(e(DRSSystemTags.HOST_LOAD_OVER_THRESHOLD_TOKEN, hostLoadOverThresholdStr)));
        creator.create();
    }

    private List<HostNode> buildHostNodes(List<HostLoad> hostLoads) {
        List<HostNode> hostNodes = new ArrayList<>();

        for (HostLoad hostLoad : hostLoads) {
            HostCapacityVO hostCapacityVO = dbf.findByUuid(hostLoad.getHostUuid(), HostCapacityVO.class);
            HostNode hostNode = new HostNode(
                    hostLoad.getHostUuid(),
                    hostLoad.getUsedCPUPercent(),
                    hostLoad.getUsedPhysicalMemoryBit(),
                    hostCapacityVO.getAvailableCpu(),
                    hostCapacityVO.getAvailablePhysicalMemory(),
                    hostCapacityVO.getTotalMemory()
            );

            for (HostLoadOccupiedByVm vmLoad : hostLoad.getVmList()) {
                VmInstanceVO vmInstanceVO = dbf.findByUuid(vmLoad.getVmUuid(), VmInstanceVO.class);
                float usedMemoryPercent = vmLoad.getUsedHostPhysicalMemoryBit() == null ? 0.0f : (1.0f * vmLoad.getUsedHostPhysicalMemoryBit() / hostNode.getTotalMemoryBit()) * 100;
                float usedHostCPUPercent = vmLoad.getUsedHostCPUPercent() == null ? 0.0f : (vmLoad.getUsedHostCPUPercent() / hostCapacityVO.getCpuNum()) * 100;
                VmNode vmNode = new VmNode(
                        vmLoad.getVmUuid(),
                        usedHostCPUPercent,
                        vmLoad.getUsedHostPhysicalMemoryBit() == null ? 0L : vmLoad.getUsedHostPhysicalMemoryBit(),
                        usedMemoryPercent,
                        vmInstanceVO.getCpuNum(),
                        vmInstanceVO.getMemorySize()
                );
                hostNode.getVmList().add(vmNode);
            }

            hostNodes.add(hostNode);
        }

        return hostNodes;
    }

    private void makeAdvice(String adviceGroupUuid, List<MigrateTask> migrateTasks, List<HostNode> hostNodes) {
        List<DRSAdviceVO> adviceVOS = new ArrayList<>();
        for (MigrateTask migrateTask : migrateTasks) {
            DRSAdviceVO adviceVO = new DRSAdviceVO();
            adviceVO.setUuid(Platform.getUuid());
            adviceVO.setAdviceGroupUuid(adviceGroupUuid);
            adviceVO.setVmUuid(migrateTask.getVmUuid());
            adviceVO.setVmTargetHostUuid(migrateTask.getTargetHostUuid());
            adviceVO.setVmSourceHostUuid(migrateTask.getSourceHostUuid());
            adviceVO.setDrsUuid(self.getUuid());
            HostNode hostNode = hostNodes.stream().filter(h -> h.getUuid().equals(migrateTask.getSourceHostUuid())).findAny().orElse(null);
            adviceVO.setReason(hostNode == null ? null : makeReason(hostNode));
            adviceVOS.add(adviceVO);
        }

        dbf.persistCollection(adviceVOS);
    }

    private String makeReason(HostNode hostNode) {
        List<String> reasons = new ArrayList<>();

        Float cpuUsedPercentThreshold = DRSUtils.getCPUUsedPercentThreshold(self.getUuid());
        Float memUsedPercentThreshold = DRSUtils.getMemUsedPercentThreshold(self.getUuid());
        if (cpuUsedPercentThreshold != null) {
            if (hostNode.getUsedCPUPercent() >= cpuUsedPercentThreshold) {
                reasons.add(i18n("Host CPU usage is %f%%, exceeding %f%%",
                        hostNode.getUsedCPUPercent(), cpuUsedPercentThreshold));
            }
        }

        if (memUsedPercentThreshold != null) {
            if (hostNode.getUsedMemoryPercent() >= memUsedPercentThreshold) {
                reasons.add(i18n("Host memory usage is %f%%, exceeding %f%%",
                        hostNode.getUsedMemoryPercent(), memUsedPercentThreshold));
            }
        }

        return String.join("\n", reasons);
    }

    private void makeMigrationActivities(List<MigrateTask> migrateTasks, List<HostNode> hostNodes) {
        assert !migrateTasks.isEmpty();

        DRSActivityConcurrencyControl.addToken(migratingVmFlag);
        List<CreateDRSVmMigrationActivityMsg> msgs = new ArrayList<>();
        for (MigrateTask migrateTask : migrateTasks) {
            CreateDRSVmMigrationActivityMsg msg = new CreateDRSVmMigrationActivityMsg();
            msg.setActivityUuid(Platform.getUuid());
            msg.setDrsUuid(self.getUuid());
            msg.setVmSourceHostUuid(migrateTask.getSourceHostUuid());
            msg.setVmTargetHostUuid(migrateTask.getTargetHostUuid());
            msg.setVmUuid(migrateTask.getVmUuid());
            HostNode hostNode = findOneOrNull(hostNodes, h -> h.getUuid().equals(migrateTask.getSourceHostUuid()));
            msg.setReason(hostNode == null ? null : makeReason(hostNode));
            bus.makeTargetServiceIdByResourceUuid(msg, DRSConstants.SERVICE_ID, self.getUuid());
            msgs.add(msg);
        }

        new While<>(msgs).step((msg, completion) -> {
            bus.send(msg, new CloudBusCallBack(completion) {
                @Override
                public void run(MessageReply rly) {
                    if (!rly.isSuccess()) {
                        logger.info(rly.getError().toString());
                    }
                    completion.done();
                }
            });
        }, 5).run(new WhileDoneCompletion(null) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                DRSActivityConcurrencyControl.removeToken(migratingVmFlag);
            }
        });
    }

    private void handle(CreateDRSVmMigrationActivityMsg msg) {
        CreateDRSVmMigrationActivityReply reply = new CreateDRSVmMigrationActivityReply();

        String uuid = msg.getActivityUuid() != null ? msg.getActivityUuid() : Platform.getUuid();
        DRSVmMigrationActivityVO activityVO = new DRSVmMigrationActivityVO();
        activityVO.setUuid(uuid);
        activityVO.setReason(msg.getReason());
        activityVO.setAdviceUuid(msg.getAdviceUuid());
        activityVO.setCause(msg.getAdviceUuid() != null ?
                DRSVmMigrationActivityCause.ApplyAdvice :
                DRSVmMigrationActivityCause.Automatic);
        activityVO.setStatus(DRSVmMigrationStatus.Created);
        activityVO.setDrsUuid(msg.getDrsUuid());
        activityVO.setVmUuid(msg.getVmUuid());
        activityVO.setVmSourceHostUuid(msg.getVmSourceHostUuid());
        activityVO.setVmTargetHostUuid(msg.getVmTargetHostUuid());
        activityVO.setCreateDate(new Timestamp(System.currentTimeMillis()));

        activityVO = dbf.persistAndRefresh(activityVO);
        reply.setActivityUuid(activityVO.getUuid());

        DRSVmMigrationActivityVO finalActivityVO = activityVO;
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return String.format("drs-%s-migrate-vm", self.getUuid());
            }

            @Override
            public int getSyncLevel() {
                return rcf.getResourceConfigValue(DRSGlobalConfig.DRS_MIGRATE_VM_CONCURRENT, self.getUuid(), Integer.class);
            }

            @Override
            public void run(SyncTaskChain chain) {
                doMigrateVm(finalActivityVO, new Completion(msg, chain) {
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

            @Override
            public String getName() {
                return String.format("drs-%s-migrate-vm-%s", self.getUuid(), finalActivityVO.getVmUuid());
            }
        });
    }

    private void doMigrateVm(DRSVmMigrationActivityVO activityVO, final Completion completion) {
        activityVO.setStatus(DRSVmMigrationStatus.InProgress);
        dbf.updateAndRefresh(activityVO);

        MigrateVmMsg msg = new MigrateVmMsg();
        msg.setVmInstanceUuid(activityVO.getVmUuid());
        msg.setTargetHostUuid(activityVO.getVmTargetHostUuid());
        bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, activityVO.getVmUuid());
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    activityVO.setStatus(DRSVmMigrationStatus.Failed);
                    activityVO.setResult(simplifyErrorCode(1024, reply.getError()));
                    activityVO.setEndDate(new Timestamp(System.currentTimeMillis()));
                    dbf.updateAndRefresh(activityVO);
                    completion.fail(reply.getError());
                    return;
                }

                activityVO.setStatus(DRSVmMigrationStatus.Successful);
                activityVO.setEndDate(new Timestamp(System.currentTimeMillis()));
                dbf.updateAndRefresh(activityVO);
                completion.success();
            }
        });
    }

    private String simplifyErrorCode(int maxSize, ErrorCode errorCode) {
        if (errorCode == null) {
            return "[???] empty error";
        }

        String content = errorCode.getReadableDetails();
        return content.length() > maxSize ? content.substring(0, maxSize - 3) + "..." : content;
    }
}
