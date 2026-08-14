package org.zstack.pluginpremium.compute.allocator;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.allocator.AbstractHostSortorFlow;
import org.zstack.header.host.HostInventory;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.zwatch.datatype.Datapoint;
import org.zstack.zwatch.datatype.Label;
import org.zstack.zwatch.datatype.MetricQueryObject;
import org.zstack.zwatch.datatype.Namespace;
import org.zstack.zwatch.namespace.CustomNamespace;
import org.zstack.zwatch.namespace.HostNamespace;
import org.zstack.zwatch.namespace.VmNamespace;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.zstack.pluginpremium.compute.allocator.HostAllocatorConstant.HOST_ALLOCATOR_STRATEGY_MODE_HARD;
import static org.zstack.pluginpremium.compute.allocator.HostAllocatorSystemTags.MINIMUM_CPU_USAGE_HOST_ALLOCATOR_STRATEGY_MODE;

/**
 * Created by lining on 2018/3/2.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class MinimumCPUUsageSortFlow extends AbstractHostSortorFlow {
    private static final CLogger logger = Utils.getLogger(MinimumCPUUsageSortFlow.class);
    @Autowired
    private DatabaseFacade dbf;

    private boolean ignoreError() {
        String instanceOfferingUuid = spec.getVmInstance().getInstanceOfferingUuid();

        // If not set, the default is soft mode
        if (!HostAllocatorSystemTags.MINIMUM_CPU_USAGE_HOST_ALLOCATOR_STRATEGY_MODE.hasTag(instanceOfferingUuid)) {
            return true;
        }

        String mode = HostAllocatorSystemTags.MINIMUM_CPU_USAGE_HOST_ALLOCATOR_STRATEGY_MODE.getTokenByResourceUuid(instanceOfferingUuid, HostAllocatorSystemTags.MINIMUM_CPU_USAGE_HOST_ALLOCATOR_STRATEGY_MODE_TOKEN);
        if (!mode.isEmpty() && HOST_ALLOCATOR_STRATEGY_MODE_HARD.equals(mode)) {
            return false;
        }

        return true;
    }

    private List<String> sortHostByMinimumCPUUsage(List<String> huuids) {
        if (huuids.isEmpty()) {
            return new ArrayList<>();
        }

        List<Datapoint> data = new ArrayList<>();
        try {
            data = this.getHostCPUAllUsedUtilizationData(huuids);
        } catch (Throwable t) {
            logger.error(String.format("query host%s %s data fail", huuids, HostNamespace.CPUAllUsedUtilization.getName()), t);
            if (!ignoreError()) {
                throw t;
            }
        }

        if (data == null || data.isEmpty()) {
            return huuids;
        }

        Map<String, List<Double>> hostCpuUsageMap = new HashMap<>();
        for (Datapoint datapoint : data) {
            String hostUuid = datapoint.getLabels().get(HostNamespace.LabelNames.HostUuid.toString());
            List<Double> values = hostCpuUsageMap.get(hostUuid);
            if (values == null) {
                values = new ArrayList<>();
                hostCpuUsageMap.put(hostUuid, values);
            }

            double value = datapoint.getValue();
            values.add(value);
        }

        Map<String, Double> hostAverageCpuUsageMap = new HashMap<>();
        hostCpuUsageMap.forEach((hostUuid, hostCpuUsedData) -> {
            double averageHostCpuUsed = hostCpuUsedData.stream().mapToDouble(i -> i).sum() / hostCpuUsedData.size();
            hostAverageCpuUsageMap.put(hostUuid, averageHostCpuUsed);
        });

        Map<String, Double> finalMap = new LinkedHashMap<>();
        hostAverageCpuUsageMap.entrySet().stream()
                .sorted(Map.Entry.<String, Double> comparingByValue())
                .forEachOrdered(e -> finalMap.put(e.getKey(), e.getValue()));

        return new ArrayList<>(finalMap.keySet());
    }

    private List<Datapoint> getHostCPUAllUsedUtilizationData(List<String> huuids) {
        long endTime =  TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis());
        long startTime = endTime - PremiumHostAllocatorGlobalConfig.MINIMUM_CPU_USAGE_HOST_ALLOCATOR_COLLECT_HOST_DATA_DURATION.value(Integer.class);
        String hostUuidListString = String.join("|" , huuids);
        Label label = new Label(String.format("%s%s%s", HostNamespace.LabelNames.HostUuid.toString(), Label.Operator.Regex.toString(), hostUuidListString));
        String namespace = String.format("ZStack/%s", HostNamespace.NAME);

        logger.info(String.format("query host[%s] %s data", hostUuidListString, VmNamespace.CPUAllUsedUtilization.getName()));
        MetricQueryObject qo = MetricQueryObject.New()
                .namespace(namespace)
                .startTime(startTime)
                .endTime(endTime)
                .period(HostAllocatorConstant.periodSecs)
                .labels(Arrays.asList(label))
                .metricName(HostNamespace.CPUAllUsedUtilization.getName())
                .build();
        Namespace ns = Namespace.getMetricNameSpace(namespace, qo.getMetricName());

        List<Datapoint> data = ns.query(qo);
        return data;
    }

    @Override
    public void sort() {
        Map<String, HostInventory> hosts = candidates.stream().collect(Collectors.toMap(HostInventory::getUuid, (candidate) -> candidate));
        List<String> sortedHostUuids = sortHostByMinimumCPUUsage(candidates.stream().map(HostInventory::getUuid).collect(Collectors.toList()));

        candidates.clear();
        sortedHostUuids.forEach(huuid -> candidates.add(hosts.get(huuid)));
        logger.debug(String.format("Sorted by MinimumCPUUsage the hosts %s", candidates.stream().map(c -> c.getUuid()).collect(Collectors.toList())));
    }

    @Override
    public boolean skipNext() {
        return true;
    }
}
