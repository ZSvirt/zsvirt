package org.zstack.autoscaling.group.instance.vm;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.autoscaling.AutoScalingConstants;
import org.zstack.autoscaling.AutoScalingGlobalConfig;
import org.zstack.autoscaling.group.AutoScalingGroupState;
import org.zstack.autoscaling.group.AutoScalingGroupSystemTags;
import org.zstack.autoscaling.group.ScalingResourceType;
import org.zstack.autoscaling.group.activity.AutoScalingGroupActivityAction;
import org.zstack.autoscaling.group.activity.AutoScalingGroupActivityCause;
import org.zstack.autoscaling.group.activity.CreateAutoScalingGroupActivityMsg;
import org.zstack.autoscaling.group.activity.action.AutoScalingGroupRemoveInstancesActionMsg;
import org.zstack.autoscaling.group.instance.*;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.*;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.thread.AsyncThread;
import org.zstack.core.thread.PeriodicTask;
import org.zstack.core.thread.SyncTask;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.Component;
import org.zstack.header.managementnode.ManagementNodeReadyExtensionPoint;
import org.zstack.header.message.MessageReply;
import org.zstack.header.vm.*;
import org.zstack.network.service.lb.LoadBalancerManager;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.zwatch.datatype.Datapoint;
import org.zstack.zwatch.datatype.Label;
import org.zstack.zwatch.datatype.MetricQueryObject;
import org.zstack.zwatch.datatype.Namespace;
import org.zstack.zwatch.namespace.LoadBalancerNamespace;

import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.zstack.autoscaling.AutoScalingConstants.AutoScalingGroupInstance.PROTECTION_STRATEGY_PROTECTED;

/**
 * Created by lining on 2018/9/30.
 */
public class AutoScalingGroupVmInstanceHealthManagerImpl implements AutoScalingGroupInstanceHealthManager, Component, ManagementNodeReadyExtensionPoint {
    protected static final CLogger logger = Utils.getLogger(AutoScalingGroupVmInstanceHealthManagerImpl.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private EventFacade evf;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ThreadFacade thdf;
    @Autowired
    private ResourceDestinationMaker destinationMaker;
    @Autowired
    private LoadBalancerManager lbMgr;

    private Future removeUnhealthyInstanceTask;
    private Set<String> scalingGroupInRemovingUnhealthyInstance = Collections.synchronizedSet(new HashSet<String>());

    private Future checkVmNicLoadBalancerListenerHealthStatusTask;
    private Set<String> autoScalingGroupInCheckingVmNicHealthStatus = Collections.synchronizedSet(new HashSet<String>());

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public void managementNodeReady() {
        AutoScalingGlobalConfig.REMOVE_UNHEALTHY_INSTANCE_INTERVAL.installUpdateExtension((oldConfig, newConfig) -> startRemoveUnhealthyInstanceTask());

        installInstanceStateEventListener();
        startRemoveUnhealthyInstanceTask();
        startCheckLoadBalancerListenerVmNicHealthStateTask();

        handleAlwaysInCreatingStatusVmInstances();
    }

    @Override
    public boolean stop() {
        if (removeUnhealthyInstanceTask != null) {
            removeUnhealthyInstanceTask.cancel(true);
        }
        return true;
    }

    @Override
    public void installInstanceStateEventListener() {
        evf.on(VmCanonicalEvents.VM_FULL_STATE_CHANGED_PATH, new EventCallback() {
            @Override
            protected void run(Map tokens, Object data) {
                VmCanonicalEvents.VmStateChangedData d = (VmCanonicalEvents.VmStateChangedData) data;
                processVmStateChange(d.getVmUuid(), d.getNewState());
            }
        });
    }

    /**
     * What conditions may generate anomalous instance data?
     * case: Vm is being created, the management node is manually stopped
     * case: Vm is being created, there is a problem with the MN database
     */
    @AsyncThread
    private void handleAlwaysInCreatingStatusVmInstances() {
        List<String> vmInstanceUuids = SQL.New("select t0.instanceUuid from AutoScalingGroupInstanceVO t0, AutoScalingGroupVO t1" +
                " where t0.scalingGroupUuid = t1.uuid" +
                " and t1.state = :groupState" +
                " and t1.scalingResourceType = :resourceType" +
                " and t0.status = :instanceStatus")
                .param("groupState", AutoScalingGroupState.Enabled)
                .param("resourceType", ScalingResourceType.VmInstance)
                .param("instanceStatus", AutoScalingGroupInstanceStatus.Creating)
                .list();

        if (vmInstanceUuids.isEmpty()) {
            return;
        }

        logger.warn(String.format("Update vms%s that is always in the creation is unhealthy", vmInstanceUuids.toString()));
        SQL.New(AutoScalingGroupInstanceVO.class)
                .in(AutoScalingGroupInstanceVO_.instanceUuid, vmInstanceUuids)
                .set(AutoScalingGroupInstanceVO_.healthStatus, AutoScalingGroupInstanceHealthStatus.Unhealthy)
                .update();
    }

    private void processVmStateChange(String vmUuid, String vmNewState) {
        if (!Arrays.asList(VmInstanceState.Stopped.toString(), VmInstanceState.Destroyed.toString(),
                VmInstanceState.Unknown.toString(), VmInstanceState.Running.toString()).contains(vmNewState)) {
            return;
        }

        List<String> groupUuids = SQL.New("select t0.uuid from AutoScalingGroupVO t0, AutoScalingGroupInstanceVO t1" +
                " where t0.scalingResourceType = :resourceType and t0.uuid = t1.scalingGroupUuid" +
                " and t1.instanceUuid = :vmUuid" +
                " and t1.status != :instanceStatus")
                .param("resourceType", ScalingResourceType.VmInstance)
                .param("vmUuid", vmUuid)
                .param("instanceStatus", AutoScalingGroupInstanceStatus.Creating)
                .limit(1)
                .list();
        if (groupUuids == null || groupUuids.isEmpty()) {
            return;
        }
        String groupUuid = groupUuids.get(0);

        if (Arrays.asList(VmInstanceState.Destroyed.toString(), VmInstanceState.Stopped.toString()).contains(vmNewState)) {
            logger.info(String.format("vm state is %s, modified instance[%s] to unhealthy status", vmNewState, vmUuid));
            updateVmHealthStatus(groupUuid, Collections.singletonList(vmUuid), AutoScalingGroupInstanceHealthStatus.Unhealthy);
            return;
        }

        String vmInstanceHealthStrategy = AutoScalingGroupSystemTags.VM_INSTANCE_HEALTH_STRATEGY.getTokenByResourceUuid(groupUuid, AutoScalingGroupSystemTags.VM_INSTANCE_HEALTH_STRATEGY_TOKEN);
        if (vmInstanceHealthStrategy == null) {
            return;
        }
        AutoScalingGroupVmInstanceHealthStrategy healthStrategy = AutoScalingGroupVmInstanceHealthStrategy.valueOf(vmInstanceHealthStrategy);

        AutoScalingGroupInstanceVO groupInstanceVO = Q.New(AutoScalingGroupInstanceVO.class)
                .eq(AutoScalingGroupInstanceVO_.instanceUuid, vmUuid)
                .find();

        if (VmInstanceState.Running.toString().equals(vmNewState)) {
            processVmRunningState(groupInstanceVO, healthStrategy);
            return;
        }

        if (VmInstanceState.Unknown.toString().equals(vmNewState)) {
            processVmUselessState(groupInstanceVO, healthStrategy);
        }
    }

    private void updateVmHealthStatus(String scalingGroupUuid, List<String> instanceUuids, AutoScalingGroupInstanceHealthStatus status) {
        List<AutoScalingGroupInstanceVO> instanceVOS = Q.New(AutoScalingGroupInstanceVO.class)
                .in(AutoScalingGroupInstanceVO_.instanceUuid, instanceUuids)
                .eq(AutoScalingGroupInstanceVO_.scalingGroupUuid, scalingGroupUuid)
                .notEq(AutoScalingGroupInstanceVO_.healthStatus, status)
                .list();
        if (instanceVOS == null || instanceVOS.isEmpty()) {
            return;
        }
        for (AutoScalingGroupInstanceVO instanceVO : instanceVOS) {
            instanceVO.setHealthStatus(status);
        }
        dbf.updateCollection(instanceVOS);
    }

    private void processVmRunningState(AutoScalingGroupInstanceVO groupInstanceVO, AutoScalingGroupVmInstanceHealthStrategy healthStrategy) {
        if (groupInstanceVO.getHealthStatus() == AutoScalingGroupInstanceHealthStatus.Healthy) {
            return;
        }

        if (healthStrategy == AutoScalingGroupVmInstanceHealthStrategy.LoadBalanceBackendStatus) {
            return;
        }

        if (healthStrategy == AutoScalingGroupVmInstanceHealthStrategy.VmInstanceStatus) {
            groupInstanceVO.setHealthStatus(AutoScalingGroupInstanceHealthStatus.Healthy);
            dbf.updateAndRefresh(groupInstanceVO);
            return;
        }

        if (healthStrategy == AutoScalingGroupVmInstanceHealthStrategy.Any) {
            //TODO
        }
    }

    // VmInstanceState.Unknown
    private void processVmUselessState(AutoScalingGroupInstanceVO groupInstanceVO, AutoScalingGroupVmInstanceHealthStrategy healthStrategy) {
        if (healthStrategy == AutoScalingGroupVmInstanceHealthStrategy.LoadBalanceBackendStatus) {
            return;
        }

        groupInstanceVO.setHealthStatus(AutoScalingGroupInstanceHealthStatus.Unhealthy);
        dbf.updateAndRefresh(groupInstanceVO);
        logger.info(String.format("vm state is Unknown, modified instance[%s] to unhealthy status", groupInstanceVO.getInstanceUuid()));
    }

    private synchronized void startCheckLoadBalancerListenerVmNicHealthStateTask() {
        if (checkVmNicLoadBalancerListenerHealthStatusTask != null) {
            checkVmNicLoadBalancerListenerHealthStatusTask.cancel(true);
            return;
        }

        checkVmNicLoadBalancerListenerHealthStatusTask = thdf.submitPeriodicTask(new PeriodicTask() {
            @Override
            public TimeUnit getTimeUnit() {
                return TimeUnit.SECONDS;
            }

            @Override
            public long getInterval() {
                return AutoScalingGlobalConfig.VMNIC_LOADBALANCERLISTENER_HEALTH_CHECK_INTERVAL.value(Long.class);
            }

            @Override
            public String getName() {
                return "check-the-health-status-of-the-vmNic-in-the-loadBalancerListener-task";
            }

            @Override
            public void run() {
                checkVmNicLoadBalancerListenerHealthStatus();
            }
        });

    }

    private void checkVmNicLoadBalancerListenerHealthStatus() {
        List<String> groupUuids = null;

        if (autoScalingGroupInCheckingVmNicHealthStatus.isEmpty()) {
            groupUuids = SQL.New("select distinct t0.scalingGroupUuid from AutoScalingGroupInstanceVO t0, AutoScalingGroupVO t1" +
                    " where t0.scalingGroupUuid = t1.uuid" +
                    " and t1.state = :groupState" +
                    " and t1.scalingResourceType = :resourceType" +
                    " and t0.status != :instanceStatus")
                    .param("groupState", AutoScalingGroupState.Enabled)
                    .param("resourceType", ScalingResourceType.VmInstance)
                    .param("instanceStatus", AutoScalingGroupInstanceStatus.Creating)
                    .limit(1000)
                    .list();
        } else {
            groupUuids = SQL.New("select distinct t0.scalingGroupUuid from AutoScalingGroupInstanceVO t0, AutoScalingGroupVO t1" +
                    " where t0.scalingGroupUuid = t1.uuid" +
                    " and t1.state = :groupState" +
                    " and t1.uuid not in (:groupUuids)" +
                    " and t1.scalingResourceType = :resourceType" +
                    " and t0.status != :instanceStatus")
                    .param("groupState", AutoScalingGroupState.Enabled)
                    .param("groupUuids", autoScalingGroupInCheckingVmNicHealthStatus)
                    .param("resourceType", ScalingResourceType.VmInstance)
                    .param("instanceStatus", AutoScalingGroupInstanceStatus.Creating)
                    .limit(1000)
                    .list();
        }

        if (groupUuids == null || groupUuids.isEmpty()) {
            return;
        }

        for (String groupUuid : groupUuids) {
            if (autoScalingGroupInCheckingVmNicHealthStatus.contains(groupUuid)) {
                continue;
            }
            autoScalingGroupInCheckingVmNicHealthStatus.add(groupUuid);

            String vmInstanceHealthStrategy = AutoScalingGroupSystemTags.VM_INSTANCE_HEALTH_STRATEGY.getTokenByResourceUuid(groupUuid, AutoScalingGroupSystemTags.VM_INSTANCE_HEALTH_STRATEGY_TOKEN);
            if (vmInstanceHealthStrategy == null) {
                autoScalingGroupInCheckingVmNicHealthStatus.remove(groupUuid);
                continue;
            }
            AutoScalingGroupVmInstanceHealthStrategy healthStrategy = AutoScalingGroupVmInstanceHealthStrategy.valueOf(vmInstanceHealthStrategy);
            if (healthStrategy == AutoScalingGroupVmInstanceHealthStrategy.VmInstanceStatus) {
                autoScalingGroupInCheckingVmNicHealthStatus.remove(groupUuid);
                continue;
            }

            thdf.syncSubmit(new SyncTask<Void>() {
                @Override
                public Void call() {
                    List<AutoScalingGroupInstanceVO> instanceVOS = Q.New(AutoScalingGroupInstanceVO.class)
                            .eq(AutoScalingGroupInstanceVO_.scalingGroupUuid, groupUuid)
                            .list();

                    List<String> instanceUuids = new ArrayList<>();

                    String graceTimeSeconds = AutoScalingGroupSystemTags.VMNIC_LOADBALANCER_LISTENER_HEALTH_CHECK_GRACE_TIME_SECONDS.getTokenByResourceUuid(groupUuid, AutoScalingGroupSystemTags.VMNIC_LOADBALANCER_LISTENER_HEALTH_CHECK_GRACE_TIME_SECONDS_TOKEN);
                    for (AutoScalingGroupInstanceVO instanceVO : instanceVOS) {
                        if (graceTimeSeconds == null) {
                            instanceUuids.add(instanceVO.getInstanceUuid());
                            continue;
                        }

                        long seconds = Long.parseLong(graceTimeSeconds);
                        long millis = TimeUnit.SECONDS.toMillis(seconds);
                        if (new Date().getTime() > instanceVO.getCreateDate().getTime() + millis) {
                            instanceUuids.add(instanceVO.getInstanceUuid());
                        }
                    }

                    if (instanceUuids.isEmpty()) {
                        autoScalingGroupInCheckingVmNicHealthStatus.remove(groupUuid);
                        return null;
                    }

                    try {
                        checkVmNicHealthState(groupUuid, instanceUuids);
                    } catch (Throwable t) {
                        logger.warn("check vmNic loadBalancerListener health status failed", t);
                    } finally {
                        autoScalingGroupInCheckingVmNicHealthStatus.remove(groupUuid);
                    }

                    return null;
                }

                private void checkVmNicHealthState(String scalingGroupUuid, List<String> vmInstanceUuids) {
                    // list vm nic uuid, vm nic ip
                    Map<String, String> vmNicUuidNicIpMap = new HashMap<>();

                    List<VmNicVO> nicVOS = Q.New(VmNicVO.class).in(VmNicVO_.vmInstanceUuid, vmInstanceUuids).list();
                    if (nicVOS == null || nicVOS.isEmpty()) {
                        return;
                    }
                    for (VmNicVO nicVO : nicVOS) {
                        vmNicUuidNicIpMap.put(nicVO.getUuid(), nicVO.getIp());
                    }

                    // list lb listener uuid
                    List<String> vmNicUuids = vmNicUuidNicIpMap.keySet().stream().collect(Collectors.toList());
                    List<String> listenerUuids = lbMgr.getLoadBalancerListenterByVmNics(vmNicUuids);
                    if (listenerUuids == null || listenerUuids.isEmpty()) {
                        return;
                    }

                    // query zwatch metric,find error vm nic
                    long endTime = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis());
                    long startTime = endTime - 1;
                    String vmNicIpListStr = String.join("|" , vmNicUuidNicIpMap.values());
                    String listenerUuidListStr = String.join("|" , listenerUuids);
                    Label label0 = new Label(String.format("%s%s%s", LoadBalancerNamespace.LabelNames.NicIpAddress.toString(), Label.Operator.Regex.toString(), vmNicIpListStr));
                    Label label1 = new Label(String.format("%s%s%s", LoadBalancerNamespace.LabelNames.ListenerUuid.toString(), Label.Operator.Regex.toString(), listenerUuidListStr));
                    String namespace = String.format("ZStack/%s", LoadBalancerNamespace.NAME);
                    MetricQueryObject qo = MetricQueryObject.New()
                            .namespace(namespace)
                            .startTime(startTime)
                            .endTime(endTime)
                            .labels(Arrays.asList(label0, label1))
                            .metricName(LoadBalancerNamespace.LoadBalancerBackendStatus.getName())
                            .build();
                    Namespace ns = Namespace.getMetricNameSpace(namespace, qo.getMetricName());
                    List<Datapoint> data = ns.query(qo);
                    if (data == null || data.isEmpty()) {
                        return;
                    }

                    // update vm instance health state
                    Set<String> unhealthyVmNicIpList = new HashSet<>();
                    Set<String> healthyVmNicIpList = new HashSet<>();
                    for (Datapoint datapoint : data) {
                        if (datapoint.getValue() == 0) {
                            unhealthyVmNicIpList.add(datapoint.getLabels().get(LoadBalancerNamespace.LabelNames.NicIpAddress.toString()));
                        } else if (datapoint.getValue() == 1) {
                            healthyVmNicIpList.add(datapoint.getLabels().get(LoadBalancerNamespace.LabelNames.NicIpAddress.toString()));
                        }
                    }

                    List<String> unhealthyVmUuids = new ArrayList<>();
                    List<String> healthyVmUuids = new ArrayList<>();

                    if (!unhealthyVmNicIpList.isEmpty()) {
                        List<String> vmUuids = Q.New(VmNicVO.class).select(VmNicVO_.vmInstanceUuid)
                                .in(VmNicVO_.ip, unhealthyVmNicIpList)
                                .in(VmNicVO_.vmInstanceUuid, vmInstanceUuids)
                                .listValues();
                        unhealthyVmUuids.addAll(vmUuids);
                    }

                    if (!healthyVmNicIpList.isEmpty()) {
                        List<String> vmUuids = Q.New(VmNicVO.class).select(VmNicVO_.vmInstanceUuid)
                                .in(VmNicVO_.ip, healthyVmNicIpList)
                                .in(VmNicVO_.vmInstanceUuid, vmInstanceUuids)
                                .listValues();
                        healthyVmUuids.addAll(vmUuids);
                    }
                    healthyVmUuids.removeAll(unhealthyVmUuids);

                    if (!unhealthyVmUuids.isEmpty()) {
                        updateVmHealthStatus(scalingGroupUuid, unhealthyVmUuids, AutoScalingGroupInstanceHealthStatus.Unhealthy);
                        logger.info(String.format("LoadBalancerBackendStatus failed, modified instance%s to unhealthy status", unhealthyVmUuids));
                    }

                    if (!healthyVmUuids.isEmpty()) {
                        updateVmHealthStatus(scalingGroupUuid, healthyVmUuids, AutoScalingGroupInstanceHealthStatus.Healthy);
                    }
                }

                @Override
                public String getName() {
                    return getSyncSignature();
                }

                @Override
                public String getSyncSignature() {
                    return "check-vmNic-loadBalancerListener-health-status-task";
                }

                @Override
                public int getSyncLevel() {
                    return AutoScalingGlobalConfig.VMNIC_LOADBALANCERLISTENER_HEALTH_CHECK_THREADS_NUM.value(Integer.class);
                }
            });
        }
    }

    private synchronized void startRemoveUnhealthyInstanceTask() {
        if (removeUnhealthyInstanceTask != null) {
            removeUnhealthyInstanceTask.cancel(true);
        }

        removeUnhealthyInstanceTask = thdf.submitPeriodicTask(new PeriodicTask() {
            @Override
            public TimeUnit getTimeUnit() {
                return TimeUnit.SECONDS;
            }

            @Override
            public long getInterval() {
                return AutoScalingGlobalConfig.REMOVE_UNHEALTHY_INSTANCE_INTERVAL.value(Long.class);
            }

            @Override
            public String getName() {
                return "remove-unhealthy-auto-scaling-group-instance-task";
            }

            @Override
            public void run() {
                handleUnhealthyInstances();
            }
        });
    }

    @Override
    public void handleUnhealthyInstances() {
        List<String> groupUuids = SQL.New("select distinct t0.scalingGroupUuid from AutoScalingGroupInstanceVO t0, AutoScalingGroupVO t1" +
                " where t0.scalingGroupUuid = t1.uuid" +
                " and t1.state = :groupState" +
                " and t0.healthStatus = :healthStatus")
                .param("groupState", AutoScalingGroupState.Enabled)
                .param("healthStatus", AutoScalingGroupInstanceHealthStatus.Unhealthy)
                .limit(1000)
                .list();
        if (groupUuids == null || groupUuids.isEmpty()) {
            return;
        }

        for (String groupUuid : groupUuids) {
            if (!destinationMaker.isManagedByUs(groupUuid)) {
                continue;
            }

            if (scalingGroupInRemovingUnhealthyInstance.contains(groupUuid)) {
                continue;
            }
            scalingGroupInRemovingUnhealthyInstance.add(groupUuid);

            thdf.syncSubmit(new SyncTask<Void>() {
                @Override
                public Void call() {
                    List<String> instanceUuids = Q.New(AutoScalingGroupInstanceVO.class)
                            .select(AutoScalingGroupInstanceVO_.instanceUuid)
                            .eq(AutoScalingGroupInstanceVO_.scalingGroupUuid, groupUuid)
                            .eq(AutoScalingGroupInstanceVO_.healthStatus, AutoScalingGroupInstanceHealthStatus.Unhealthy)
                            .listValues();

                    if (instanceUuids == null || instanceUuids.isEmpty()) {
                        scalingGroupInRemovingUnhealthyInstance.remove(groupUuid);
                        return null;
                    }

                    handleUnhealthyInstances(groupUuid, instanceUuids);
                    return null;
                }

                @Override
                public String getName() {
                    return getSyncSignature();
                }

                @Override
                public String getSyncSignature() {
                    return "auto-scaling-group-remove-unhealthy-instance-task";
                }

                @Override
                public int getSyncLevel() {
                    return AutoScalingGlobalConfig.REMOVE_UNHEALTHY_INSTANCE_THREADS_NUM.value(Integer.class);
                }
            });
        }
    }

    private List<String> getProtectedInstanceUuids(List<String> instanceUuids) {
        List<String> uuids = Q.New(AutoScalingGroupInstanceVO.class)
                .select(AutoScalingGroupInstanceVO_.instanceUuid)
                .in(AutoScalingGroupInstanceVO_.instanceUuid, instanceUuids)
                .eq(AutoScalingGroupInstanceVO_.protectionStrategy, PROTECTION_STRATEGY_PROTECTED)
                .listValues();
        return uuids;
    }

    private void handleUnhealthyInstances(String scalingGroupUuid, List<String> instanceUuids) {
        if(!AutoScalingGroupSystemTags.AUTOMATICALLY_REMOVE_UNHEALTHY_INSTANCE.hasTag(scalingGroupUuid)) {
            scalingGroupInRemovingUnhealthyInstance.remove(scalingGroupUuid);
            return;
        }
        assert instanceUuids.size() >= 1;

        List<String> protectedInstanceUuids = getProtectedInstanceUuids(instanceUuids);
        if (!protectedInstanceUuids.isEmpty()) {
            // vm may have been manually deleted, and the AutoScalingGroupInstanceVO record needs to be deleted
            protectedInstanceUuids = Q.New(VmInstanceVO.class)
                    .select(VmInstanceVO_.uuid)
                    .in(VmInstanceVO_.uuid, protectedInstanceUuids)
                    .listValues();

            if (!protectedInstanceUuids.isEmpty()) {
                logger.trace(String.format("Instances%s are protected, skip deletion", protectedInstanceUuids));
                instanceUuids.removeAll(protectedInstanceUuids);
            }
        }

        if (instanceUuids.isEmpty()) {
            scalingGroupInRemovingUnhealthyInstance.remove(scalingGroupUuid);
            return;
        }

        String activityUuid = Platform.getUuid();
        CreateAutoScalingGroupActivityMsg msg = new CreateAutoScalingGroupActivityMsg();
        msg.setActivityUuid(activityUuid);
        msg.setName("");
        msg.setScalingGroupUuid(scalingGroupUuid);
        msg.setDescription("");
        msg.setActivityAction(AutoScalingGroupActivityAction.RemovalInstance.toString());
        msg.setCause(AutoScalingGroupActivityCause.HealthCheck.toString());
        bus.makeTargetServiceIdByResourceUuid(msg, AutoScalingConstants.SERVICE_ID, scalingGroupUuid);

        AutoScalingGroupRemoveInstancesActionMsg actionMsg = new AutoScalingGroupRemoveInstancesActionMsg();
        actionMsg.setRemovalInstanceSize(instanceUuids.size());
        actionMsg.setIgnoreInstanceSizeLimit(true);
        actionMsg.setAutoScalingGroupUuid(scalingGroupUuid);
        actionMsg.setInstanceUuids(instanceUuids);
        actionMsg.setAutoScalingGroupActivityUuid(activityUuid);
        bus.makeLocalServiceId(actionMsg, AutoScalingConstants.SERVICE_ID);
        msg.setActionMessage(actionMsg);

        bus.send(msg, new CloudBusCallBack(null) {
            @Override
            public void run(MessageReply reply) {
                scalingGroupInRemovingUnhealthyInstance.remove(scalingGroupUuid);
                //log
            }
        });
    }
}
