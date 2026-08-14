package org.zstack.drs;

import com.google.common.collect.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.cascade.CascadeFacade;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.cloudbus.ResourceDestinationMaker;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.thread.PeriodicTask;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.drs.api.APICreateClusterDRSEvent;
import org.zstack.drs.api.APICreateClusterDRSMsg;
import org.zstack.drs.api.APIValidateClusterSupportDRSMsg;
import org.zstack.drs.api.APIValidateClusterSupportDRSReply;
import org.zstack.drs.data.*;
import org.zstack.drs.entity.ClusterDRSInventory;
import org.zstack.drs.entity.ClusterDRSVO;
import org.zstack.drs.entity.ClusterDRSVO_;
import org.zstack.drs.header.DRSErrors;
import org.zstack.drs.message.ExecuteDRSSchedulingMsg;
import org.zstack.drs.message.GetClusterHostsLoadMsg;
import org.zstack.drs.message.GetClusterHostsLoadReply;
import org.zstack.drs.util.DRSUtils;
import org.zstack.header.AbstractService;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.cluster.ClusterVO_;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.managementnode.ManagementNodeReadyExtensionPoint;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmInstanceVO_;
import org.zstack.identity.AccountManager;
import org.zstack.resourceconfig.ResourceConfig;
import org.zstack.resourceconfig.ResourceConfigFacade;
import org.zstack.tag.TagManager;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;
import org.zstack.zwatch.datatype.Datapoint;
import org.zstack.zwatch.datatype.Label;
import org.zstack.zwatch.datatype.MetricQueryObject;
import org.zstack.zwatch.datatype.Namespace;
import org.zstack.zwatch.namespace.HostNamespace;
import org.zstack.zwatch.namespace.VmNamespace;

import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.zstack.core.Platform.err;
import static org.zstack.core.Platform.operr;

/**
 * Created by lining on 2019/12/12.
 */
public class DRSManagerImpl extends AbstractService implements DRSManager, ManagementNodeReadyExtensionPoint {
    private static final CLogger logger = Utils.getLogger(DRSManagerImpl.class);
    private final Object lock = new Object();

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
    private TagManager tagMgr;
    @Autowired
    private ThreadFacade thdf;
    @Autowired
    private ResourceDestinationMaker destinationMaker;
    @Autowired
    private ResourceConfigFacade rcf;

    private Map<String, Future<Void>> drsScheduleTaskMap = new ConcurrentHashMap<>();
    private Set<String> clusterDRSInTracking = ConcurrentHashMap.newKeySet();

    @Override
    public boolean start() {
        startAutoScalingGroupInstanceTask();
        installGlobalConfigUpdater();
        return true;
    }

    private void installGlobalConfigUpdater() {
        DRSGlobalConfig.DRS_SCHEDULING_INTERVAL.installUpdateExtension((oldConfig, newConfig) -> startAutoScalingGroupInstanceTask());
        ResourceConfig resourceConfig = rcf.getResourceConfig(DRSGlobalConfig.DRS_SCHEDULING_INTERVAL.getIdentity());
        resourceConfig.installUpdateExtension((config, resourceUuid, resourceType, oldValue, newValue) ->
                updateAutoScalingGroupInstanceTask(resourceUuid));
    }

    private void cancelOldTaskAndPutNewTask(String clusterUuid) {
        Future<Void> newTask = startAutoScalingGroupInstanceTask(clusterUuid);
        Future<Void> oldTask;
        synchronized (lock) {
            oldTask = drsScheduleTaskMap.put(clusterUuid, newTask);
        }
        if (oldTask != null) {
            oldTask.cancel(true);
        }
    }

    private void cancelAndRemoveOldTask(String clusterUuid) {
        Future<Void> oldTask;
        synchronized (lock) {
            oldTask = drsScheduleTaskMap.remove(clusterUuid);
        }

        if (oldTask != null) {
            oldTask.cancel(true);
        }
    }

    private void updateAutoScalingGroupInstanceTask(String clusterUuid) {
        cancelOldTaskAndPutNewTask(clusterUuid);
    }

    private synchronized void startAutoScalingGroupInstanceTask() {
        drsScheduleTaskMap.keySet().forEach(this::cancelAndRemoveOldTask);

        List<String> clusterUuids = Q.New(ClusterVO.class)
                .select(ClusterVO_.uuid)
                .listValues();

        for (String clusterUuid : clusterUuids) {
            cancelOldTaskAndPutNewTask(clusterUuid);
        }
    }

    private Future<Void> startAutoScalingGroupInstanceTask(String clusterUuid) {
        return thdf.submitPeriodicTask(new PeriodicTask() {
            @Override
            public TimeUnit getTimeUnit() {
                return TimeUnit.SECONDS;
            }

            @Override
            public long getInterval() {
                return rcf.getResourceConfigValue(DRSGlobalConfig.DRS_SCHEDULING_INTERVAL, clusterUuid, Long.class);
            }

            @Override
            public String getName() {
                return "check-the-number-of-instances-in-each-auto-scaling-group-task";
            }

            @Override
            public void run() {
                ClusterVO vo = dbf.findByUuid(clusterUuid, ClusterVO.class);
                if (vo == null) {
                    cancelAndRemoveOldTask(clusterUuid);
                    return;
                }

                List<String> drsUuids = Q.New(ClusterDRSVO.class)
                        .select(ClusterDRSVO_.uuid)
                        .eq(ClusterDRSVO_.clusterUuid, clusterUuid)
                        .eq(ClusterDRSVO_.state, DRSState.Enabled)
                        .listValues();

                for (String drsUuid : drsUuids) {
                    executeDRSScheduling(drsUuid);
                }
            }
        });
    }

    private void executeDRSScheduling(String drsUuid) {
        if (!destinationMaker.isManagedByUs(drsUuid) || clusterDRSInTracking.contains(drsUuid)) {
            return;
        }

        ExecuteDRSSchedulingMsg msg = new ExecuteDRSSchedulingMsg();
        msg.setDrsUuid(drsUuid);
        bus.makeTargetServiceIdByResourceUuid(msg, DRSConstants.SERVICE_ID, drsUuid);
        clusterDRSInTracking.add(drsUuid);
        bus.send(msg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply rly) {
                if (!rly.isSuccess()) {
                    logger.info(rly.getError().toString());
                }
                clusterDRSInTracking.remove(msg.getDRSUuid());
            }
        });
    }

    @Override
    public void managementNodeReady() {
        startAutoScalingGroupInstanceTask();
    }

    @Override
    public boolean stop() {
        if (!drsScheduleTaskMap.isEmpty()) {
            drsScheduleTaskMap.values().forEach(t -> t.cancel(true));
            drsScheduleTaskMap.clear();
        }
        return true;
    }

    @Override
    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof DRSMessage) {
            passThrough((DRSMessage) msg);
        } else if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage) msg);
        } else {
            handleLocalMessage(msg);
        }
    }

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APIValidateClusterSupportDRSMsg) {
            handle((APIValidateClusterSupportDRSMsg) msg);
        } else if (msg instanceof APICreateClusterDRSMsg) {
            handle((APICreateClusterDRSMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void passThrough(DRSMessage msg) {
        ClusterDRSVO vo = dbf.findByUuid(msg.getDRSUuid(), ClusterDRSVO.class);

        if (vo == null) {
            String err = String.format("Cannot find ClusterDRSVO[uuid:%s], it may have been deleted", msg.getDRSUuid());
            bus.replyErrorByMessageType((Message) msg, err);
            return;
        }

        DRSBase drsBase = new DRSBase(vo);
        drsBase.handleMessage((Message) msg);
    }

    private void handleLocalMessage(Message msg) {
        if (msg instanceof GetClusterHostsLoadMsg) {
            handle((GetClusterHostsLoadMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(APIValidateClusterSupportDRSMsg msg) {
        APIValidateClusterSupportDRSReply reply = new APIValidateClusterSupportDRSReply();

        List<String> notSupportedReasons = DRSUtils.validateSupportDRS(msg.getClusterUuid());
        if (notSupportedReasons.isEmpty()) {
            reply.setSupported(true);
            bus.reply(msg, reply);
            return;
        }

        ErrorCode error = err(DRSErrors.DRS_NOT_SUPPORT, "The cluster[%s] does not support DRS.", msg.getClusterUuid());
        LinkedHashMap errOpaque = new LinkedHashMap();
        errOpaque.put("notSupportedReasons", notSupportedReasons);
        error.setOpaque(errOpaque);

        reply.setSupported(false);
        reply.setReason(error);
        bus.reply(msg, reply);
    }

    private void handle(APICreateClusterDRSMsg msg) {
        APICreateClusterDRSEvent event = new APICreateClusterDRSEvent(msg.getId());

        String clusterUuid = msg.getClusterUuid();
        List<String> notSupportedReasons = DRSUtils.validateSupportDRS(msg.getClusterUuid());
        if (!notSupportedReasons.isEmpty()) {
            String reasons = String.join(";", notSupportedReasons);
            event.setError(err(DRSErrors.DRS_NOT_SUPPORT, "Can not create DRS, %s", reasons));
            bus.publish(event);
            return;
        }

        String uuid = msg.getResourceUuid();
        uuid = uuid != null ? uuid : Platform.getUuid();

        ClusterDRSVO vo = new ClusterDRSVO();
        vo.setUuid(uuid);
        vo.setAutomationLevel(DRSAutomationLevel.valueOf(msg.getAutomationLevel()));
        vo.setClusterUuid(clusterUuid);
        vo.setState(msg.isDefaultEnable() ? DRSState.Enabled : DRSState.Disabled);
        vo.setBalancedState(BalancedState.Unknown);
        vo.setThresholdDuration(msg.getThresholdDuration());
        vo.setThresholds(JSONObjectUtil.toJsonString(msg.getThresholds()));
        vo.setCreateDate(new Timestamp(System.currentTimeMillis()));
        vo.setDescription(msg.getDescription());
        vo.setName(msg.getName());
        vo = dbf.persist(vo);

        drsScheduleTaskMap.computeIfAbsent(clusterUuid, key -> startAutoScalingGroupInstanceTask(clusterUuid));
        event.setInventory(ClusterDRSInventory.valueOf(vo));
        bus.publish(event);
    }

    private void handle(GetClusterHostsLoadMsg msg) {
        GetClusterHostsLoadReply reply = new GetClusterHostsLoadReply();

        List<String> hostUuids = msg.getHostUuids();
        List<String> vmUuids = msg.getVmUuids();

        if (hostUuids == null || hostUuids.isEmpty()) {
            reply.setError(operr("hostUuids is empty"));
            bus.reply(msg, reply);
            return;
        }

        List<HostLoad> hostLoads = getHostLoads(hostUuids);
        if (hostLoads.isEmpty()) {
            reply.setError(err(DRSErrors.DRS_ADVICE_NOT_PREPARED, "query hosts utilization data failed"));
            bus.reply(msg, reply);
            return;
        }

        if (vmUuids == null || vmUuids.isEmpty()) {
            reply.setHostLoads(hostLoads);
            bus.reply(msg, reply);
            return;
        }

        List<HostLoadOccupiedByVm> vmLoads = getVmLoads(vmUuids);

        Map<String, String> vmHostMap = new HashMap<>();
        Q.New(VmInstanceVO.class)
                .select(VmInstanceVO_.uuid, VmInstanceVO_.hostUuid)
                .in(VmInstanceVO_.uuid, vmUuids)
                .listTuple()
                .forEach(t -> vmHostMap.put(t.get(0, String.class), t.get(1, String.class)));

        for (HostLoadOccupiedByVm vmLoad : vmLoads) {
            String hostUuid = vmHostMap.get(vmLoad.getVmUuid());
            Optional<HostLoad> optional = hostLoads.stream()
                    .filter(h -> h.getHostUuid().equals(hostUuid))
                    .findAny();
            if (!optional.isPresent()) {
                continue;
            }

            HostLoad hostLoad = optional.get();
            hostLoad.getVmList().add(vmLoad);
        }

        reply.setHostLoads(hostLoads);
        bus.reply(msg, reply);
    }

    private Map<String, Double> getHostUtilizationData(List<String> hostUuids, String metricName) {
        Map<String, Double> result = new HashMap<>();

        Integer duration = DRSGlobalConfig.DRS_COLLECT_HOST_METRIC_DATA_DURATION.value(Integer.class);
        long endTime =  TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis());
        long startTime = endTime - duration;
        String hostUuidListString = String.join("|" , hostUuids);
        Label label = new Label(String.format("%s%s%s", HostNamespace.LabelNames.HostUuid.toString(), Label.Operator.Regex.toString(), hostUuidListString));
        String namespace = String.format("ZStack/%s", HostNamespace.NAME);

        MetricQueryObject qo = MetricQueryObject.New()
                .namespace(namespace)
                .startTime(startTime)
                .endTime(endTime)
                .period(duration)
                .labels(Arrays.asList(label))
                .metricName(metricName)
                .build();
        Namespace ns = Namespace.getMetricNameSpace(namespace, qo.getMetricName());
        List<Datapoint> data = ns.query(qo);
        if (data == null || data.isEmpty()) {
            return result;
        }

        Map<String, List<Double>> hostUsageMap = new HashMap<>();
        for (Datapoint datapoint : data) {
            String hostUuid = datapoint.getLabels().get(HostNamespace.LabelNames.HostUuid.toString());
            List<Double> values = hostUsageMap.computeIfAbsent(hostUuid, k -> new ArrayList<>());

            double value = datapoint.getValue();
            values.add(value);
        }

        hostUsageMap.forEach((hostUuid, hostCpuUsedData) -> {
            double averageHostCpuUsed = hostCpuUsedData.stream().mapToDouble(i -> i).sum() / hostCpuUsedData.size();
            result.put(hostUuid, averageHostCpuUsed);
        });

        return result;
    }

    private List<HostLoad> getHostLoads(List<String> hostUuids) {
        Map<String, Double> cpuUsedMap = new HashMap<>();
        Map<String, Double> memoryUsedMap = new HashMap<>();

        List<List<String>> hostUuidParts = Lists.partition(hostUuids, 10);
        hostUuidParts.forEach(uuids -> {
            Map<String, Double> subCpuUsedMap = getHostUtilizationData(uuids, HostNamespace.CPUAllUsedUtilization.getName());
            Map<String, Double> subMemoryUsedMap = getHostUtilizationData(uuids, HostNamespace.MemoryUsedBytes.getName());
            cpuUsedMap.putAll(subCpuUsedMap);
            memoryUsedMap.putAll(subMemoryUsedMap);
        });

        Map<String, HostLoad> hostLoadMap = new HashMap<>();

        for(String hostUuid : cpuUsedMap.keySet()){
            HostLoad hostLoad = hostLoadMap.get(hostUuid);
            if (hostLoad == null) {
                hostLoad = new HostLoad();
                hostLoad.setHostUuid(hostUuid);
                hostLoadMap.put(hostUuid, hostLoad);
            }

            hostLoad.setUsedCPUPercent(cpuUsedMap.get(hostUuid).floatValue());
        }

        for(String hostUuid : memoryUsedMap.keySet()){
            HostLoad hostLoad = hostLoadMap.get(hostUuid);
            if (hostLoad == null) {
                hostLoad = new HostLoad();
                hostLoad.setHostUuid(hostUuid);
                hostLoadMap.put(hostUuid, hostLoad);
            }

            hostLoad.setUsedPhysicalMemoryBit(memoryUsedMap.get(hostUuid).longValue());
        }

        return new ArrayList<>(hostLoadMap.values());
    }

    private Map<String, Double> getVmUtilizationData(List<String> vmUuids, String metricName) {
        Map<String, Double> result = new HashMap<>();

        Integer duration = DRSGlobalConfig.DRS_COLLECT_HOST_METRIC_DATA_DURATION.value(Integer.class);
        long endTime =  TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis());
        long startTime = endTime - duration;
        String vmUuidListString = String.join("|" , vmUuids);
        Label label = new Label(String.format("%s%s%s", VmNamespace.LabelNames.VMUuid.toString(), Label.Operator.Regex.toString(), vmUuidListString));
        String namespace = String.format("ZStack/%s", VmNamespace.NAME);

        logger.info(String.format("query vm[%s] %s data", vmUuidListString, metricName));
        MetricQueryObject qo = MetricQueryObject.New()
                .namespace(namespace)
                .startTime(startTime)
                .endTime(endTime)
                .period(duration)
                .labels(Arrays.asList(label))
                .metricName(metricName)
                .build();
        Namespace ns = Namespace.getMetricNameSpace(namespace, qo.getMetricName());
        List<Datapoint> data = ns.query(qo);
        if (data == null || data.isEmpty()) {
            return result;
        }

        Map<String, List<Double>> vmUsageMap = new HashMap<>();
        for (Datapoint datapoint : data) {
            String vmUuid = datapoint.getLabels().get(VmNamespace.LabelNames.VMUuid.toString());
            List<Double> values = vmUsageMap.computeIfAbsent(vmUuid, k -> new ArrayList<>());

            double value = datapoint.getValue();
            values.add(value);
        }

        vmUsageMap.forEach((hostUuid, hostCpuUsedData) -> {
            double averageVmCpuUsed = hostCpuUsedData.stream().mapToDouble(i -> i).sum() / hostCpuUsedData.size();
            result.put(hostUuid, averageVmCpuUsed);
        });

        return result;
    }

    private List<HostLoadOccupiedByVm> getVmLoads(List<String> vmUuids) {
        Map<String, Double> cpuUsedMap = new HashMap<>();
        Map<String, Double> memoryUsedMap = new HashMap<>();

        List<List<String>> vmUuidParts = Lists.partition(vmUuids, 10);
        vmUuidParts.forEach(uuids -> {
            Map<String, Double> subCpuUsedMap = getVmUtilizationData(uuids, VmNamespace.CPUOccupiedByVM.getName());
            Map<String, Double> subMemoryUsedMap = getVmUtilizationData(uuids, VmNamespace.MemoryOccupiedByVM.getName());
            cpuUsedMap.putAll(subCpuUsedMap);
            memoryUsedMap.putAll(subMemoryUsedMap);
        });

        Map<String, HostLoadOccupiedByVm> hostLoadMap = new HashMap<>();

        for(String vmUuid : cpuUsedMap.keySet()){
            HostLoadOccupiedByVm hostLoad = hostLoadMap.get(vmUuid);
            if (hostLoad == null) {
                hostLoad = new HostLoadOccupiedByVm();
                hostLoad.setVmUuid(vmUuid);
                hostLoadMap.put(vmUuid, hostLoad);
            }

            hostLoad.setUsedHostCPUPercent(cpuUsedMap.get(vmUuid).floatValue() / 100f);
        }

        for(String vmUuid : memoryUsedMap.keySet()){
            HostLoadOccupiedByVm hostLoad = hostLoadMap.get(vmUuid);
            if (hostLoad == null) {
                hostLoad = new HostLoadOccupiedByVm();
                hostLoad.setVmUuid(vmUuid);
                hostLoadMap.put(vmUuid, hostLoad);
            }

            hostLoad.setUsedHostPhysicalMemoryBit(memoryUsedMap.get(vmUuid).longValue());
        }

        return new ArrayList<>(hostLoadMap.values());
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(DRSConstants.SERVICE_ID);
    }
}
