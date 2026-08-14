package org.zstack.zwatch.prometheus;

import org.zstack.header.core.StaticInit;
import org.zstack.zwatch.datatype.metric.Metric;
import org.zstack.zwatch.datatype.Namespace;
import org.zstack.zwatch.namespace.BaremetalVmNamespace;

public class BaremetalVmPrometheusNamespace extends AbstractPrometheusNamespace {

    @StaticInit
    static void staticInit() {
        PrometheusNamespace.namespacesClasses.put(BaremetalVmNamespace.class, BaremetalVmPrometheusNamespace.class);
    }

    protected RecordingRule createRecordingRule(Metric metric) {
        RecordingRule rule = new RecordingRule(makeSeriesName(metric.getName()));
        rule.labelMapping("virt", BaremetalVmNamespace.LabelNames.BaremetalVMUuid.toString());

        if (metric == BaremetalVmNamespace.OperatingSystemCPUSystemUtilization) {
            rule.setExpression("bm_collectd_cpu_percent{type=\"system\", vmUuid!=\"\"}");
            rule.labelMapping("vmUuid", BaremetalVmNamespace.LabelNames.BaremetalVMUuid.toString());
            rule.labelMapping("cpu", BaremetalVmNamespace.LabelNames.CPUNum.toString());
        } else if (metric == BaremetalVmNamespace.OperatingSystemCPUUserUtilization) {
            rule.setExpression("bm_collectd_cpu_percent{type=\"user\", vmUuid!=\"\"}");
            rule.labelMapping("vmUuid", BaremetalVmNamespace.LabelNames.BaremetalVMUuid.toString());
            rule.labelMapping("cpu", BaremetalVmNamespace.LabelNames.CPUNum.toString());
        } else if (metric == BaremetalVmNamespace.OperatingSystemCPUWaitUtilization) {
            rule.setExpression("bm_collectd_cpu_percent{type=\"wait\", vmUuid!=\"\"}");
            rule.labelMapping("vmUuid", BaremetalVmNamespace.LabelNames.BaremetalVMUuid.toString());
            rule.labelMapping("cpu", BaremetalVmNamespace.LabelNames.CPUNum.toString());
        } else if (metric == BaremetalVmNamespace.OperatingSystemCPUIdleUtilization) {
            rule.setExpression("(sum(bm_collectd_cpu_percent{type=\"idle\", vmUuid!=\"\"}) by(vmUuid) / sum(bm_collectd_cpu_percent) by(vmUuid)) * 100");
            rule.labelMapping("vmUuid", BaremetalVmNamespace.LabelNames.BaremetalVMUuid.toString());
            rule.labelMapping("cpu", BaremetalVmNamespace.LabelNames.CPUNum.toString());
        } else if (metric == BaremetalVmNamespace.OperatingSystemCPUUsedUtilization) {
            rule.setExpression("100 - bm_collectd_cpu_percent{type=\"idle\", vmUuid!=\"\"}");
            rule.labelMapping("vmUuid", BaremetalVmNamespace.LabelNames.BaremetalVMUuid.toString());
            rule.labelMapping("cpu", BaremetalVmNamespace.LabelNames.CPUNum.toString());
        } else if (metric == BaremetalVmNamespace.OperatingSystemCPUAverageSystemUtilization) {
            rule.setExpression("avg(bm_collectd_cpu_percent{type=\"system\", vmUuid!=\"\"}) by (vmUuid)");
            rule.labelMapping("vmUuid", BaremetalVmNamespace.LabelNames.BaremetalVMUuid.toString());
        } else if (metric == BaremetalVmNamespace.OperatingSystemCPUAverageUserUtilization) {
            rule.setExpression("avg(bm_collectd_cpu_percent{type=\"user\", vmUuid!=\"\"}) by (vmUuid)");
            rule.labelMapping("vmUuid", BaremetalVmNamespace.LabelNames.BaremetalVMUuid.toString());
        } else if (metric == BaremetalVmNamespace.OperatingSystemCPUAverageWaitUtilization) {
            rule.setExpression("avg(bm_collectd_cpu_percent{type=\"wait\", vmUuid!=\"\"}) by (vmUuid)");
            rule.labelMapping("vmUuid", BaremetalVmNamespace.LabelNames.BaremetalVMUuid.toString());
        } else if (metric == BaremetalVmNamespace.OperatingSystemCPUAverageIdleUtilization) {
            rule.setExpression("avg(bm_collectd_cpu_percent{type=\"idle\", vmUuid!=\"\"}) by (vmUuid)");
            rule.labelMapping("vmUuid", BaremetalVmNamespace.LabelNames.BaremetalVMUuid.toString());
        } else if (metric == BaremetalVmNamespace.OperatingSystemCPUAverageUsedUtilization) {
            rule.setExpression("avg(100 - bm_collectd_cpu_percent{type=\"idle\", vmUuid!=\"\"}) by (vmUuid)");
            rule.labelMapping("vmUuid", BaremetalVmNamespace.LabelNames.BaremetalVMUuid.toString());
        } else if (metric == BaremetalVmNamespace.OperatingSystemMemoryTotalBytes) {
            rule.setExpression("sum(bm_collectd_memory{vmUuid!=\"\"}) by (vmUuid)");
            rule.labelMapping("vmUuid", BaremetalVmNamespace.LabelNames.BaremetalVMUuid.toString());
        } else if (metric == BaremetalVmNamespace.OperatingSystemMemoryFreeBytes) {
            rule.setExpression("bm_collectd_memory{vmUuid!=\"\",  memory=\"free\"}");
            rule.labelMapping("vmUuid", BaremetalVmNamespace.LabelNames.BaremetalVMUuid.toString());
        } else if (metric == BaremetalVmNamespace.OperatingSystemMemoryUsedBytes) {
            rule.setExpression("bm_collectd_memory{vmUuid!=\"\",  memory=\"used\"}");
            rule.labelMapping("vmUuid", BaremetalVmNamespace.LabelNames.BaremetalVMUuid.toString());
        } else if (metric == BaremetalVmNamespace.OperatingSystemMemoryAvailableBytes) {
            rule.setExpression("bm_node_memory_available {vmUuid!=\"\"}");
            rule.labelMapping("vmUuid", BaremetalVmNamespace.LabelNames.BaremetalVMUuid.toString());
        } else if (metric == BaremetalVmNamespace.OperatingSystemMemoryFreePercent) {
            rule.setExpression("100 * (sum(bm_collectd_memory{memory=\"free\", vmUuid!=\"\"}) by (vmUuid))/ (sum(bm_collectd_memory{vmUuid!=\"\"}) by (vmUuid))");
            rule.labelMapping("vmUuid", BaremetalVmNamespace.LabelNames.BaremetalVMUuid.toString());
        } else if (metric == BaremetalVmNamespace.OperatingSystemMemoryUsedPercent) {
            rule.setExpression("100 * (sum(bm_collectd_memory{memory=\"used\", vmUuid!=\"\"}) by (vmUuid))/ (sum(bm_collectd_memory{vmUuid!=\"\"}) by (vmUuid))");
            rule.labelMapping("vmUuid", BaremetalVmNamespace.LabelNames.BaremetalVMUuid.toString());
        } else if (metric == BaremetalVmNamespace.DiskFreeCapacityInBytes) {
            rule.setExpression("bm_node_filesystem_avail{vmUuid!=\"\"}");
            rule.labelMapping("device", BaremetalVmNamespace.LabelNames.DiskDeviceLetter.toString());
            rule.labelMapping("mountpoint", BaremetalVmNamespace.LabelNames.MountPoint.toString());
            rule.labelMapping("vmUuid", BaremetalVmNamespace.LabelNames.BaremetalVMUuid.toString());
            rule.labelMapping("fstype", BaremetalVmNamespace.LabelNames.FSType.toString());
        } else if (metric == BaremetalVmNamespace.DiskFreeCapacityInPercent) {
            // +1 to handle the case where node_filesystem_total is 0
            // one byte won't effect the accuracy of disk usage too much
            rule.setExpression("((bm_node_filesystem_avail{vmUuid!=\"\"}  + 1) / (bm_node_filesystem_size{vmUuid!=\"\"} + 1)) * 100");
            rule.labelMapping("device", BaremetalVmNamespace.LabelNames.DiskDeviceLetter.toString());
            rule.labelMapping("mountpoint", BaremetalVmNamespace.LabelNames.MountPoint.toString());
            rule.labelMapping("vmUuid", BaremetalVmNamespace.LabelNames.BaremetalVMUuid.toString());
            rule.labelMapping("fstype", BaremetalVmNamespace.LabelNames.FSType.toString());
        } else if (metric == BaremetalVmNamespace.DiskUsedCapacityInBytes) {
            rule.setExpression("bm_node_filesystem_size{vmUuid!=\"\"} - bm_node_filesystem_avail{vmUuid!=\"\"}");
            rule.labelMapping("device", BaremetalVmNamespace.LabelNames.DiskDeviceLetter.toString());
            rule.labelMapping("mountpoint", BaremetalVmNamespace.LabelNames.MountPoint.toString());
            rule.labelMapping("vmUuid", BaremetalVmNamespace.LabelNames.BaremetalVMUuid.toString());
            rule.labelMapping("fstype", BaremetalVmNamespace.LabelNames.FSType.toString());
        } else if (metric == BaremetalVmNamespace.DiskUsedCapacityInPercent) {
            rule.setExpression("(((bm_node_filesystem_size{vmUuid!=\"\"} - bm_node_filesystem_avail{vmUuid!=\"\"}) + 1) / (bm_node_filesystem_size{vmUuid!=\"\"}  + 1)) * 100");
            rule.labelMapping("device", BaremetalVmNamespace.LabelNames.DiskDeviceLetter.toString());
            rule.labelMapping("mountpoint", BaremetalVmNamespace.LabelNames.MountPoint.toString());
            rule.labelMapping("vmUuid", BaremetalVmNamespace.LabelNames.BaremetalVMUuid.toString());
            rule.labelMapping("fstype", BaremetalVmNamespace.LabelNames.FSType.toString());
        } else if (metric == BaremetalVmNamespace.DiskTotalCapacityInBytes) {
            rule.setExpression("bm_node_filesystem_size{vmUuid!=\"\"}");
            rule.labelMapping("device", BaremetalVmNamespace.LabelNames.DiskDeviceLetter.toString());
            rule.labelMapping("mountpoint", BaremetalVmNamespace.LabelNames.MountPoint.toString());
            rule.labelMapping("vmUuid", BaremetalVmNamespace.LabelNames.BaremetalVMUuid.toString());
            rule.labelMapping("fstype", BaremetalVmNamespace.LabelNames.FSType.toString());
        } else if (metric == BaremetalVmNamespace.DiskReadBytesPerSecond) {
            rule.setExpression("bm_node_disk_bytes_read{vmUuid!=\"\"}");
            rule.labelMapping("device", BaremetalVmNamespace.LabelNames.DiskDeviceLetter.toString());
            rule.labelMapping("mountpoint", BaremetalVmNamespace.LabelNames.MountPoint.toString());
            rule.labelMapping("vmUuid", BaremetalVmNamespace.LabelNames.BaremetalVMUuid.toString());
            rule.labelMapping("fstype", BaremetalVmNamespace.LabelNames.FSType.toString());
        } else if (metric == BaremetalVmNamespace.DiskReadRequestPerSecond) {
            rule.setExpression("bm_node_disk_reads_completed{vmUuid!=\"\"}");
            rule.labelMapping("device", BaremetalVmNamespace.LabelNames.DiskDeviceLetter.toString());
            rule.labelMapping("mountpoint", BaremetalVmNamespace.LabelNames.MountPoint.toString());
            rule.labelMapping("vmUuid", BaremetalVmNamespace.LabelNames.BaremetalVMUuid.toString());
            rule.labelMapping("fstype", BaremetalVmNamespace.LabelNames.FSType.toString());
        } else if (metric == BaremetalVmNamespace.DiskWriteBytesPerSecond) {
            rule.setExpression("bm_node_disk_bytes_written{vmUuid!=\"\"}");
            rule.labelMapping("device", BaremetalVmNamespace.LabelNames.DiskDeviceLetter.toString());
            rule.labelMapping("mountpoint", BaremetalVmNamespace.LabelNames.MountPoint.toString());
            rule.labelMapping("vmUuid", BaremetalVmNamespace.LabelNames.BaremetalVMUuid.toString());
            rule.labelMapping("fstype", BaremetalVmNamespace.LabelNames.FSType.toString());
        } else if (metric == BaremetalVmNamespace.DiskWriteRequestPerSecond) {
            rule.setExpression("bm_node_disk_writes_completed{vmUuid!=\"\"}");
            rule.labelMapping("device", BaremetalVmNamespace.LabelNames.DiskDeviceLetter.toString());
            rule.labelMapping("mountpoint", BaremetalVmNamespace.LabelNames.MountPoint.toString());
            rule.labelMapping("vmUuid", BaremetalVmNamespace.LabelNames.BaremetalVMUuid.toString());
            rule.labelMapping("fstype", BaremetalVmNamespace.LabelNames.FSType.toString());
        } else if (metric == BaremetalVmNamespace.DiskAllFreeCapacityInBytes) {
            rule.setExpression("sum(bm_node_filesystem_avail{vmUuid!=\"\", fstype!=\"rootfs\"}) by(vmUuid)");
            rule.labelMapping("vmUuid", BaremetalVmNamespace.LabelNames.BaremetalVMUuid.toString());
        } else if (metric == BaremetalVmNamespace.DiskAllFreeCapacityInPercent) {
            rule.setExpression("(sum(bm_node_filesystem_avail{vmUuid!=\"\", fstype!=\"rootfs\"}) by(vmUuid) / sum(bm_node_filesystem_size{vmUuid!=\"\", fstype!=\"rootfs\"}) by(vmUuid)) * 100");
            rule.labelMapping("vmUuid", BaremetalVmNamespace.LabelNames.BaremetalVMUuid.toString());
        } else if (metric == BaremetalVmNamespace.DiskAllUsedCapacityInBytes) {
            rule.setExpression("sum(bm_node_filesystem_size{vmUuid!=\"\", fstype!=\"rootfs\"} - bm_node_filesystem_avail{vmUuid!=\"\", fstype!=\"rootfs\"}) by(vmUuid)");
            rule.labelMapping("vmUuid", BaremetalVmNamespace.LabelNames.BaremetalVMUuid.toString());
        } else if (metric == BaremetalVmNamespace.DiskAllUsedCapacityInPercent) {
            rule.setExpression("(sum(bm_node_filesystem_size{vmUuid!=\"\", fstype!=\"rootfs\"} - bm_node_filesystem_avail{vmUuid!=\"\", fstype!=\"rootfs\"}) by(vmUuid) / sum(bm_node_filesystem_size{vmUuid!=\"\", fstype!=\"rootfs\"}) by(vmUuid)) * 100");
            rule.labelMapping("vmUuid", BaremetalVmNamespace.LabelNames.BaremetalVMUuid.toString());
        } else if (metric == BaremetalVmNamespace.OperatingSystemNetworkInBytes) {
            rule.setExpression("irate(bm_node_network_receive_bytes{vmUuid!=\"\"}[10m])");
            rule.labelMapping("device", BaremetalVmNamespace.LabelNames.NetworkDeviceLetter.toString());
            rule.labelMapping("vmUuid", BaremetalVmNamespace.LabelNames.BaremetalVMUuid.toString());
        }  else if (metric == BaremetalVmNamespace.OperatingSystemNetworkAllInBytes) {
            rule.setExpression("sum(irate(bm_node_network_receive_bytes{vmUuid!=\"\"}[10m])) by (vmUuid)");
            rule.labelMapping("vmUuid", BaremetalVmNamespace.LabelNames.BaremetalVMUuid.toString());
        }  else if (metric == BaremetalVmNamespace.OperatingSystemNetworkInPackets) {
            rule.setExpression("irate(bm_node_network_receive_packets{vmUuid!=\"\"}[10m])");
            rule.labelMapping("device", BaremetalVmNamespace.LabelNames.NetworkDeviceLetter.toString());
            rule.labelMapping("vmUuid", BaremetalVmNamespace.LabelNames.BaremetalVMUuid.toString());
        }  else if (metric == BaremetalVmNamespace.OperatingSystemNetworkAllInPackets) {
            rule.setExpression("sum(irate(bm_node_network_receive_packets{vmUuid!=\"\"}[10m])) by (vmUuid)");
            rule.labelMapping("vmUuid", BaremetalVmNamespace.LabelNames.BaremetalVMUuid.toString());
        }  else if (metric == BaremetalVmNamespace.OperatingSystemNetworkInErrors) {
            rule.setExpression("irate(bm_node_network_receive_errs{vmUuid!=\"\"}[10m])");
            rule.labelMapping("device", BaremetalVmNamespace.LabelNames.NetworkDeviceLetter.toString());
            rule.labelMapping("vmUuid", BaremetalVmNamespace.LabelNames.BaremetalVMUuid.toString());
        }  else if (metric == BaremetalVmNamespace.OperatingSystemNetworkAllInErrors) {
            rule.setExpression("sum(irate(bm_node_network_receive_errs{vmUuid!=\"\"}[10m])) by (vmUuid)");
            rule.labelMapping("device", BaremetalVmNamespace.LabelNames.NetworkDeviceLetter.toString());
            rule.labelMapping("vmUuid", BaremetalVmNamespace.LabelNames.BaremetalVMUuid.toString());
        }  else if (metric == BaremetalVmNamespace.OperatingSystemNetworkOutBytes) {
            rule.setExpression("irate(bm_node_network_transmit_bytes{vmUuid!=\"\"}[10m])");
            rule.labelMapping("device", BaremetalVmNamespace.LabelNames.NetworkDeviceLetter.toString());
            rule.labelMapping("vmUuid", BaremetalVmNamespace.LabelNames.BaremetalVMUuid.toString());
        }  else if (metric == BaremetalVmNamespace.OperatingSystemNetworkAllOutBytes) {
            rule.setExpression("sum(irate(bm_node_network_transmit_bytes{vmUuid!=\"\"}[10m])) by (vmUuid)");
            rule.labelMapping("vmUuid", BaremetalVmNamespace.LabelNames.BaremetalVMUuid.toString());
        }  else if (metric == BaremetalVmNamespace.OperatingSystemNetworkOutPackets) {
            rule.setExpression("irate(bm_node_network_transmit_packets{vmUuid!=\"\"}[10m])");
            rule.labelMapping("device", BaremetalVmNamespace.LabelNames.NetworkDeviceLetter.toString());
            rule.labelMapping("vmUuid", BaremetalVmNamespace.LabelNames.BaremetalVMUuid.toString());
        }  else if (metric == BaremetalVmNamespace.OperatingSystemNetworkAllOutPackets) {
            rule.setExpression("sum(irate(bm_node_network_transmit_packets{vmUuid!=\"\"}[10m])) by (vmUuid)");
            rule.labelMapping("vmUuid", BaremetalVmNamespace.LabelNames.BaremetalVMUuid.toString());
        }  else if (metric == BaremetalVmNamespace.OperatingSystemNetworkOutErrors) {
            rule.setExpression("irate(bm_node_network_transmit_errs{vmUuid!=\"\"}[10m])");
            rule.labelMapping("device", BaremetalVmNamespace.LabelNames.NetworkDeviceLetter.toString());
            rule.labelMapping("vmUuid", BaremetalVmNamespace.LabelNames.BaremetalVMUuid.toString());
        }  else if (metric == BaremetalVmNamespace.OperatingSystemNetworkAllOutErrors) {
            rule.setExpression("sum(irate(bm_node_network_transmit_errs{vmUuid!=\"\"}[10m])) by (vmUuid)");
            rule.labelMapping("vmUuid", BaremetalVmNamespace.LabelNames.BaremetalVMUuid.toString());
        } else {
            rule.setForLabelMappingOnly(true);
            metric.getLabelNames().forEach(l->rule.labelMapping(l, l));
            return rule;
        }

        return rule;
    }

    public BaremetalVmPrometheusNamespace(Namespace namespace) {
        super(namespace);
    }
}
