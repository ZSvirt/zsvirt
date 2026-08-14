package org.zstack.memory;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.compute.cluster.MevocoClusterGlobalConfig;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.thread.PeriodicTask;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.header.AbstractService;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowChain;
import org.zstack.header.core.workflow.FlowDoneHandler;
import org.zstack.header.core.workflow.FlowErrorHandler;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.host.HostConstant;
import org.zstack.header.host.HostState;
import org.zstack.header.host.HostStatus;
import org.zstack.header.message.Message;
import org.zstack.header.vm.ApplyMemoryBalloonDecisionMsg;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmInstanceVO_;
import org.zstack.kvm.KVMGlobalConfig;
import org.zstack.kvm.KVMHostVO;
import org.zstack.resourceconfig.ResourceConfigFacade;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.zwatch.datatype.Datapoint;
import org.zstack.zwatch.datatype.Label;
import org.zstack.zwatch.datatype.MetricQueryObject;
import org.zstack.zwatch.datatype.Namespace;
import org.zstack.zwatch.namespace.HostNamespace;
import org.zstack.zwatch.namespace.VmAbstractNamespace;
import org.zstack.zwatch.namespace.VmNamespace;

import javax.persistence.Tuple;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.operr;

public class MemoryBalloonManagerImpl extends AbstractService implements MemoryBalloonManager {
    private static final CLogger logger = Utils.getLogger(MemoryBalloonManagerImpl.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private ThreadFacade thdf;
    @Autowired
    private ResourceConfigFacade rcf;

    @SuppressWarnings("rawtypes")
    Future memoryBalloonMakeDecisionTask;

    private final String FORCE_INCREASE = "FORCE_INCREASE";

    private void startMemoryBalloonMakeDecisionTask() {
        if (memoryBalloonMakeDecisionTask != null) {
            memoryBalloonMakeDecisionTask.cancel(true);
        }

        memoryBalloonMakeDecisionTask = thdf.submitPeriodicTask(new PeriodicTask() {
            @Override
            public TimeUnit getTimeUnit() {
                return TimeUnit.SECONDS;
            }

            @Override
            public long getInterval() {
                return MemoryBalloonGlobalConfig.MEMORY_AUTO_BALLOON_SCAN_INTERVAL.value(Long.class);
            }

            @Override
            public String getName() {
                return "vm-memory-auto-balloon-scan";
            }

            @Override
            public void run() {
                makeMemoryBalloonDecision();
            }
        });
    }

    private void makeMemoryBalloonDecision() {
        long total = Q.New(KVMHostVO.class).count();
        SQL.New("select uuid, clusterUuid from KVMHostVO where state = :state and status = :status", Tuple.class)
                .param("state", HostState.Enabled)
                .param("status", HostStatus.Connected)
                .limit(100)
                .paginate(total, (List<Tuple> tuples) -> tuples.forEach(tuple -> memoryBalloonByHost(tuple.get(0, String.class), tuple.get(1, String.class))));
    }

    private void memoryBalloonByHost(String hostUuid, String clusterUuid) {
        if (clusterUuid == null) {
            logger.warn(String.format("host %s has no clusterUuid, skip memory balloon", hostUuid));
            return;
        }

        Boolean useHugePage = rcf.getResourceConfigValue(MevocoClusterGlobalConfig.HUGEPAGE_ENABLE, clusterUuid, Boolean.class);
        if (useHugePage == null || useHugePage) {
            logger.debug(String.format("host %s in cluster %s use huge page, skip memory balloon", hostUuid, clusterUuid));
            return;
        }

        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName("memory-balloon-by-host");
        chain.then(new NoRollbackFlow() {
            String __name__ = "check-host-memory-usage";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                Label label = new Label(HostNamespace.LabelNames.HostUuid.toString(), Label.Operator.Equal, hostUuid);
                String namespace = String.format("ZStack/%s", HostNamespace.NAME);
                Integer duration = MemoryBalloonGlobalConfig.MEMORY_AUTO_BALLOON_SCAN_INTERVAL.value(Integer.class);
                long endTime = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis());
                long startTime = endTime - duration;
                MetricQueryObject qo = MetricQueryObject.New()
                        .namespace(namespace)
                        .startTime(startTime)
                        .endTime(endTime)
                        .period(duration)
                        .labels(Arrays.asList(label))
                        .metricName(HostNamespace.MemoryUsedInPercent.getName())
                        .build();
                logger.trace(String.format("query host memory usage by %s", qo));
                Namespace ns = Namespace.getMetricNameSpace(namespace, qo.getMetricName());
                logger.trace("namespace name is " + ns.getName());
                List<Datapoint> dataPoints = ns.query(qo);

                if (dataPoints.isEmpty()) {
                    trigger.fail(operr("no data of %s found on host[%s]", HostNamespace.MemoryUsedInPercent.toString(), hostUuid));
                    return;
                }

                if (dataPoints.get(0).getValue() < 80) {
                    data.put("FORCE_INCREASE", true);
                }

                trigger.next();
            }
        }).then(new NoRollbackFlow() {
            @Override
            public void run(FlowTrigger trigger, Map data) {
                makeMemoryBalloonDecisionByHost(hostUuid, (Boolean) data.getOrDefault(FORCE_INCREASE, false));
                trigger.next();
            }
        }).error(new FlowErrorHandler(null) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                logger.debug(String.format("memory balloon by host[%s] failed, %s", hostUuid, errCode));
            }
        }).done(new FlowDoneHandler(null) {
            @Override
            public void handle(Map data) {
                logger.debug(String.format("memory balloon by host[%s] done", hostUuid));
            }
        }).start();
    }

    private void forceIncreaseMemory(String hostUuid, List<String> vmUuidList) {
        Integer thresholdPercent = MemoryBalloonGlobalConfig.MEMORY_AUTO_BALLOON_THRESHOLD_PERCENT.value(Integer.class);
        applyMemoryBalloonDecision(hostUuid, vmUuidList, AutoBalloonDirection.Increase, thresholdPercent);
    }

    private void makeMemoryBalloonDecisionByHost(String hostUuid, boolean forceIncrease) {
        List<String> autoBalloonVmUuidList = getAutoBalloonVmUuidList(hostUuid);
        logger.debug(String.format("auto balloon vm %s", String.join(",", autoBalloonVmUuidList)));
        if (autoBalloonVmUuidList.isEmpty()) {
            return;
        }

        if (forceIncrease) {
            forceIncreaseMemory(hostUuid, autoBalloonVmUuidList);
            return;
        }

        List<String> vmInstanceUuidList = getHostVmUuidList(hostUuid);

        String st = MemoryBalloonGlobalConfig.MEMORY_AUTO_BALLOON_STRATEGY.value();
        AutoBalloonStrategy strategy = AutoBalloonStrategy.valueOf(st);
        switch (strategy) {
            case UrgentFirst:
                makeMemoryBalloonDecisionByUrgentFirst(hostUuid, vmInstanceUuidList, autoBalloonVmUuidList);
                break;
            case IdleFirst:
                makeMemoryBalloonDecisionByIdleFirst(hostUuid, vmInstanceUuidList, autoBalloonVmUuidList);
                break;
            case All:
                makeMemoryBalloonDecisionByAll(hostUuid, vmInstanceUuidList, autoBalloonVmUuidList);
                break;
            default:
                throw new RuntimeException(String.format("unknown auto balloon strategy[%s]", st));
        }
    }

    private void makeMemoryBalloonDecisionByAll(String hostUuid, List<String> vmInstanceUuidList, List<String> autoBalloonVmUuidList) {
        makeMemoryBalloonDecisionByIdleFirst(hostUuid, vmInstanceUuidList, autoBalloonVmUuidList);
        makeMemoryBalloonDecisionByUrgentFirst(hostUuid, vmInstanceUuidList, autoBalloonVmUuidList);
    }

    private void makeMemoryBalloonDecisionByIdleFirst(String hostUuid, List<String> vmInstanceUuidList, List<String> autoBalloonVmUuidList) {
        TreeMap<Integer, List<String>> sortedMap = rateVmMemoryUrgentByMetric(vmInstanceUuidList, VmAbstractNamespace.MemoryFreeInPercent.getName());
        if (sortedMap == null || sortedMap.isEmpty()) {
            return;
        }

        // calculate total with key * size of list
        int averageFreeMemory = sortedMap.entrySet()
                .stream()
                .mapToInt(e -> e.getKey() * e.getValue().size())
                .sum() /
                sortedMap
                        .values()
                        .stream()
                        .mapToInt(List::size)
                        .sum();

        logger.debug(String.format("average free memory of host[%s] is %s", hostUuid, averageFreeMemory));
        Integer thresholdPercent = MemoryBalloonGlobalConfig.MEMORY_AUTO_BALLOON_THRESHOLD_PERCENT.value(Integer.class);
        Integer lowerBound = averageFreeMemory * (100 - thresholdPercent) / 100;
        Integer upperBound = averageFreeMemory * (100 + thresholdPercent) / 100;
        logger.debug(String.format("lower bound[%s], upper bound[%s]", lowerBound, upperBound));
        logger.debug(String.format("autoBalloonVmUuidList : %s", String.join(",", autoBalloonVmUuidList)));

        // find all vm which free memory is less than average free memory
        List<String> vmUuidListToBalloon = sortedMap.entrySet().stream()
                .filter(e -> e.getKey() < lowerBound)
                .flatMap(e -> e.getValue().stream())
                .filter(autoBalloonVmUuidList::contains)
                .collect(Collectors.toList());

        if (!vmUuidListToBalloon.isEmpty()) {
            logger.info(String.format("make memory balloon decision for host[%s], vm[%s]", hostUuid, vmUuidListToBalloon));
            applyMemoryBalloonDecision(hostUuid, vmUuidListToBalloon, AutoBalloonDirection.Increase, thresholdPercent);
        }

        // find all vm which free memory is greater than average free memory
        List<String> vmUuidListToUnBalloon = sortedMap.entrySet().stream()
                .filter(e -> e.getKey() > upperBound)
                .flatMap(e -> e.getValue().stream())
                .filter(autoBalloonVmUuidList::contains)
                .collect(Collectors.toList());
        if (vmUuidListToUnBalloon.isEmpty()) {
            return;
        }

        applyMemoryBalloonDecision(hostUuid, vmUuidListToUnBalloon, AutoBalloonDirection.Decrease, thresholdPercent);
    }

    private void applyMemoryBalloonDecision(String hostUuid, List<String> vmUuidList, AutoBalloonDirection direction, Integer adjustPercent) {
        List<Tuple> ts = Q.New(VmInstanceVO.class)
                .in(VmInstanceVO_.uuid, vmUuidList)
                .select(VmInstanceVO_.uuid, VmInstanceVO_.reservedMemorySize)
                .listTuple();

        Map<String, Long> vmPreReservedMemoryMap = ts.stream()
                .collect(Collectors.toMap(t -> t.get(0, String.class), t -> t.get(1, Long.class)));

        ApplyMemoryBalloonDecisionMsg msg = new ApplyMemoryBalloonDecisionMsg();
        msg.setVmInstanceUuids(vmUuidList);
        msg.setDirection(direction.toString());
        msg.setAdjustPercent(adjustPercent);
        msg.setHostUuid(hostUuid);
        msg.setVmReservedMemory(vmPreReservedMemoryMap);
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hostUuid);
        bus.send(msg);
    }

    private void makeMemoryBalloonDecisionByUrgentFirst(String hostUuid, List<String> vmInstanceUuidList, List<String> autoBalloonVmUuidList) {
        TreeMap<Integer, List<String>> sortedMap = rateVmMemoryUrgentByMetric(vmInstanceUuidList, VmAbstractNamespace.MemoryMinorPageFaults.getName());
        if (sortedMap == null || sortedMap.isEmpty()) {
            return;
        }

        int averagePageFault = sortedMap.entrySet()
                .stream()
                .mapToInt(e -> e.getKey() * e.getValue().size())
                .sum() /
                sortedMap
                        .values()
                        .stream()
                        .mapToInt(List::size)
                        .sum();
        int total = sortedMap.entrySet()
                .stream()
                .mapToInt(e -> e.getKey() * e.getValue().size())
                .sum();
        logger.debug(String.format("total page fault of host[%s] is %s", hostUuid, total));
        logger.debug(String.format("size of map is %s", sortedMap.size()));
        logger.debug(String.format("average page fault of host[%s] is %s", hostUuid, averagePageFault));
        Integer thresholdPercent = MemoryBalloonGlobalConfig.MEMORY_AUTO_BALLOON_THRESHOLD_PERCENT.value(Integer.class);
        Integer lowerBound = averagePageFault * (100 - thresholdPercent) / 100;
        Integer upperBound = averagePageFault * (100 + thresholdPercent) / 100;
        logger.debug(String.format("lower bound[%s], upper bound[%s]", lowerBound, upperBound));
        logger.debug(String.format("autoBalloonVmUuidList : %s", String.join(",", autoBalloonVmUuidList)));

        // higher page fault means more urgent
        List<String> vmUuidListToBalloon = sortedMap.entrySet().stream()
                .filter(e -> e.getKey() > upperBound)
                .flatMap(e -> e.getValue().stream())
                .filter(autoBalloonVmUuidList::contains)
                .collect(Collectors.toList());

        logger.info(String.format("make memory balloon decision for host[%s], vm[%s]", hostUuid, vmUuidListToBalloon));
        if (!vmUuidListToBalloon.isEmpty()) {
            logger.info(String.format("make memory balloon decision for host[%s], vm[%s]", hostUuid, vmUuidListToBalloon));
            applyMemoryBalloonDecision(hostUuid, vmUuidListToBalloon, AutoBalloonDirection.Increase, thresholdPercent);
        }

        // find all vm which free memory is greater than average free memory
        List<String> vmUuidListToUnBalloon = sortedMap.entrySet().stream()
                .filter(e -> e.getKey() < lowerBound)
                .flatMap(e -> e.getValue().stream())
                .filter(autoBalloonVmUuidList::contains)
                .collect(Collectors.toList());
        if (vmUuidListToUnBalloon.isEmpty()) {
            return;
        }

        logger.info(String.format("make memory balloon decision for host[%s], vm[%s]", hostUuid, vmUuidListToUnBalloon));
        applyMemoryBalloonDecision(hostUuid, vmUuidListToUnBalloon, AutoBalloonDirection.Decrease, thresholdPercent);
    }

    private TreeMap<Integer, List<String>> rateVmMemoryUrgentByMetric(List<String> vmInstanceUuidList, String metricName) {
        Integer duration = MemoryBalloonGlobalConfig.MEMORY_AUTO_BALLOON_SCAN_INTERVAL.value(Integer.class);
        long endTime =  TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis());
        long startTime = endTime - duration;
        String vmUuidListString = String.join("|" , vmInstanceUuidList);
        Label label = new Label(VmNamespace.LabelNames.VMUuid.toString(), Label.Operator.Regex, vmUuidListString);
        String namespace = String.format("ZStack/%s", VmNamespace.NAME);

        logger.info(String.format("query vm[%s] %s data", vmUuidListString, metricName));
        MetricQueryObject qo = MetricQueryObject.New()
                .namespace(namespace)
                .startTime(startTime)
                .endTime(endTime)
                .period(duration)
                .labels(Collections.singletonList(label))
                .metricName(metricName)
                .build();
        Namespace ns = Namespace.getMetricNameSpace(namespace, qo.getMetricName());

        List<Datapoint> data = null;
        try {
            data = ns.query(qo);
        } catch (Exception e) {
            logger.trace(String.format("query vm[%s] %s data failed", vmUuidListString, metricName), e);
        }

        if (data == null || data.isEmpty()) {
            return null;
        }

        TreeMap<Integer, List<String>> result = new TreeMap<>();
        for (Datapoint datapoint : data) {
            String vmUuid = datapoint.getLabels().get(VmNamespace.LabelNames.VMUuid.toString());
            result.computeIfAbsent(datapoint.getValue().intValue(), x -> new ArrayList<>());
            result.get(datapoint.getValue().intValue()).add(vmUuid);
        }

        return result;
    }

    private List<String> getHostVmUuidList(String hostUuid) {
        // list vm running on host
        return Q.New(VmInstanceVO.class)
                .select(VmInstanceVO_.uuid)
                .eq(VmInstanceVO_.hostUuid, hostUuid)
                .eq(VmInstanceVO_.state, VmInstanceState.Running)
                .listValues();
    }

    private List<String> getAutoBalloonVmUuidList(String hostUuid) {
        // list vm running on host
        List<String> vmInstanceUuidList = Q.New(VmInstanceVO.class)
                .select(VmInstanceVO_.uuid)
                .eq(VmInstanceVO_.hostUuid, hostUuid)
                .eq(VmInstanceVO_.state, VmInstanceState.Running)
                .listValues();

        // find memory auto balloon vm
        return vmInstanceUuidList.stream()
                .filter(vmInstanceUuid -> rcf
                        .getResourceConfigValue(
                                KVMGlobalConfig.MEMORY_AUTO_BALLOON,
                                vmInstanceUuid,
                                Boolean.class
                        )
                ).collect(Collectors.toList());
    }

    private void installGlobalConfigHooks() {
        MemoryBalloonGlobalConfig.MEMORY_AUTO_BALLOON_SCAN_INTERVAL.installUpdateExtension((oldConfig, newConfig) -> startMemoryBalloonMakeDecisionTask());
    }

    @Override
    public boolean start() {
        startMemoryBalloonMakeDecisionTask();
        installGlobalConfigHooks();
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public void handleMessage(Message msg) {
        bus.dealWithUnknownMessage(msg);
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(MemoryBalloonManager.SERVICE_ID);
    }
}
