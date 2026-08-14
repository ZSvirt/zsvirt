package org.zstack.zwatch.prometheus;

import io.prometheus.client.Collector;
import io.prometheus.client.Collector.MetricFamilySamples;
import io.prometheus.client.GaugeMetricFamily;
import org.apache.commons.lang.StringUtils;
import org.zstack.compute.host.HostXfsFragReader;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQLBatchWithReturn;
import org.zstack.header.allocator.HostCapacityVO;
import org.zstack.header.core.StaticInit;
import org.zstack.header.host.*;
import org.zstack.zwatch.datatype.metric.InfoMetric;
import org.zstack.zwatch.datatype.metric.Metric;
import org.zstack.zwatch.datatype.Namespace;
import org.zstack.zwatch.namespace.*;

import javax.persistence.Tuple;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;

public class HostPrometheusNamespace extends AbstractPrometheusNamespace {
    public static class HostCollector extends BasicMetricCollector {
        @Override
        protected String getNamespaceName() {
            return HostNamespace.NAME;
        }

        protected List<Collector.MetricFamilySamples> createHostInfoMetric(String hypervisorType) {
            List<Collector.MetricFamilySamples> samples = new ArrayList<>();
            GaugeMetricFamily hostInfoMetric = createMetric(HostAbstractNamespace.HostInfo);

            Q q = Q.New(HostVO.class).select(HostVO_.uuid, HostVO_.name, HostVO_.managementIp);
            if (hypervisorType != null) {
                q.eq(HostVO_.hypervisorType, hypervisorType);
            }

            List<Tuple> rows = q.listTuple();
            rows.forEach(row -> {
                String uuid = row.get(0, String.class);
                String name = row.get(1, String.class);
                String managementIp = row.get(2, String.class);
                hostInfoMetric.addMetric(asList(
                        StringUtils.defaultString(uuid),
                        StringUtils.defaultString(name),
                        StringUtils.defaultString(managementIp)
                ), 1D);
            });

            samples.add(hostInfoMetric);
            return samples;
        }

        @Override
        public List<Collector.MetricFamilySamples> collect() {
            return new SQLBatchWithReturn<List<Collector.MetricFamilySamples>>() {
                private long totalHosts;
                Long totalCpu = 0L;
                Long availCpu = 0L;
                Long lockedCpu = 0L;
                Long usedCpu = 0L;
                Long totalMem = 0L;
                Long availMem = 0L;
                Long lockedMem = 0L;
                Long usedMem = 0L;

                @Override
                protected List<Collector.MetricFamilySamples> scripts() {
                    List<Collector.MetricFamilySamples> samples = new ArrayList<>();

                    samples.addAll(createHostInfoMetric(null));
                    samples.addAll(createHostMetrics());
                    samples.addAll(createCPUMemoryMetrics());
                    return samples;
                }

                private List<Collector.MetricFamilySamples> createCPUMemoryMetrics() {
                    List<Collector.MetricFamilySamples> samples = new ArrayList<>();

                    GaugeMetricFamily CPUAvailableCapacityPerHostCount = createMetric(HostNamespace.CPUAvailableCapacityPerHostCount);
                    samples.add(CPUAvailableCapacityPerHostCount);

                    GaugeMetricFamily CPUAvailableCapacityPerHostInPercent = createMetric(HostNamespace.CPUAvailableCapacityPerHostInPercent);
                    samples.add(CPUAvailableCapacityPerHostInPercent);

                    GaugeMetricFamily CPUUsedCapacityPerHostCount = createMetric(HostNamespace.CPUUsedCapacityPerHostCount);
                    samples.add(CPUUsedCapacityPerHostCount);

                    GaugeMetricFamily CPUUsedCapacityPerHostInPercent = createMetric(HostNamespace.CPUUsedCapacityPerHostInPercent);
                    samples.add(CPUUsedCapacityPerHostInPercent);

                    GaugeMetricFamily MemoryAvailableCapacityPerHostInBytes = createMetric(HostNamespace.MemoryAvailableCapacityPerHostInBytes);
                    samples.add(MemoryAvailableCapacityPerHostInBytes);

                    GaugeMetricFamily MemoryAvailableCapacityPerHostInPercent = createMetric(HostNamespace.MemoryAvailableCapacityPerHostInPercent);
                    samples.add(MemoryAvailableCapacityPerHostInPercent);

                    GaugeMetricFamily MemoryUsedCapacityPerHostInBytes = createMetric(HostNamespace.MemoryUsedCapacityPerHostInBytes);
                    samples.add(MemoryUsedCapacityPerHostInBytes);

                    GaugeMetricFamily MemoryUsedCapacityPerHostInPercent = createMetric(HostNamespace.MemoryUsedCapacityPerHostInPercent);
                    samples.add(MemoryUsedCapacityPerHostInPercent);

                    sql("select h.status, h.state, cap, h.hypervisorType, h.name, h.managementIp from HostVO h, HostCapacityVO cap where h.uuid = cap.uuid", Tuple.class)
                            .limit(500).paginate(totalHosts, (List<Tuple> ts) -> ts.forEach(it ->
                    {

                        HostStatus status = it.get(0, HostStatus.class);
                        HostState state = it.get(1, HostState.class);
                        HostCapacityVO cap = it.get(2, HostCapacityVO.class);
                        String hvType = StringUtils.defaultString(it.get(3, String.class));
                        String hostName = StringUtils.defaultString(it.get(4, String.class));
                        String managementIp = StringUtils.defaultString(it.get(5, String.class));

                        boolean avail = (status == HostStatus.Connected && state == HostState.Enabled);

                        totalCpu += cap.getTotalCpu();
                        totalMem += cap.getTotalMemory();
                        if (avail) {
                            availCpu += cap.getAvailableCpu();
                            availMem += cap.getAvailableMemory();
                        } else {
                            lockedCpu += cap.getAvailableCpu();
                            lockedMem += cap.getAvailableMemory();
                        }
                        usedCpu += cap.getUsedCpu();
                        usedMem += cap.getUsedMemory();

                        List<String> labels = asList(cap.getUuid(), hvType, hostName, managementIp);

                        CPUAvailableCapacityPerHostCount.addMetric(labels, cap.getAvailableCpu());
                        CPUAvailableCapacityPerHostInPercent.addMetric(labels,  cap.getTotalCpu () == 0 ? 0 : ((double)cap.getAvailableCpu() / cap.getTotalCpu()) * 100);
                        CPUUsedCapacityPerHostCount.addMetric(labels, cap.getUsedCpu());
                        CPUUsedCapacityPerHostInPercent.addMetric(labels, cap.getTotalCpu () == 0 ? 0 : ((double)cap.getUsedCpu() / cap.getTotalCpu()) * 100);
                        MemoryAvailableCapacityPerHostInBytes.addMetric(labels, cap.getAvailableMemory());
                        MemoryAvailableCapacityPerHostInPercent.addMetric(labels, cap.getTotalMemory() == 0 ? 0 : ((double) cap.getAvailableMemory() / cap.getTotalMemory()) * 100);
                        MemoryUsedCapacityPerHostInBytes.addMetric(labels, cap.getUsedMemory());
                        MemoryUsedCapacityPerHostInPercent.addMetric(labels,cap.getTotalMemory() == 0 ? 0 : ((double) cap.getUsedMemory() / cap.getTotalMemory()) * 100);
                    }));

                    samples.add(new GaugeMetricFamily(seriesName(HostNamespace.CPUCapacityTotal.getName()), "help for CPUCapacityTotal", totalCpu));
                    samples.add(new GaugeMetricFamily(seriesName(HostNamespace.CPUUsedCapacityCount.getName()), "help for CPUUsedCapacityCount", usedCpu));
                    samples.add(new GaugeMetricFamily(seriesName(HostNamespace.CPUUsedCapacityInPercent.getName()), "help for CPUUsedCapacityInPercent", totalCpu == 0 ? 0 : ((double)usedCpu / totalCpu) * 100));
                    samples.add(new GaugeMetricFamily(seriesName(HostNamespace.CPUAvailableCapacityCount.getName()), "help for CPUAvailableCapacityCount", availCpu));
                    samples.add(new GaugeMetricFamily(seriesName(HostNamespace.CPUAvailableCapacityInPercent.getName()), "help for CPUAvailableCapacityInPercent", totalCpu == 0 ? 0 : ((double) availCpu / totalCpu) * 100));
                    samples.add(new GaugeMetricFamily(seriesName(HostNamespace.CPULockedCapacityCount.getName()), "help for CPULockedCapacityCount", lockedCpu));
                    samples.add(new GaugeMetricFamily(seriesName(HostNamespace.CPULockedCapacityInPercent.getName()), "help for CPULockedCapacityInPercent", totalCpu == 0 ? 0 : ((double) lockedCpu / totalCpu) * 100));

                    samples.add(new GaugeMetricFamily(seriesName(HostNamespace.MemoryCapacityTotal.getName()), "help for MemoryCapacityTotal", totalMem));
                    samples.add(new GaugeMetricFamily(seriesName(HostNamespace.MemoryUsedCapacityInBytes.getName()), "help for MemoryUsedCapacityInBytes", usedMem));
                    samples.add(new GaugeMetricFamily(seriesName(HostNamespace.MemoryUsedCapacityInPercent.getName()), "help for MemoryUsedCapacityInPercent", totalMem == 0 ? 0 : ((double) usedMem / totalMem) * 100));
                    samples.add(new GaugeMetricFamily(seriesName(HostNamespace.MemoryAvailableCapacityInBytes.getName()), "help for MemoryAvailableCapacityInBytes", availMem));
                    samples.add(new GaugeMetricFamily(seriesName(HostNamespace.MemoryAvailableCapacityInPercent.getName()), "help for MemoryAvailableCapacityInPercent", totalMem == 0 ? 0 : ((double) availMem / totalMem) * 100));
                    samples.add(new GaugeMetricFamily(seriesName(HostNamespace.MemoryLockedCapacityInBytes.getName()), "help for MemoryLockedCapacityInBytes", lockedMem));
                    samples.add(new GaugeMetricFamily(seriesName(HostNamespace.MemoryLockedCapacityInPercent.getName()), "help for MemoryLockedCapacityInPercent", totalMem == 0 ? 0 : ((double) lockedMem / totalMem) * 100));

                    return samples;
                }

                private List<Collector.MetricFamilySamples> createHostMetrics() {
                    List<Collector.MetricFamilySamples> samples = new ArrayList<>();
                    createAllHostMetrics(samples);
                    GaugeMetricFamily DiskXfsFragInPercent = createMetric(HostNamespace.DiskXfsFragInPercent);
                    Map<String, String> hostXfsFragMap = HostXfsFragReader.getHostXfsFrag();
                    for (Map.Entry<String, String> entry : hostXfsFragMap.entrySet()) {
                        DiskXfsFragInPercent.addMetric(asList(entry.getKey()), Double.parseDouble(entry.getValue()));
                    }
                    samples.add(DiskXfsFragInPercent);
                    return samples;
                }

                private void createAllHostMetrics(List<MetricFamilySamples> samples) {
                    Long total = sql("select count(h) from HostVO h", Long.class).find();
                    Long connected = sql("select count(h) from HostVO h where h.status = :status", Long.class)
                            .param("status", HostStatus.Connected).find();
                    Long disconnected = sql("select count(h) from HostVO h where h.status = :status", Long.class)
                            .param("status", HostStatus.Disconnected).find();

                    samples.add(new GaugeMetricFamily(
                            seriesName(HostNamespace.HostTotal.getName()),
                            "help for HostTotal", total.doubleValue()));
                    samples.add(new GaugeMetricFamily(
                            seriesName(HostNamespace.ConnectedHostCount.getName()),
                            "help for ConnectedHostCount", connected.doubleValue()));
                    samples.add(new GaugeMetricFamily(
                            seriesName(HostNamespace.DisconnectedHostCount.getName()),
                            "help for DisconnectedHostCount",
                            disconnected.doubleValue()));
                    samples.add(new GaugeMetricFamily(
                            seriesName(HostNamespace.ConnectedHostInPercent.getName()),
                            "help for ConnectedHostInPercent",
                            total == 0 ? 0 : connected.doubleValue() / total.doubleValue() * 100));
                    samples.add(new GaugeMetricFamily(
                            seriesName(HostNamespace.DisconnectedHostInPercent.getName()),
                            "help for DisconnectedHostInPercent",
                            total == 0 ? 0 : disconnected.doubleValue() / total.doubleValue() * 100));

                    totalHosts = total;
                }
            }.execute();
        }

        @Override
        public String getCollectorName() {
            return HostCollector.class.getName();
        }

    }

    @StaticInit
    static void staticInit() {
        PrometheusNamespace.namespacesClasses.put(HostNamespace.class, HostPrometheusNamespace.class);
        PrometheusCollector.registerMetricCollector(new HostCollector());
    }

    public HostPrometheusNamespace(Namespace namespace) {
        super(namespace);
    }

    @Override
    protected RecordingRule createRecordingRule(Metric metric) {
        RecordingRule rule = new RecordingRule(makeSeriesName(metric.getName()));

        // no mapping needed. Other metrics use hostUuid label from external sources and need mapping to HostUuid.
        if (metric instanceof InfoMetric) {
            rule.setExpression(makeSeriesName(metric.getName()));
            rule.setSkipRecord(true);
            return rule;
        }

        String ignoredFSType = "proc|tmpfs|rootfs|ramfs|iso9660|rpc_pipefs";
        String zstackMountPointPrefix = "/tmp/zs-.*";

        if (metric == HostNamespace.CPUIdleUtilization) {
            rule.setExpression("collectd_cpu_percent{type=\"idle\", hostUuid!=\"\"}");
            rule.labelMapping("cpu", HostNamespace.LabelNames.CPUNum.toString());
        } else if (metric == HostNamespace.CPUSystemUtilization) {
            rule.setExpression("collectd_cpu_percent{type=\"system\", hostUuid!=\"\"}");
            rule.labelMapping("cpu", HostNamespace.LabelNames.CPUNum.toString());
        } else if (metric == HostNamespace.CPUUserUtilization) {
            rule.setExpression("collectd_cpu_percent{type=\"user\", hostUuid!=\"\"}");
            rule.labelMapping("cpu", HostNamespace.LabelNames.CPUNum.toString());
        } else if (metric == HostNamespace.CPUWaitUtilization) {
            rule.setExpression("collectd_cpu_percent{type=\"wait\", hostUuid!=\"\"}");
            rule.labelMapping("cpu", HostNamespace.LabelNames.CPUNum.toString());
        } else if (metric == HostNamespace.CPUAllIdleUtilization) {
            rule.setExpression("(sum(collectd_cpu_percent{type=\"idle\", hostUuid!=\"\"}) by(hostUuid) / sum(collectd_cpu_percent{hostUuid!=\"\"}) by(hostUuid)) * 100");
        } else if (metric == HostNamespace.CPUUsedUtilization) {
            // collectd_cpu_percent might return 100.00000000000001
            // in order to avoid negative value use abs here
            rule.setExpression("abs(100 - collectd_cpu_percent{type=\"idle\", hostUuid!=\"\"})");
            rule.labelMapping("cpu", HostNamespace.LabelNames.CPUNum.toString());
        } else if (metric == HostNamespace.CPUAllUsedUtilization) {
            rule.setExpression("clamp((sum(100 - collectd_cpu_percent{type=\"idle\", hostUuid!=\"\"}) by(hostUuid) / sum(collectd_cpu_percent{hostUuid!=\"\"}) by(hostUuid)) * 100, 0, 100)");
        } else if (metric == HostNamespace.CPUAverageUsedUtilization) {
            rule.setExpression("avg((sum(100 - collectd_cpu_percent{type=\"idle\", hostUuid!=\"\"}) by(hostUuid) / sum(collectd_cpu_percent{hostUuid!=\"\"}) by(hostUuid)) * 100) by (hostUuid)");
        } else if (metric == HostNamespace.CPUAverageUserUtilization) {
            rule.setExpression("avg((sum(collectd_cpu_percent{type=\"user\", hostUuid!=\"\"}) by(hostUuid) / sum(collectd_cpu_percent{hostUuid!=\"\"}) by(hostUuid)) * 100) by (hostUuid)");
        } else if (metric == HostNamespace.CPUAverageWaitUtilization) {
            rule.setExpression("avg((sum(collectd_cpu_percent{type=\"wait\", hostUuid!=\"\"}) by(hostUuid) / sum(collectd_cpu_percent{hostUuid!=\"\"}) by(hostUuid)) * 100) by (hostUuid)");
        } else if (metric == HostNamespace.CPUAverageSystemUtilization) {
            rule.setExpression("avg((sum(collectd_cpu_percent{type=\"system\", hostUuid!=\"\"}) by(hostUuid) / sum(collectd_cpu_percent{hostUuid!=\"\"}) by(hostUuid)) * 100) by (hostUuid)");
        } else if (metric == HostNamespace.CPUAverageIdleUtilization) {
            rule.setExpression("avg((sum(collectd_cpu_percent{type=\"idle\", hostUuid!=\"\"}) by(hostUuid) / sum(collectd_cpu_percent{hostUuid!=\"\"}) by(hostUuid)) * 100) by (hostUuid)");
        } else if (metric == HostNamespace.DiskFreeCapacityInBytes) {
            rule.setExpression(String.format("node_filesystem_avail{hostUuid!=\"\", fstype!~\"%s\", mountpoint!~\"%s\"}", ignoredFSType, zstackMountPointPrefix));
            rule.labelMapping("device", HostNamespace.LabelNames.DiskDeviceLetter.toString());
            rule.labelMapping("mountpoint", HostNamespace.LabelNames.MountPoint.toString());
            rule.labelMapping("fstype", HostNamespace.LabelNames.FSType.toString());
        } else if (metric == HostNamespace.DiskFreeCapacityInPercent) {
            // +1 to handle the case where node_filesystem_size is 0
            // one byte won't effect the accuracy of disk usage too much
            rule.setExpression(String.format("((node_filesystem_avail{hostUuid!=\"\", fstype!~\"%s\", mountpoint!~\"%s\"}  + 1) / (node_filesystem_size{hostUuid!=\"\", fstype!~\"%s\", mountpoint!~\"%s\"} + 1)) * 100",
                    ignoredFSType, zstackMountPointPrefix, ignoredFSType, zstackMountPointPrefix));
            rule.labelMapping("device", HostNamespace.LabelNames.DiskDeviceLetter.toString());
            rule.labelMapping("mountpoint", HostNamespace.LabelNames.MountPoint.toString());
            rule.labelMapping("fstype", HostNamespace.LabelNames.FSType.toString());
        } else if (metric == HostNamespace.DiskUsedCapacityInBytes) {
            rule.setExpression(String.format("node_filesystem_size{hostUuid!=\"\", fstype!~\"%s\", mountpoint!~\"%s\"}  - node_filesystem_avail{hostUuid!=\"\", fstype!~\"%s\", mountpoint!~\"%s\"}",
                    ignoredFSType, zstackMountPointPrefix, ignoredFSType, zstackMountPointPrefix));
            rule.labelMapping("device", HostNamespace.LabelNames.DiskDeviceLetter.toString());
            rule.labelMapping("mountpoint", HostNamespace.LabelNames.MountPoint.toString());
            rule.labelMapping("fstype", HostNamespace.LabelNames.FSType.toString());
        } else if (metric == HostNamespace.DiskUsedCapacityInPercent) {
            rule.setExpression(String.format("(((node_filesystem_size{hostUuid!=\"\", fstype!~\"%s\", mountpoint!~\"%s\"}  - node_filesystem_avail{hostUuid!=\"\", fstype!~\"%s\", mountpoint!~\"%s\"}) + 1) / (node_filesystem_size{hostUuid!=\"\", fstype!~\"%s\", mountpoint!~\"%s\"}  + 1)) * 100",
                    ignoredFSType, zstackMountPointPrefix, ignoredFSType, zstackMountPointPrefix, ignoredFSType, zstackMountPointPrefix));
            rule.labelMapping("device", HostNamespace.LabelNames.DiskDeviceLetter.toString());
            rule.labelMapping("mountpoint", HostNamespace.LabelNames.MountPoint.toString());
            rule.labelMapping("fstype", HostNamespace.LabelNames.FSType.toString());
        } else if (metric == HostNamespace.DiskTotalCapacityInBytes) {
            rule.setExpression(String.format("sum(node_filesystem_size{hostUuid!=\"\", fstype!~\"%s\", mountpoint!~\"%s\"}) by(hostUuid)", ignoredFSType, zstackMountPointPrefix));
        } else if (metric == HostNamespace.DiskAllFreeCapacityInBytes) {
            rule.setExpression(String.format("sum(node_filesystem_avail{hostUuid!=\"\", fstype!~\"%s\", mountpoint!~\"%s\"}) by(hostUuid)", ignoredFSType, zstackMountPointPrefix));
        } else if (metric == HostNamespace.DiskAllFreeCapacityInPercent) {
            rule.setExpression(String.format("(sum(node_filesystem_avail{hostUuid!=\"\", fstype!~\"%s\", mountpoint!~\"%s\"}) by(hostUuid) / sum(node_filesystem_size{hostUuid!=\"\", fstype!~\"%s\", mountpoint!~\"%s\"}) by(hostUuid)) * 100",
                    ignoredFSType, zstackMountPointPrefix, ignoredFSType, zstackMountPointPrefix));
        } else if (metric == HostNamespace.DiskAllUsedCapacityInBytes) {
            rule.setExpression(String.format("sum(node_filesystem_size{hostUuid!=\"\", fstype!~\"%s\", mountpoint!~\"%s\"} - node_filesystem_avail{hostUuid!=\"\", fstype!~\"%s\", mountpoint!~\"%s\"}) by(hostUuid)",
                    ignoredFSType, zstackMountPointPrefix, ignoredFSType, zstackMountPointPrefix));
        } else if (metric == HostNamespace.DiskAllUsedCapacityInPercent) {
            rule.setExpression(String.format("(sum(node_filesystem_size{hostUuid!=\"\", fstype!~\"%s\", mountpoint!~\"%s\"} - node_filesystem_avail{hostUuid!=\"\", fstype!~\"%s\", mountpoint!~\"%s\"}) by(hostUuid) / sum(node_filesystem_size{hostUuid!=\"\", fstype!~\"%s\", mountpoint!~\"%s\"}) by(hostUuid)) * 100",
                    ignoredFSType, zstackMountPointPrefix, ignoredFSType, zstackMountPointPrefix, ignoredFSType, zstackMountPointPrefix));
        } else if (metric == HostNamespace.DiskZStackUsedCapacityInBytes) {
            rule.setExpression("zstack_used_capacity_in_bytes");
        } else if (metric == HostNamespace.DiskZStackUsedCapacityInPercent) {
            rule.setExpression(String.format("(sum(zstack_used_capacity_in_bytes) by(hostUuid) / sum(node_filesystem_size{hostUuid!=\"\", fstype!~\"%s\", mountpoint!~\"%s\"}) by(hostUuid)) * 100",
                    ignoredFSType, zstackMountPointPrefix));
        } else if (metric == HostNamespace.DiskRootUsedCapacityInBytes) {
            rule.setExpression("sum(node_filesystem_size{hostUuid!=\"\", fstype!=\"rootfs\",mountpoint=\"/\"} - node_filesystem_avail{hostUuid!=\"\", fstype!=\"rootfs\",mountpoint=\"/\"}) by(hostUuid)");
        } else if (metric == HostNamespace.DiskRootUsedCapacityInPercent) {
            rule.setExpression("(sum(node_filesystem_size{hostUuid!=\"\", fstype!=\"rootfs\",mountpoint=\"/\"} - node_filesystem_avail{hostUuid!=\"\", fstype!=\"rootfs\",mountpoint=\"/\"}) by(hostUuid) / sum(node_filesystem_size{hostUuid!=\"\", fstype!=\"rootfs\",mountpoint=\"/\"}) by(hostUuid)) * 100");
        } else if (metric == HostNamespace.DiskTransUsedCapacityInBytes) {
            rule.setExpression("sum(node_filesystem_size{hostUuid!=\"\", fstype!=\"rootfs\"} - node_filesystem_avail{hostUuid!=\"\", fstype!=\"rootfs\"}) by(hostUuid) - sum(zstack_used_capacity_in_bytes) by(hostUuid)");
        } else if (metric == HostNamespace.DiskTransUsedCapacityInPercent) {
            rule.setExpression("(sum(node_filesystem_size{hostUuid!=\"\", fstype!=\"rootfs\"} - node_filesystem_avail{hostUuid!=\"\", fstype!=\"rootfs\"}) by(hostUuid) - sum(zstack_used_capacity_in_bytes) by(hostUuid)) / sum(node_filesystem_size{hostUuid!=\"\", fstype!=\"rootfs\"}) by(hostUuid) * 100");
        } else if (metric == HostNamespace.DiskReadBytes) {
            rule.setExpression("irate(collectd_disk_disk_octets_0{hostUuid!=\"\"}[10m])");
            rule.labelMapping("disk", HostNamespace.LabelNames.DiskDeviceLetter.toString());
        } else if (metric == HostNamespace.DiskAllReadBytes) {
            rule.setExpression("sum(irate(collectd_disk_disk_octets_0{hostUuid!=\"\"}[10m])) by(hostUuid)");
        } else if (metric == HostNamespace.DiskReadOps) {
            rule.setExpression("irate(collectd_disk_disk_ops_0{hostUuid!=\"\"}[10m])");
            rule.labelMapping("disk", HostNamespace.LabelNames.DiskDeviceLetter.toString());
        } else if (metric == HostNamespace.DiskAllReadOps) {
            rule.setExpression("sum(irate(collectd_disk_disk_ops_0{hostUuid!=\"\"}[10m])) by(hostUuid)");
        } else if (metric == HostNamespace.DiskWriteBytes) {
            rule.setExpression("irate(collectd_disk_disk_octets_1{hostUuid!=\"\"}[10m])");
            rule.labelMapping("disk", HostNamespace.LabelNames.DiskDeviceLetter.toString());
        } else if (metric == HostNamespace.DiskAllWriteBytes) {
            rule.setExpression("sum(irate(collectd_disk_disk_octets_1{hostUuid!=\"\"}[10m])) by(hostUuid)");
        } else if (metric == HostNamespace.DiskWriteOps) {
            rule.setExpression("irate(collectd_disk_disk_ops_1{hostUuid!=\"\"}[10m])");
            rule.labelMapping("disk", HostNamespace.LabelNames.DiskDeviceLetter.toString());
        } else if (metric == HostNamespace.DiskAllWriteOps) {
            rule.setExpression("sum(irate(collectd_disk_disk_ops_1{hostUuid!=\"\"}[10m])) by(hostUuid)");
        } else if (metric == HostNamespace.DiskLatency) {
            rule.setExpression("(delta(collectd_disk_disk_io_time_0[1m]) + delta(collectd_disk_disk_io_time_1[1m])+1) / (delta(collectd_disk_disk_ops_0[1m]) + delta(collectd_disk_disk_ops_1[1m])+1)");
            rule.labelMapping("disk", HostNamespace.LabelNames.DiskDeviceLetter.toString());
        } else if (metric == HostNamespace.DiskLatencyWwid) {
            rule.setExpression("(delta(collectd_disk_disk_io_time_0[1m]) + delta(collectd_disk_disk_io_time_1[1m])+1) / (delta(collectd_disk_disk_ops_0[1m]) + delta(collectd_disk_disk_ops_1[1m])+1) * on (disk, hostUuid) group_left(wwid) node_disk_wwid");
            rule.labelMapping("wwid", HostNamespace.LabelNames.Wwid.toString());
        } else if (metric == HostNamespace.DiskWriteOpsWwid) {
            rule.setExpression("irate(collectd_disk_disk_ops_1[10m]) * on (disk, hostUuid) group_left(wwid) node_disk_wwid");
            rule.labelMapping("wwid", HostNamespace.LabelNames.Wwid.toString());
        } else if (metric == HostNamespace.DiskReadOpsWwid) {
            rule.setExpression("irate(collectd_disk_disk_ops_0[10m:]) * on (disk, hostUuid) group_left(wwid) node_disk_wwid");
            rule.labelMapping("wwid", HostNamespace.LabelNames.Wwid.toString());
        } else if (metric == HostNamespace.DiskWriteBytesWwid) {
            rule.setExpression("irate(collectd_disk_disk_octets_1[10m:]) * on (disk, hostUuid) group_left(wwid) node_disk_wwid");
            rule.labelMapping("wwid", HostNamespace.LabelNames.Wwid.toString());
        } else if (metric == HostNamespace.DiskReadBytesWwid) {
            rule.setExpression("irate(collectd_disk_disk_octets_0[10m:]) * on (disk, hostUuid) group_left(wwid) node_disk_wwid");
            rule.labelMapping("wwid", HostNamespace.LabelNames.Wwid.toString());
        } else if (metric == HostNamespace.PhysicalVolumeStatus) {
            rule.setExpression("disk_device_status * on (disk, hostUuid) group_left(wwid) node_disk_wwid");
            rule.labelMapping("wwid", HostNamespace.LabelNames.Wwid.toString());
            rule.labelMapping("disk", HostNamespace.LabelNames.DiskDeviceLetter.toString());
        } else if (metric == HostNamespace.PhysicalVolumeState) {
            rule.setExpression("disk_device_state * on (disk, hostUuid) group_left(wwid) node_disk_wwid");
            rule.labelMapping("wwid", HostNamespace.LabelNames.Wwid.toString());
            rule.labelMapping("disk", HostNamespace.LabelNames.DiskDeviceLetter.toString());
        } else if (metric == HostNamespace.VolumeGroupCapacityInbytes) {
            rule.setExpression("vg_size");
            rule.labelMapping("vg_name", HostNamespace.LabelNames.VolumeGroupName.toString());
        } else if (metric == HostNamespace.VolumeGroupFreeCapacityInbytes) {
            rule.setExpression("vg_avail");
            rule.labelMapping("vg_name", HostNamespace.LabelNames.VolumeGroupName.toString());
        } else if (metric == HostNamespace.VolumeGroupUsedCapacityInPercent) {
            rule.setExpression("((vg_size - vg_avail + 1) / (vg_size + 1)) * 100");
            rule.labelMapping("vg_name", HostNamespace.LabelNames.VolumeGroupName.toString());
        } else if (metric == HostNamespace.RaidState) {
            rule.setExpression("raid_state");
            rule.labelMapping("target_id", HostNamespace.LabelNames.TargetId.toString());
        } else if (metric == HostNamespace.PhysicalDiskState) {
            rule.setExpression("physical_disk_state");
            rule.labelMapping("slot_number", HostNamespace.LabelNames.SlotNumber.toString());
            rule.labelMapping("disk_group", HostNamespace.LabelNames.DiskGroup.toString());
        } else if (metric == HostNamespace.PhysicalDiskTemperature) {
            rule.setExpression("physical_disk_temperature");
            rule.labelMapping("slot_number", HostNamespace.LabelNames.SlotNumber.toString());
            rule.labelMapping("disk_group", HostNamespace.LabelNames.DiskGroup.toString());
        } else if (metric == HostNamespace.PowerSupply) {
            rule.setExpression("power_supply");
            rule.labelMapping("ps_id", HostNamespace.LabelNames.PowerSupplyId.toString());
        } else if (metric == HostNamespace.PowerSupplyCurrentOutputPower) {
            rule.setExpression("power_supply_current_output_power");
            rule.labelMapping("ps_id", HostNamespace.LabelNames.PowerSupplyId.toString());
        } else if (metric == HostNamespace.IpmiStatus) {
            rule.setExpression("ipmi_status");
        } else if (metric == HostNamespace.PhysicalNetworkInterface) {
            rule.setExpression("physical_network_interface");
            rule.labelMapping("interface_name", HostNamespace.LabelNames.InterfaceName.toString());
            rule.labelMapping("speed", HostNamespace.LabelNames.InterfaceSpeed.toString());
        } else if (metric == HostNamespace.NetworkInBytes) {
            rule.setExpression("irate(collectd_interface_if_octets_0{hostUuid!=\"\"}[10m])");
            rule.labelMapping("interface", HostNamespace.LabelNames.NetworkDeviceLetter.toString());
        } else if (metric == HostNamespace.NetworkAllInBytes) {
            rule.setExpression("sum(irate(host_network_all_in_bytes{hostUuid!=\"\"}[10m])) by(hostUuid)");
        } else if (metric == HostNamespace.NetworkAllInBytesByServiceType) {
            rule.setExpression("sum(irate(host_network_all_in_bytes_by_service_type{hostUuid!=\"\"}[10m])) by(hostUuid, service_type)");
            rule.labelMapping("service_type", HostNamespace.LabelNames.NetworkServiceType.toString());
        } else if (metric == HostNamespace.NetworkInPackets) {
            rule.setExpression("irate(collectd_interface_if_packets_0{hostUuid!=\"\"}[10m])");
            rule.labelMapping("interface", HostNamespace.LabelNames.NetworkDeviceLetter.toString());
        } else if (metric == HostNamespace.NetworkAllInPackets) {
            rule.setExpression("sum(irate(host_network_all_in_packages{hostUuid!=\"\"}[10m])) by(hostUuid)");
        } else if (metric == HostNamespace.NetworkAllInPacketsByServiceType) {
            rule.setExpression("sum(irate(host_network_all_in_packages_by_service_type{hostUuid!=\"\"}[10m])) by(hostUuid, service_type)");
            rule.labelMapping("service_type", HostNamespace.LabelNames.NetworkServiceType.toString());
        } else if (metric == HostNamespace.NetworkInErrors) {
            rule.setExpression("irate(collectd_interface_if_errors_0{hostUuid!=\"\"}[10m])");
            rule.labelMapping("interface", HostNamespace.LabelNames.NetworkDeviceLetter.toString());
        } else if (metric == HostNamespace.NetworkInDropped) {
            rule.setExpression("irate(collectd_interface_if_dropped_0{hostUuid!=\"\"}[10m])");
            rule.labelMapping("interface", HostNamespace.LabelNames.NetworkDeviceLetter.toString());
        } else if (metric == HostNamespace.NetworkAllInErrors) {
            rule.setExpression("sum(irate(host_network_all_in_errors{hostUuid!=\"\"}[10m])) by(hostUuid)");
        } else if (metric == HostNamespace.NetworkAllInErrorsByServiceType) {
            rule.setExpression("sum(irate(host_network_all_in_bytes_by_service_type{hostUuid!=\"\"}[10m])) by(hostUuid, service_type)");
            rule.labelMapping("service_type", HostNamespace.LabelNames.NetworkServiceType.toString());
        } else if (metric == HostNamespace.NetworkOutBytes) {
            rule.setExpression("irate(collectd_interface_if_octets_1{hostUuid!=\"\"}[10m])");
            rule.labelMapping("interface", HostNamespace.LabelNames.NetworkDeviceLetter.toString());
        } else if (metric == HostNamespace.NetworkAllOutBytes) {
            rule.setExpression("sum(irate(host_network_all_out_bytes{hostUuid!=\"\"}[10m])) by(hostUuid)");
        } else if (metric == HostNamespace.NetworkAllOutBytesByServiceType) {
            rule.setExpression("sum(irate(host_network_all_out_bytes_by_service_type{hostUuid!=\"\"}[10m])) by(hostUuid, service_type)");
            rule.labelMapping("service_type", HostNamespace.LabelNames.NetworkServiceType.toString());
        } else if (metric == HostNamespace.NetworkOutPackets) {
            rule.setExpression("irate(collectd_interface_if_packets_1{hostUuid!=\"\"}[10m])");
            rule.labelMapping("interface", HostNamespace.LabelNames.NetworkDeviceLetter.toString());
        } else if (metric == HostNamespace.NetworkAllOutPackets) {
            rule.setExpression("sum(irate(host_network_all_out_packages{hostUuid!=\"\"}[10m])) by(hostUuid)");
        } else if (metric == HostNamespace.NetworkAllOutPacketsByServiceType) {
            rule.setExpression("sum(irate(host_network_all_out_packages_by_service_type{hostUuid!=\"\"}[10m])) by(hostUuid, service_type)");
            rule.labelMapping("service_type", HostNamespace.LabelNames.NetworkServiceType.toString());
        } else if (metric == HostNamespace.NetworkOutErrors) {
            rule.setExpression("irate(collectd_interface_if_errors_1{hostUuid!=\"\"}[10m])");
            rule.labelMapping("interface", HostNamespace.LabelNames.NetworkDeviceLetter.toString());
        } else if (metric == HostNamespace.NetworkOutDropped) {
            rule.setExpression("irate(collectd_interface_if_dropped_1{hostUuid!=\"\"}[10m])");
            rule.labelMapping("interface", HostNamespace.LabelNames.NetworkDeviceLetter.toString());
        } else if (metric == HostNamespace.NetworkAllOutErrors) {
            rule.setExpression("sum(irate(host_network_all_out_errors{hostUuid!=\"\"}[10m])) by(hostUuid)");
        }  else if (metric == HostNamespace.NetworkAllOutErrorsByServiceType) {
            rule.setExpression("sum(irate(host_network_all_out_errors_by_service_type{hostUuid!=\"\"}[10m])) by(hostUuid, service_type)");
            rule.labelMapping("service_type", HostNamespace.LabelNames.NetworkServiceType.toString());
        } else if (metric == HostNamespace.MemoryFreeBytes) {
            rule.setExpression("node_memory_MemAvailable_bytes");
        } else if (metric == HostNamespace.MemoryFreeInPercent) {
            rule.setExpression("100 * (node_memory_MemAvailable_bytes/node_memory_MemTotal_bytes)");
        } else if (metric == HostNamespace.MemoryUsedBytes) {
            rule.setExpression("node_memory_MemTotal_bytes-node_memory_MemAvailable_bytes");
        } else if (metric == HostNamespace.MemoryUsedInPercent) {
            rule.setExpression("100 * (1 - (node_memory_MemAvailable_bytes/node_memory_MemTotal_bytes))");
        } else if(metric == HostNamespace.NetworkConntrackCount) {
            rule.setExpression("zstack_conntrack_in_count");
        } else if(metric == HostNamespace.NetworkConntrackInPercent){
            rule.setExpression("zstack_conntrack_in_percent");
        } else if (metric == HostNamespace.FanSpeedRpm) {
            rule.setExpression("fan_speed_rpm");
            rule.labelMapping("fan_speed_name", HostNamespace.LabelNames.FanSpeedName.toString());
        } else if (metric == HostNamespace.FanSpeedState) {
            rule.setExpression("fan_speed_state");
            rule.labelMapping("fan_speed_name", HostNamespace.LabelNames.FanSpeedName.toString());
        } else if (metric == HostNamespace.CpuTemperature) {
            rule.setExpression("cpu_temperature");
            rule.labelMapping("cpu", HostNamespace.LabelNames.CPUNum.toString());
        } else if (metric == HostNamespace.CpuStatus) {
            rule.setExpression("cpu_status");
            rule.labelMapping("cpu", HostNamespace.LabelNames.CPUNum.toString());
        } else if (metric == HostNamespace.PhysicalMemoryStatus) {
            rule.setExpression("physical_memory_status");
            rule.labelMapping("slot_number", HostNamespace.LabelNames.SlotNumber.toString());
        } else if (metric == HostNamespace.SSDLifeLeft) {
            rule.setExpression("ssd_life_left");
            rule.labelMapping("disk", HostNamespace.LabelNames.DiskDeviceLetter.toString());
            rule.labelMapping("serial_number", HostNamespace.LabelNames.SerialNumber.toString());
        } else if (metric == HostNamespace.SSDTemperature) {
            rule.setExpression("ssd_temperature");
            rule.labelMapping("disk", HostNamespace.LabelNames.DiskDeviceLetter.toString());
            rule.labelMapping("serial_number", HostNamespace.LabelNames.SerialNumber.toString());
        } else if (metric == HostNamespace.BlockDeviceUsedCapacityInBytes) {
            rule.setExpression("block_device_used_capacity_in_bytes");
            rule.labelMapping("disk", HostNamespace.LabelNames.DiskDeviceLetter.toString());
        } else if (metric == HostNamespace.BlockDeviceUsedCapacityInPercent) {
            rule.setExpression("block_device_used_capacity_in_percent");
            rule.labelMapping("disk", HostNamespace.LabelNames.DiskDeviceLetter.toString());
        } else if (metric == HostNamespace.SharedPagesMemoryInBytes) {
            rule.setExpression("host_ksm_pages_shared_in_bytes");
        } else if (metric == HostNamespace.ReclaimedMemoryInBytes) {
            rule.setExpression("clamp_min(sum(collectd_virt_memory{hostUuid!=\"\", type=\"max_balloon\"}) by (hostUuid) - on(hostUuid) sum(collectd_virt_memory{hostUuid!=\"\", type=\"actual_balloon\"}) by (hostUuid), 0)");
        } else if (metric == HostNamespace.GpuPowerDraw) {
            rule.setExpression("host_gpu_power_draw");
            rule.labelMapping("pci_device_address", HostAbstractNamespace.LabelNames.PciDeviceAddress.toString());
            rule.labelMapping("gpu_serial", HostAbstractNamespace.LabelNames.GpuSerialNumber.toString());
        } else if (metric == HostNamespace.GpuTemperature) {
            rule.setExpression("host_gpu_temperature");
            rule.labelMapping("pci_device_address", HostAbstractNamespace.LabelNames.PciDeviceAddress.toString());
            rule.labelMapping("gpu_serial", HostAbstractNamespace.LabelNames.GpuSerialNumber.toString());
        } else if (metric == HostNamespace.GpuFanSpeed) {
            rule.setExpression("host_gpu_fan_speed");
            rule.labelMapping("pci_device_address", HostAbstractNamespace.LabelNames.PciDeviceAddress.toString());
            rule.labelMapping("gpu_serial", HostAbstractNamespace.LabelNames.GpuSerialNumber.toString());
        } else if (metric == HostNamespace.GpuUtilization) {
            rule.setExpression("host_gpu_utilization");
            rule.labelMapping("pci_device_address", HostAbstractNamespace.LabelNames.PciDeviceAddress.toString());
            rule.labelMapping("gpu_serial", HostAbstractNamespace.LabelNames.GpuSerialNumber.toString());
        } else if (metric == HostNamespace.GpuMemoryUtilization) {
            rule.setExpression("host_gpu_memory_utilization");
            rule.labelMapping("pci_device_address", HostAbstractNamespace.LabelNames.PciDeviceAddress.toString());
            rule.labelMapping("gpu_serial", HostAbstractNamespace.LabelNames.GpuSerialNumber.toString());
        } else if (metric == HostNamespace.GpuPciRxThroughputInBytes) {
            rule.setExpression("host_gpu_rxpci_in_bytes");
            rule.labelMapping("pci_device_address", HostAbstractNamespace.LabelNames.PciDeviceAddress.toString());
            rule.labelMapping("gpu_serial", HostAbstractNamespace.LabelNames.GpuSerialNumber.toString());
        } else if (metric == HostNamespace.GpuPciTxThroughputInBytes) {
            rule.setExpression("host_gpu_txpci_in_bytes");
            rule.labelMapping("pci_device_address", HostAbstractNamespace.LabelNames.PciDeviceAddress.toString());
            rule.labelMapping("gpu_serial", HostAbstractNamespace.LabelNames.GpuSerialNumber.toString());
        } else if (metric == HostNamespace.VGpuUtilization) {
            rule.setExpression("vgpu_utilization");
            rule.labelMapping("mdev_uuid", HostAbstractNamespace.LabelNames.MdevDeviceUuid.toString());
            rule.labelMapping("vm_uuid", HostAbstractNamespace.LabelNames.VMUuid.toString());
        } else if (metric == HostNamespace.GpuStatus) {
            rule.setExpression("host_gpu_status");
            rule.labelMapping("pci_device_address", HostAbstractNamespace.LabelNames.PciDeviceAddress.toString());
            rule.labelMapping("gpuStatus", HostAbstractNamespace.LabelNames.GpuStatus.toString());
            rule.labelMapping("gpu_serial", HostAbstractNamespace.LabelNames.GpuSerialNumber.toString());
        } else if (metric == HostNamespace.VGpuMemoryUtilization) {
            rule.setExpression("vgpu_memory_utilization");
            rule.labelMapping("mdev_uuid", HostAbstractNamespace.LabelNames.MdevDeviceUuid.toString());
            rule.labelMapping("vm_uuid", HostAbstractNamespace.LabelNames.VMUuid.toString());
        } else if (metric == HostNamespace.IPMIPhysicalMemoryStatus) {
            rule.setExpression("ipmi_memory_status");
            rule.labelMapping("name", HostAbstractNamespace.LabelNames.Name.toString());
            rule.labelMapping("type", HostAbstractNamespace.LabelNames.Type.toString());
        } else if (metric == HostNamespace.IPMIPowerSupply) {
            rule.setExpression("ipmi_sensor_state{type=\"Power Supply\"}");
            rule.labelMapping("name", HostAbstractNamespace.LabelNames.Name.toString());
            rule.labelMapping("type", HostAbstractNamespace.LabelNames.Type.toString());
        } else if (metric == HostNamespace.IPMIFanSpeedState) {
            rule.setExpression("ipmi_fan_speed_state");
            rule.labelMapping("name", HostAbstractNamespace.LabelNames.Name.toString());
            rule.labelMapping("type", HostAbstractNamespace.LabelNames.Type.toString());
        } else if (metric == HostNamespace.IPMIFanSpeedRpm) {
            rule.setExpression("ipmi_fan_speed_rpm");
            rule.labelMapping("name", HostAbstractNamespace.LabelNames.Name.toString());
            rule.labelMapping("type", HostAbstractNamespace.LabelNames.Type.toString());
        } else if (metric == HostNamespace.IPMITemperatureState) {
            rule.setExpression("ipmi_temperature_state");
            rule.labelMapping("name", HostAbstractNamespace.LabelNames.Name.toString());
            rule.labelMapping("type", HostAbstractNamespace.LabelNames.Type.toString());
        } else if (metric == HostNamespace.IPMITemperatureCelsius) {
            rule.setExpression("ipmi_temperature_celsius");
            rule.labelMapping("name", HostAbstractNamespace.LabelNames.Name.toString());
            rule.labelMapping("type", HostAbstractNamespace.LabelNames.Type.toString());
        } else if (metric == HostNamespace.FcHbaLinkFailureTotal) {
            rule.setExpression("increase(node_fibrechannel_link_failure_total{hostUuid!=\"\"}[5m])");
            rule.labelMapping("fc_host", HostAbstractNamespace.LabelNames.Name.toString());
        } else if (metric == HostNamespace.FcHbaLossOfSignalTotal) {
            rule.setExpression("increase(node_fibrechannel_loss_of_signal_total{hostUuid!=\"\"}[5m])");
            rule.labelMapping("fc_host", HostAbstractNamespace.LabelNames.Name.toString());
        } else {
            rule.setForLabelMappingOnly(true);
            metric.getLabelNames().forEach(l -> rule.labelMapping(l, l));
            return rule;
        }

        rule.setSeriesName(makeSeriesName(metric.getName()));
        rule.labelMapping("hostUuid", HostNamespace.LabelNames.HostUuid.toString());
        return rule;
    }
}
