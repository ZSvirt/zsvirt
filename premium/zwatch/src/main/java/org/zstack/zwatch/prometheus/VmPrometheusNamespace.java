package org.zstack.zwatch.prometheus;

import io.prometheus.client.Collector;
import io.prometheus.client.GaugeMetricFamily;
import org.apache.commons.lang.StringUtils;
import org.zstack.core.db.SQL;
import org.zstack.core.db.SQLBatchWithReturn;
import org.zstack.header.core.StaticInit;
import org.zstack.utils.network.IPv6Constants;
import org.zstack.zwatch.datatype.Namespace;
import org.zstack.zwatch.datatype.metric.InfoMetric;
import org.zstack.zwatch.datatype.metric.Metric;
import org.zstack.zwatch.namespace.VmAbstractNamespace;
import org.zstack.zwatch.namespace.VmNamespace;

import javax.persistence.Tuple;
import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;
import static org.zstack.zwatch.ZWatchConstants.*;

public class VmPrometheusNamespace extends AbstractPrometheusNamespace {
    public VmPrometheusNamespace(Namespace namespace) {
        super(namespace);
    }

    public static class VmCollector extends BasicMetricCollector {
        @Override
        protected String getNamespaceName() {
            return VmNamespace.NAME;
        }

        protected String getApplianceVmType() {
            return null;
        }

        private List<Collector.MetricFamilySamples> createVmInfoMetric() {
            List<Collector.MetricFamilySamples> samples = new ArrayList<>();
            GaugeMetricFamily vmInfoMetric = createMetric(VmAbstractNamespace.VmInfo);

            String vmInfoSql = "select vm.uuid, vm.name, uip.ip" +
                    " from VmInstanceVO vm" +
                    " left join VmNicVO nic on nic.vmInstanceUuid = vm.uuid and nic.l3NetworkUuid = vm.defaultL3NetworkUuid" +
                    " and nic.deviceId = (select min(n.deviceId) from VmNicVO n" +
                    " where n.vmInstanceUuid = vm.uuid and n.l3NetworkUuid = vm.defaultL3NetworkUuid)" +
                    " left join UsedIpVO uip on uip.vmNicUuid = nic.uuid and uip.ipVersion = :ipVersion";

            if (getApplianceVmType() != null) {
                vmInfoSql = vmInfoSql + " left join ApplianceVmVO appVm on appVm.uuid = vm.uuid" +
                        " where appVm.applianceVmType = :applianceVmType ";
            }

            SQL vmInfoQuery = SQL.New(vmInfoSql, Tuple.class)
                    .param("ipVersion", IPv6Constants.IPv4);
            if (getApplianceVmType() != null) {
                vmInfoQuery.param("applianceVmType", getApplianceVmType());
            }

            List<Tuple> rows = vmInfoQuery.list();

            rows.forEach(row -> {
                String uuid = row.get(0, String.class);
                String name = row.get(1, String.class);
                String defaultIp = row.get(2, String.class);
                vmInfoMetric.addMetric(asList(
                        StringUtils.defaultString(uuid),
                        StringUtils.defaultString(name),
                        StringUtils.defaultString(defaultIp)
                ), 1D);
            });

            samples.add(vmInfoMetric);
            return samples;
        }

        @Override
        public List<Collector.MetricFamilySamples> collect() {
            return new SQLBatchWithReturn<List<Collector.MetricFamilySamples>>() {
                @Override
                protected List<Collector.MetricFamilySamples> scripts() {
                    List<Collector.MetricFamilySamples> samples = new ArrayList<>();
                    samples.addAll(createVmInfoMetric());
                    return samples;
                }
            }.execute();
        }

        @Override
        public String getCollectorName() {
            return VmCollector.class.getName();
        }
    }

    @StaticInit
    static void staticInit() {
        PrometheusNamespace.namespacesClasses.put(VmNamespace.class, VmPrometheusNamespace.class);
        PrometheusCollector.registerMetricCollector(new VmCollector());
    }

    //In Prometheus calculate it is necessary to set floating-point arithmetic precision to avoid floating-point overflow.
    private static final String NUMERIC_EPSILON = "0.000001";

    @Override
    protected RecordingRule createRecordingRule(Metric metric) {
        RecordingRule rule = new RecordingRule(makeSeriesName(metric.getName()));
        // no mapping needed. Other metrics use virt/vmUuid/vrouter labels from external sources and need mapping to VMUuid.
        if (metric instanceof InfoMetric) {
            rule.setExpression(makeSeriesName(metric.getName()));
            rule.setSkipRecord(true);
            return rule;
        }

        rule.labelMapping("virt", VmNamespace.LabelNames.VMUuid.toString());

        if (metric == VmNamespace.CPUAllUsedUtilization) {
            rule.setExpression("rate(collectd_virt_virt_cpu_total[1m]) / 10000000");
        } else if (metric == VmNamespace.CPUUsedUtilization) {
            rule.setExpression("rate(collectd_virt_virt_vcpu[1m]) / 10000000");
            rule.labelMapping("type", VmNamespace.LabelNames.CPUNum.toString());
        } else if (metric == VmNamespace.CPUAverageUsedUtilization) {
            rule.setExpression("avg(rate(collectd_virt_virt_vcpu[1m]) / 10000000) by (virt)");
        } else if (metric == VmNamespace.CPUAllIdleUtilization) {
            rule.setExpression("100 - (rate(collectd_virt_virt_cpu_total[1m]) / 10000000)");
        } else if (metric == VmNamespace.CPUIdleUtilization) {
            rule.setExpression("100 - (rate(collectd_virt_virt_vcpu[1m]) / 10000000)");
            rule.labelMapping("type", VmNamespace.LabelNames.CPUNum.toString());
        } else if (metric == VmNamespace.OperatingSystemCPUSystemUtilization) {
            // windows CPU privileged time simply used as kernel mode cpu time
            // details: http://jira.zstack.io/browse/ZSTAC-45385
            rule.setExpression("collectd_cpu_percent{type=\"system\", vmUuid!=\"\"} or irate(wmi_cpu_time_total{type=\"privileged\", vmUuid!=\"\"}[5m]) * 100");
            rule.labelMapping("vmUuid", VmNamespace.LabelNames.VMUuid.toString());
            rule.labelMapping("cpu", VmNamespace.LabelNames.CPUNum.toString());
        } else if (metric == VmNamespace.OperatingSystemCPUUserUtilization) {
            rule.setExpression("collectd_cpu_percent{type=\"user\", vmUuid!=\"\"} or irate(wmi_cpu_time_total{type=\"user\", vmUuid!=\"\"}[5m]) * 100");
            rule.labelMapping("vmUuid", VmNamespace.LabelNames.VMUuid.toString());
            rule.labelMapping("cpu", VmNamespace.LabelNames.CPUNum.toString());
        } else if (metric == VmNamespace.OperatingSystemCPUWaitUtilization) {
            rule.setExpression("collectd_cpu_percent{type=\"wait\", vmUuid!=\"\"}");
            rule.labelMapping("vmUuid", VmNamespace.LabelNames.VMUuid.toString());
            rule.labelMapping("cpu", VmNamespace.LabelNames.CPUNum.toString());
        } else if (metric == VmNamespace.OperatingSystemCPUIdleUtilization) {
            rule.setExpression("round(collectd_cpu_percent{type=\"idle\", vmUuid!=\"\"}, " + NUMERIC_EPSILON + ") or sum by (cpu,vmUuid) (increase(wmi_cpu_time_total{type=\"idle\", vmUuid!=\"\"}[1m])) / sum by (cpu,vmUuid) (sum(increase(wmi_cpu_time_total{vmUuid!=\"\"}[1m])) by (cpu,vmUuid)) * 100");
            rule.labelMapping("vmUuid", VmNamespace.LabelNames.VMUuid.toString());
            rule.labelMapping("cpu", VmNamespace.LabelNames.CPUNum.toString());
        } else if (metric == VmNamespace.OperatingSystemCPUUsedUtilization) {
            rule.setExpression("round(100 - collectd_cpu_percent{type=\"idle\", vmUuid!=\"\"}, " + NUMERIC_EPSILON + ") or 100 - (sum by (cpu,vmUuid) (increase(wmi_cpu_time_total{type=\"idle\", vmUuid!=\"\"}[1m])) / sum by (cpu,vmUuid) (sum(increase(wmi_cpu_time_total{vmUuid!=\"\"}[1m])) by (cpu,vmUuid)) * 100)");
            rule.labelMapping("vmUuid", VmNamespace.LabelNames.VMUuid.toString());
            rule.labelMapping("cpu", VmNamespace.LabelNames.CPUNum.toString());
        } else if (metric == VmNamespace.OperatingSystemCPUAverageSystemUtilization) {
            rule.setExpression("avg(collectd_cpu_percent{type=\"system\", vmUuid!=\"\"} or irate(wmi_cpu_time_total{type=\"privileged\", vmUuid!=\"\"}[5m]) * 100) by (vmUuid)");
            rule.labelMapping("vmUuid", VmNamespace.LabelNames.VMUuid.toString());
        } else if (metric == VmNamespace.OperatingSystemCPUAverageUserUtilization) {
            rule.setExpression("avg(collectd_cpu_percent{type=\"user\", vmUuid!=\"\"} or irate(wmi_cpu_time_total{type=\"user\", vmUuid!=\"\"}[5m]) * 100) by (vmUuid)");
            rule.labelMapping("vmUuid", VmNamespace.LabelNames.VMUuid.toString());
        } else if (metric == VmNamespace.OperatingSystemCPUAverageWaitUtilization) {
            rule.setExpression("avg(collectd_cpu_percent{type=\"wait\", vmUuid!=\"\"}) by (vmUuid)");
            rule.labelMapping("vmUuid", VmNamespace.LabelNames.VMUuid.toString());
        } else if (metric == VmNamespace.OperatingSystemCPUAverageIdleUtilization) {
            rule.setExpression("avg(round(collectd_cpu_percent{type=\"idle\", vmUuid!=\"\"}, " + NUMERIC_EPSILON + ") or sum by (cpu,vmUuid) (increase(wmi_cpu_time_total{type=\"idle\", vmUuid!=\"\"}[1m])) / sum by (cpu,vmUuid) (sum(increase(wmi_cpu_time_total{vmUuid!=\"\"}[1m])) by (cpu,vmUuid)) * 100) by (vmUuid)");
            rule.labelMapping("vmUuid", VmNamespace.LabelNames.VMUuid.toString());
        } else if (metric == VmNamespace.OperatingSystemCPUAverageUsedUtilization) {
            rule.setExpression("avg(round(100 - collectd_cpu_percent{type=\"idle\", vmUuid!=\"\"}, " + NUMERIC_EPSILON + ") or 100 - sum by (cpu,vmUuid) (increase(wmi_cpu_time_total{type=\"idle\", vmUuid!=\"\"}[1m])) / sum by (cpu,vmUuid) (sum(increase(wmi_cpu_time_total{vmUuid!=\"\"}[1m])) by (cpu,vmUuid)) * 100) by (vmUuid)");
            rule.labelMapping("vmUuid", VmNamespace.LabelNames.VMUuid.toString());
        } else if (metric == VmNamespace.MemoryFreeBytes) {
            rule.setExpression("collectd_virt_memory{type=\"unused\"}");
        } else if (metric == VmNamespace.MemoryFreeInPercent) {
            // type=available is the total memory the VM see
            // on(virt,hostUuid) for the case that the VM used to start on other hosts will have records with the same 'virt'
            // but different 'hostUuid' that results in many-to-many mapping, on(virt,hostUuid) consolidate them to one-one-mapping
            rule.setExpression("(collectd_virt_memory{type=\"unused\"} / on(virt,hostUuid) collectd_virt_memory{type=\"available\"}) * 100");
        } else if (metric == VmNamespace.MemoryUsedBytes) {
            // type=available is the total memory the VM see
            // the output is equal to "used + shared" which you see by 'free' command in the VM
            rule.setExpression("collectd_virt_memory{type=\"available\"} - on(virt,hostUuid) collectd_virt_memory{type=\"unused\"}");
        } else if (metric == VmNamespace.MemoryUsedInPercent) {
            rule.setExpression("((collectd_virt_memory{type=\"available\"} - on(virt,hostUuid) collectd_virt_memory{type=\"unused\"}) / on(virt,hostUuid) (collectd_virt_memory{type=\"available\"} + 1)) * 100");
        } else if (metric == VmNamespace.MemoryMinorPageFaults) {
            rule.setExpression("rate(collectd_virt_memory{type=\"minor_fault\"}[5m])");
        } else if (metric == VmNamespace.OperatingSystemMemoryTotalBytes) {
            rule.setExpression("sum(collectd_memory{vmUuid!=\"\"}) by (vmUuid)");
            rule.labelMapping("vmUuid", VmNamespace.LabelNames.VMUuid.toString());
        } else if (metric == VmNamespace.OperatingSystemMemoryFreeBytes) {
            rule.setExpression("collectd_memory{vmUuid!=\"\",  memory=\"free\"}");
            rule.labelMapping("vmUuid", VmNamespace.LabelNames.VMUuid.toString());
        } else if (metric == VmNamespace.OperatingSystemMemoryUsedBytes) {
            rule.setExpression("collectd_memory{vmUuid!=\"\",  memory=\"used\"}");
            rule.labelMapping("vmUuid", VmNamespace.LabelNames.VMUuid.toString());
        } else if (metric == VmNamespace.OperatingSystemMemoryAvailableBytes) {
            rule.setExpression("node_memory_available{vmUuid!=\"\"}");
            rule.labelMapping("vmUuid", VmNamespace.LabelNames.VMUuid.toString());
        } else if (metric == VmNamespace.OperatingSystemMemoryFreePercent) {
            rule.setExpression("100 * (sum(collectd_memory{memory=\"free\", vmUuid!=\"\"}) by (vmUuid))/ (sum(collectd_memory{vmUuid!=\"\"}) by (vmUuid) + 1)");
            rule.labelMapping("vmUuid", VmNamespace.LabelNames.VMUuid.toString());
        } else if (metric == VmNamespace.OperatingSystemMemoryUsedPercent) {
            rule.setExpression("100 * (sum(collectd_memory{memory=\"used\", vmUuid!=\"\"}) by (vmUuid))/ (sum(collectd_memory{vmUuid!=\"\"}) by (vmUuid) + 1)");
            rule.labelMapping("vmUuid", VmNamespace.LabelNames.VMUuid.toString());
        } else if (metric == VmNamespace.DiskReadBytes) {
            rule.setExpression("irate(collectd_virt_disk_octets_0[10m])");
            rule.labelMapping("type", VmNamespace.LabelNames.DiskDeviceLetter.toString());
        } else if (metric == VmNamespace.DiskAllReadBytes) {
            rule.setExpression("sum(irate(collectd_virt_disk_octets_0[10m])) by(virt)");
        } else if (metric == VmNamespace.DiskReadOps) {
            rule.setExpression("irate(collectd_virt_disk_ops_0[10m])");
            rule.labelMapping("type", VmNamespace.LabelNames.DiskDeviceLetter.toString());
        } else if (metric == VmNamespace.DiskAllReadOps) {
            rule.setExpression("sum(irate(collectd_virt_disk_ops_0[10m])) by (virt)");
        } else if (metric == VmNamespace.DiskWriteBytes) {
            rule.setExpression("irate(collectd_virt_disk_octets_1[10m])");
            rule.labelMapping("type", VmNamespace.LabelNames.DiskDeviceLetter.toString());
        } else if (metric == VmNamespace.DiskAllWriteBytes) {
            rule.setExpression("sum(irate(collectd_virt_disk_octets_1[10m])) by(virt)");
        } else if (metric == VmNamespace.DiskWriteOps) {
            rule.setExpression("irate(collectd_virt_disk_ops_1[10m])");
            rule.labelMapping("type", VmNamespace.LabelNames.DiskDeviceLetter.toString());
        } else if (metric == VmNamespace.DiskAllWriteOps) {
            rule.setExpression("sum(irate(collectd_virt_disk_ops_1[10m])) by (virt)");
        } else if (metric == VmNamespace.DiskFreeCapacityInBytes) {
            rule.setExpression(String.format("node_filesystem_avail{vmUuid!=\"\", fstype!~\"%1$s\", mountpoint!~\"%2$s\"}",
                    IGNORED_FS_TYPE,
                    WINDOWS_HARDDISK_PREFIX));
            rule.labelMapping("device", VmNamespace.LabelNames.DiskDeviceLetter.toString());
            rule.labelMapping("mountpoint", VmNamespace.LabelNames.MountPoint.toString());
            rule.labelMapping("vmUuid", VmNamespace.LabelNames.VMUuid.toString());
            rule.labelMapping("fstype", VmNamespace.LabelNames.FSType.toString());
        } else if (metric == VmNamespace.DiskFreeCapacityInPercent) {
            // +1 to handle the case where node_filesystem_total is 0
            // one byte won't effect the accuracy of disk usage too much
            rule.setExpression(String.format("((node_filesystem_avail{vmUuid!=\"\", fstype!~\"%1$s\", mountpoint!~\"%2$s\"}  + 1) / (node_filesystem_size{vmUuid!=\"\", fstype!~\"%1$s\", mountpoint!~\"%2$s\"} + 1)) * 100",
                    IGNORED_FS_TYPE,
                    WINDOWS_HARDDISK_PREFIX));
            rule.labelMapping("device", VmNamespace.LabelNames.DiskDeviceLetter.toString());
            rule.labelMapping("mountpoint", VmNamespace.LabelNames.MountPoint.toString());
            rule.labelMapping("vmUuid", VmNamespace.LabelNames.VMUuid.toString());
            rule.labelMapping("fstype", VmNamespace.LabelNames.FSType.toString());
        } else if (metric == VmNamespace.DiskUsedCapacityInBytes) {
            rule.setExpression(String.format("node_filesystem_size{vmUuid!=\"\", fstype!~\"%1$s\", mountpoint!~\"%2$s\"} - node_filesystem_avail{vmUuid!=\"\", fstype!~\"%1$s\", mountpoint!~\"%2$s\"}",
                    IGNORED_FS_TYPE,
                    WINDOWS_HARDDISK_PREFIX));
            rule.labelMapping("device", VmNamespace.LabelNames.DiskDeviceLetter.toString());
            rule.labelMapping("mountpoint", VmNamespace.LabelNames.MountPoint.toString());
            rule.labelMapping("vmUuid", VmNamespace.LabelNames.VMUuid.toString());
            rule.labelMapping("fstype", VmNamespace.LabelNames.FSType.toString());
        } else if (metric == VmNamespace.DiskUsedCapacityInPercent) {
            rule.setExpression(String.format("(((node_filesystem_size{vmUuid!=\"\", fstype!~\"%1$s\", mountpoint!~\"%2$s\"} - node_filesystem_avail{vmUuid!=\"\", fstype!~\"%1$s\", mountpoint!~\"%2$s\"}) + 1) / (node_filesystem_size{vmUuid!=\"\", fstype!~\"%1$s\", mountpoint!~\"%2$s\"}  + 1)) * 100",
                    IGNORED_FS_TYPE,
                    WINDOWS_HARDDISK_PREFIX));
            rule.labelMapping("device", VmNamespace.LabelNames.DiskDeviceLetter.toString());
            rule.labelMapping("mountpoint", VmNamespace.LabelNames.MountPoint.toString());
            rule.labelMapping("vmUuid", VmNamespace.LabelNames.VMUuid.toString());
            rule.labelMapping("fstype", VmNamespace.LabelNames.FSType.toString());
        } else if (metric == VmNamespace.DiskAllFreeCapacityInBytes) {
            rule.setExpression(String.format("sum(node_filesystem_avail{vmUuid!=\"\", fstype!~\"%1$s\", mountpoint!~\"%2$s\"}) by(vmUuid)",
                    IGNORED_FS_TYPE,
                    WINDOWS_HARDDISK_PREFIX));
            rule.labelMapping("vmUuid", VmNamespace.LabelNames.VMUuid.toString());
        } else if (metric == VmNamespace.DiskAllFreeCapacityInPercent) {
            rule.setExpression(String.format("(sum(node_filesystem_avail{vmUuid!=\"\", fstype!~\"%1$s\", mountpoint!~\"%2$s\"}) by(vmUuid) / sum(node_filesystem_size{vmUuid!=\"\", fstype!~\"%1$s\", mountpoint!~\"%2$s\"}) by(vmUuid)) * 100",
                    IGNORED_FS_TYPE,
                    WINDOWS_HARDDISK_PREFIX));
            rule.labelMapping("vmUuid", VmNamespace.LabelNames.VMUuid.toString());
        } else if (metric == VmNamespace.DiskAllUsedCapacityInBytes) {
            rule.setExpression(String.format("sum(node_filesystem_size{vmUuid!=\"\", fstype!~\"%1$s\", mountpoint!~\"%2$s\"} - node_filesystem_avail{vmUuid!=\"\", fstype!~\"%1$s\", mountpoint!~\"%2$s\"}) by(vmUuid)",
                    IGNORED_FS_TYPE,
                    WINDOWS_HARDDISK_PREFIX));
            rule.labelMapping("vmUuid", VmNamespace.LabelNames.VMUuid.toString());
        } else if (metric == VmNamespace.DiskAllUsedCapacityInPercent) {
            rule.setExpression(String.format("(sum(node_filesystem_size{vmUuid!=\"\", fstype!~\"%1$s\", mountpoint!~\"%2$s\"} - node_filesystem_avail{vmUuid!=\"\", fstype!~\"%1$s\", mountpoint!~\"%2$s\"}) by(vmUuid) / sum(node_filesystem_size{vmUuid!=\"\", fstype!~\"%1$s\", mountpoint!~\"%2$s\"}) by(vmUuid)) * 100",
                    IGNORED_FS_TYPE,
                    WINDOWS_HARDDISK_PREFIX));
            rule.labelMapping("vmUuid", VmNamespace.LabelNames.VMUuid.toString());
        } else if (metric == VmNamespace.NetworkInBytes) {
            rule.setExpression("irate(collectd_virt_if_octets_0[10m])");
            rule.labelMapping("type", VmNamespace.LabelNames.NetworkDeviceLetter.toString());
        } else if (metric == VmNamespace.NetworkAllInBytes) {
            rule.setExpression("sum(irate(collectd_virt_if_octets_0[10m])) by (virt)");
        } else if (metric == VmNamespace.NetworkInPackets) {
            rule.setExpression("irate(collectd_virt_if_packets_0[10m])");
            rule.labelMapping("type", VmNamespace.LabelNames.NetworkDeviceLetter.toString());
        } else if (metric == VmNamespace.NetworkAllInPackets) {
            rule.setExpression("sum(irate(collectd_virt_if_packets_0[10m])) by (virt)");
        } else if (metric == VmNamespace.NetworkInErrors) {
            rule.setExpression("irate(collectd_virt_if_errors_0[10m])");
            rule.labelMapping("type", VmNamespace.LabelNames.NetworkDeviceLetter.toString());
        } else if (metric == VmNamespace.NetworkAllInErrors) {
            rule.setExpression("sum(irate(collectd_virt_if_errors_0[10m])) by (virt)");
        } else if (metric == VmNamespace.NetworkInDroppedBytes) {
            rule.setExpression("irate(collectd_virt_if_dropped_0[10m])");
            rule.labelMapping("type", VmNamespace.LabelNames.NetworkDeviceLetter.toString());
        } else if (metric == VmNamespace.NetworkAllInDroppedBytes) {
            rule.setExpression("sum(irate(collectd_virt_if_dropped_0[10m])) by (virt)");
        } else if (metric == VmNamespace.NetworkOutBytes) {
            rule.setExpression("irate(collectd_virt_if_octets_1[10m])");
            rule.labelMapping("type", VmNamespace.LabelNames.NetworkDeviceLetter.toString());
        } else if (metric == VmNamespace.NetworkAllOutBytes) {
            rule.setExpression("sum(irate(collectd_virt_if_octets_1[10m])) by (virt)");
        } else if (metric == VmNamespace.NetworkOutPackets) {
            rule.setExpression("irate(collectd_virt_if_packets_1[10m])");
            rule.labelMapping("type", VmNamespace.LabelNames.NetworkDeviceLetter.toString());
        } else if (metric == VmNamespace.NetworkAllOutPackets) {
            rule.setExpression("sum(irate(collectd_virt_if_packets_1[10m])) by (virt)");
        } else if (metric == VmNamespace.NetworkOutErrors) {
            rule.setExpression("irate(collectd_virt_if_errors_1[10m])");
            rule.labelMapping("type", VmNamespace.LabelNames.NetworkDeviceLetter.toString());
        } else if (metric == VmNamespace.NetworkAllOutErrors) {
            rule.setExpression("sum(irate(collectd_virt_if_errors_1[10m])) by (virt)");
        }  else if (metric == VmNamespace.NetworkOutDroppedBytes) {
            rule.setExpression("irate(collectd_virt_if_dropped_1[10m])");
            rule.labelMapping("type", VmNamespace.LabelNames.NetworkDeviceLetter.toString());
        } else if (metric == VmNamespace.NetworkAllOutDroppedBytes) {
            rule.setExpression("sum(irate(collectd_virt_if_dropped_1[10m])) by (virt)");
        } else if(metric == VmNamespace.TotalNetworkInBytesIn5Min) {
            rule.setExpression("increase(collectd_virt_if_octets_0{type!=\"\"}[5m])");
            rule.labelMapping("type", VmNamespace.LabelNames.NetworkDeviceLetter.toString());
        } else if (metric == VmNamespace.TotalNetworkInPacketsIn5Min ){
            rule.setExpression("increase(collectd_virt_if_packets_0{type!=\"\"}[5m])");
            rule.labelMapping("type", VmNamespace.LabelNames.NetworkDeviceLetter.toString());
        } else if (metric == VmNamespace.TotalNetworkOutBytesIn5Min ){
            rule.setExpression("increase(collectd_virt_if_octets_1{type!=\"\"}[5m])");
            rule.labelMapping("type", VmNamespace.LabelNames.NetworkDeviceLetter.toString());
        } else if (metric == VmNamespace.TotalNetworkOutPacketsIn5Min ) {
            rule.setExpression("increase(collectd_virt_if_packets_1{type!=\"\"}[5m])");
            rule.labelMapping("type", VmNamespace.LabelNames.NetworkDeviceLetter.toString());
        } else if(metric == VmNamespace.TotalNetworkInBytesIn1Min) {
            rule.setExpression("increase(collectd_virt_if_octets_0{type!=\"\"}[1m])");
            rule.labelMapping("type", VmNamespace.LabelNames.NetworkDeviceLetter.toString());
        } else if (metric == VmNamespace.TotalNetworkInPacketsIn1Min ){
            rule.setExpression("increase(collectd_virt_if_packets_0{type!=\"\"}[1m])");
            rule.labelMapping("type", VmNamespace.LabelNames.NetworkDeviceLetter.toString());
        } else if (metric == VmNamespace.TotalNetworkOutBytesIn1Min ){
            rule.setExpression("increase(collectd_virt_if_octets_1{type!=\"\"}[1m])");
            rule.labelMapping("type", VmNamespace.LabelNames.NetworkDeviceLetter.toString());
        } else if (metric == VmNamespace.TotalNetworkOutPacketsIn1Min ) {
            rule.setExpression("increase(collectd_virt_if_packets_1{type!=\"\"}[1m])");
            rule.labelMapping("type", VmNamespace.LabelNames.NetworkDeviceLetter.toString());
        } else if (metric == VmNamespace.ZWatchAgentVersion) {
            rule.setExpression("zwatch_vm_agent_version or zwatch_agent_version");
            rule.setSkipRecord(false);
            rule.labelMapping("vmUuid", VmNamespace.LabelNames.VMUuid.toString());
        } else if (metric == VmNamespace.AgentLastResponseTimestampDelta) {
            rule.setExpression("irate(agent_last_response_timestamp[1m])");
            rule.labelMapping("vmUuid", VmNamespace.LabelNames.VMUuid.toString());
        } else if (metric == VmNamespace.ZWatchAgentFeaturePvpanic) {
            rule.setExpression("zwatch_vm_agent_pvpanic or zwatch_agent_feature_pvpanic");
            rule.setSkipRecord(false);
            rule.labelMapping("vmUuid", VmNamespace.LabelNames.VMUuid.toString());
        } else if (metric == VmNamespace.VRouterCPUSystemUtilization) {
            rule.setExpression("vpcCpuUsage{type=\"system\"}");
            rule.labelMapping("vrouter", VmNamespace.LabelNames.VMUuid.toString());
            rule.labelMapping("cpu", VmNamespace.LabelNames.CPUNum.toString());
        } else if (metric == VmNamespace.VRouterCPUUserUtilization) {
            rule.setExpression("vpcCpuUsage{type=\"user\"}");
            rule.labelMapping("vrouter", VmNamespace.LabelNames.VMUuid.toString());
            rule.labelMapping("cpu", VmNamespace.LabelNames.CPUNum.toString());
        } else if (metric == VmNamespace.VRouterCPUWaitUtilization) {
            rule.setExpression("vpcCpuUsage{type=\"iowait\"}");
            rule.labelMapping("vrouter", VmNamespace.LabelNames.VMUuid.toString());
            rule.labelMapping("cpu", VmNamespace.LabelNames.CPUNum.toString());
        } else if (metric == VmNamespace.VRouterCPUIdleUtilization) {
            rule.setExpression("vpcCpuUsage{type=\"idle\"}");
            rule.labelMapping("vrouter", VmNamespace.LabelNames.VMUuid.toString());
            rule.labelMapping("cpu", VmNamespace.LabelNames.CPUNum.toString());
        } else if (metric == VmNamespace.VRouterCPUUsedUtilization) {
            rule.setExpression("100 - vpcCpuUsage{type=\"idle\"}");
            rule.labelMapping("vrouter", VmNamespace.LabelNames.VMUuid.toString());
            rule.labelMapping("cpu", VmNamespace.LabelNames.CPUNum.toString());
        } else if (metric == VmNamespace.VRouterCPUAverageSystemUtilization) {
            rule.setExpression("avg(vpcCpuUsage{type=\"system\"}) by (vrouter)");
            rule.labelMapping("vrouter", VmNamespace.LabelNames.VMUuid.toString());
        } else if (metric == VmNamespace.VRouterCPUAverageUserUtilization) {
            rule.setExpression("avg(vpcCpuUsage{type=\"user\"}) by (vrouter)");
            rule.labelMapping("vrouter", VmNamespace.LabelNames.VMUuid.toString());
        } else if (metric == VmNamespace.VRouterCPUAverageWaitUtilization) {
            rule.setExpression("avg(vpcCpuUsage{type=\"iowait\"}) by (vrouter)");
            rule.labelMapping("vrouter", VmNamespace.LabelNames.VMUuid.toString());
        } else if (metric == VmNamespace.VRouterCPUAverageIdleUtilization) {
            rule.setExpression("avg(vpcCpuUsage{type=\"idle\"}) by (vrouter)");
            rule.labelMapping("vrouter", VmNamespace.LabelNames.VMUuid.toString());
        } else if (metric == VmNamespace.VRouterCPUAverageUsedUtilization) {
            rule.setExpression("avg(100 - vpcCpuUsage{type=\"idle\"}) by (vrouter)");
            rule.labelMapping("vrouter", VmNamespace.LabelNames.VMUuid.toString());
        } else if (metric == VmNamespace.VRouterMemoryTotalBytes) {
            rule.setExpression("vpcMemoryUsage{memory=\"total\"}");
            rule.labelMapping("vrouter", VmNamespace.LabelNames.VMUuid.toString());
        } else if (metric == VmNamespace.VRouterMemoryFreeBytes) {
            rule.setExpression("vpcMemoryUsage{memory=\"free\"}");
            rule.labelMapping("vrouter", VmNamespace.LabelNames.VMUuid.toString());
        } else if (metric == VmNamespace.VRouterMemoryUsedBytes) {
            rule.setExpression("vpcMemoryUsage{memory=\"used\"}");
            rule.labelMapping("vrouter", VmNamespace.LabelNames.VMUuid.toString());
        } else if (metric == VmNamespace.VRouterMemoryAvailableBytes) {
            rule.setExpression("vpcMemoryUsage{memory=\"available\"}");
            rule.labelMapping("vrouter", VmNamespace.LabelNames.VMUuid.toString());
        } else if (metric == VmNamespace.VRouterMemoryFreePercent) {
            rule.setExpression("100 * sum(vpcMemoryUsage{memory=\"available\"}) by (vrouter) / sum(vpcMemoryUsage{memory=\"total\"}) by (vrouter)");
            rule.labelMapping("vrouter", VmNamespace.LabelNames.VMUuid.toString());
        } else if (metric == VmNamespace.VRouterMemoryUsedPercent) {
            rule.setExpression("100 * sum(vpcMemoryUsage{memory=\"used\"}) by (vrouter) / sum(vpcMemoryUsage{memory=\"total\"}) by (vrouter)");
            rule.labelMapping("vrouter", VmNamespace.LabelNames.VMUuid.toString());
        } else if (metric == VmNamespace.VRouterDiskFreeCapacityInBytes) {
            rule.setExpression("vpcDiskUsage{type=\"free\"}");
            rule.labelMapping("device", VmNamespace.LabelNames.DiskDeviceLetter.toString());
            rule.labelMapping("mountpoint", VmNamespace.LabelNames.MountPoint.toString());
            rule.labelMapping("vrouter", VmNamespace.LabelNames.VMUuid.toString());
            rule.labelMapping("fstype", VmNamespace.LabelNames.FSType.toString());
        } else if (metric == VmNamespace.VRouterDiskFreeCapacityInPercent) {
            rule.setExpression("vpcDiskUsage{type=\"freePercent\"}");
            rule.labelMapping("device", VmNamespace.LabelNames.DiskDeviceLetter.toString());
            rule.labelMapping("mountpoint", VmNamespace.LabelNames.MountPoint.toString());
            rule.labelMapping("vrouter", VmNamespace.LabelNames.VMUuid.toString());
            rule.labelMapping("fstype", VmNamespace.LabelNames.FSType.toString());
        } else if (metric == VmNamespace.VRouterDiskUsedCapacityInBytes) {
            rule.setExpression("vpcDiskUsage{type=\"used\"}");
            rule.labelMapping("device", VmNamespace.LabelNames.DiskDeviceLetter.toString());
            rule.labelMapping("mountpoint", VmNamespace.LabelNames.MountPoint.toString());
            rule.labelMapping("vrouter", VmNamespace.LabelNames.VMUuid.toString());
            rule.labelMapping("fstype", VmNamespace.LabelNames.FSType.toString());
        } else if (metric == VmNamespace.VRouterDiskUsedCapacityInPercent) {
            rule.setExpression("vpcDiskUsage{type=\"usedPercent\"}");
            rule.labelMapping("device", VmNamespace.LabelNames.DiskDeviceLetter.toString());
            rule.labelMapping("mountpoint", VmNamespace.LabelNames.MountPoint.toString());
            rule.labelMapping("vrouter", VmNamespace.LabelNames.VMUuid.toString());
            rule.labelMapping("fstype", VmNamespace.LabelNames.FSType.toString());
        } else if (metric == VmNamespace.VRouterDiskAllFreeCapacityInBytes) {
            rule.setExpression("sum(vpcDiskUsage{type=\"free\"}) by(vrouter)");
            rule.labelMapping("vrouter", VmNamespace.LabelNames.VMUuid.toString());
        } else if (metric == VmNamespace.VRouterDiskAllFreeCapacityInPercent) {
            /* TODO: if vpc has more than 1 volumes, it should be fixed */
            rule.setExpression("vpcDiskUsage{type=\"freePercent\"}");
            rule.labelMapping("vrouter", VmNamespace.LabelNames.VMUuid.toString());
        } else if (metric == VmNamespace.VRouterDiskAllUsedCapacityInBytes) {
            rule.setExpression("sum(vpcDiskUsage{type=\"used\"}) by(vrouter)");
            rule.labelMapping("vrouter", VmNamespace.LabelNames.VMUuid.toString());
        } else if (metric == VmNamespace.VRouterDiskAllUsedCapacityInPercent) {
            rule.setExpression("100 - vpcDiskUsage{type=\"freePercent\"}");
            rule.labelMapping("vrouter", VmNamespace.LabelNames.VMUuid.toString());
        } else if (metric == VmNamespace.CPUOccupiedByVM) {
            rule.setExpression("cpu_occupied_by_vm");
            rule.labelMapping("vmUuid", VmNamespace.LabelNames.VMUuid.toString());
        } else if (metric == VmNamespace.MemoryOccupiedByVM) {
            rule.setExpression("collectd_virt_memory{type=\"rss\"}");
        } else if (metric == VmNamespace.PVPanicEnableInDomainXML) {
            rule.setExpression("pvpanic_enable_in_domain_xml");
            rule.labelMapping("vmUuid", VmNamespace.LabelNames.VMUuid.toString());
        } else if (metric == VmAbstractNamespace.ReclaimedMemoryInBytes) {
            rule.setExpression("collectd_virt_memory{type=\"max_balloon\"} - on(virt,hostUuid) collectd_virt_memory{type=\"actual_balloon\"}");
        } else if (metric == VmNamespace.GpuPowerDraw) {
            rule.setExpression("vm_gpu_power_draw");
            rule.labelMapping("pci_device_address", VmNamespace.LabelNames.PciDeviceAddress.toString());
            rule.labelMapping("gpu_serial", VmNamespace.LabelNames.SerialNumber.toString());
            rule.labelMapping("vmUuid", VmNamespace.LabelNames.VMUuid.toString());
        } else if (metric == VmNamespace.GpuTemperature) {
            rule.setExpression("vm_gpu_temperature");
            rule.labelMapping("pci_device_address", VmNamespace.LabelNames.PciDeviceAddress.toString());
            rule.labelMapping("gpu_serial", VmNamespace.LabelNames.SerialNumber.toString());
            rule.labelMapping("vmUuid", VmNamespace.LabelNames.VMUuid.toString());
        } else if (metric == VmNamespace.GpuFanSpeed) {
            rule.setExpression("vm_gpu_fan_speed");
            rule.labelMapping("pci_device_address", VmNamespace.LabelNames.PciDeviceAddress.toString());
            rule.labelMapping("gpu_serial", VmNamespace.LabelNames.SerialNumber.toString());
            rule.labelMapping("vmUuid", VmNamespace.LabelNames.VMUuid.toString());
        } else if (metric == VmNamespace.GpuUtilization) {
            rule.setExpression("vm_gpu_utilization");
            rule.labelMapping("pci_device_address", VmNamespace.LabelNames.PciDeviceAddress.toString());
            rule.labelMapping("gpu_serial", VmNamespace.LabelNames.SerialNumber.toString());
            rule.labelMapping("vmUuid", VmNamespace.LabelNames.VMUuid.toString());
        } else if (metric == VmNamespace.GpuStatus) {
            rule.setExpression("vm_gpu_status");
            rule.labelMapping("pci_device_address", VmNamespace.LabelNames.PciDeviceAddress.toString());
            rule.labelMapping("gpuStatus", VmNamespace.LabelNames.GpuStatus.toString());
            rule.labelMapping("gpu_serial", VmNamespace.LabelNames.SerialNumber.toString());
            rule.labelMapping("vmUuid", VmNamespace.LabelNames.VMUuid.toString());
        } else if (metric == VmNamespace.GpuMemoryUtilization) {
            rule.setExpression("vm_gpu_memory_utilization");
            rule.labelMapping("pci_device_address", VmNamespace.LabelNames.PciDeviceAddress.toString());
            rule.labelMapping("gpu_serial", VmNamespace.LabelNames.SerialNumber.toString());
            rule.labelMapping("vmUuid", VmNamespace.LabelNames.VMUuid.toString());
        } else if (metric == VmNamespace.GpuPciRxThroughputInBytes) {
            rule.setExpression("vm_gpu_rxpci_in_bytes");
            rule.labelMapping("pci_device_address", VmNamespace.LabelNames.PciDeviceAddress.toString());
            rule.labelMapping("gpu_serial", VmNamespace.LabelNames.SerialNumber.toString());
            rule.labelMapping("vmUuid", VmNamespace.LabelNames.VMUuid.toString());
        } else if (metric == VmNamespace.GpuPciTxThroughputInBytes) {
            rule.setExpression("vm_gpu_txpci_in_bytes");
            rule.labelMapping("pci_device_address", VmNamespace.LabelNames.PciDeviceAddress.toString());
            rule.labelMapping("gpu_serial", VmNamespace.LabelNames.SerialNumber.toString());
            rule.labelMapping("vmUuid", VmNamespace.LabelNames.VMUuid.toString());
        } else {
            rule.setForLabelMappingOnly(true);
            metric.getLabelNames().forEach(l->rule.labelMapping(l, l));
            return rule;
        }

        return rule;
    }
}
