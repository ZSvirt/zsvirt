package org.zstack.zwatch.prometheus;

import io.prometheus.client.Collector;
import io.prometheus.client.GaugeMetricFamily;
import org.apache.commons.lang.StringUtils;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQLBatchWithReturn;
import org.zstack.header.core.StaticInit;
import org.zstack.header.storage.backup.BackupStorageState;
import org.zstack.header.storage.backup.BackupStorageStatus;
import org.zstack.header.storage.backup.BackupStorageVO;
import org.zstack.header.storage.backup.BackupStorageVO_;
import org.zstack.zwatch.datatype.metric.InfoMetric;
import org.zstack.zwatch.datatype.metric.Metric;
import org.zstack.zwatch.datatype.Namespace;
import org.zstack.zwatch.namespace.BackupStorageNamespace;

import javax.persistence.Tuple;
import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;

public class BackupStoragePrometheusNamespace extends AbstractPrometheusNamespace {
    public static class BackupStorageCollector extends BasicMetricCollector {
        @Override
        protected String getNamespaceName() {
            return BackupStorageNamespace.NAME;
        }

        @Override
        public List<Collector.MetricFamilySamples> collect() {
            return new SQLBatchWithReturn<List<Collector.MetricFamilySamples>>() {
                long total;
                long used;
                long available;
                long locked;

                private List<Collector.MetricFamilySamples> createBackupStorageInfoMetric() {
                    List<Collector.MetricFamilySamples> samples = new ArrayList<>();
                    GaugeMetricFamily backupStorageInfoMetric = createMetric(BackupStorageNamespace.BackupStorageInfo);

                    List<Tuple> rows = Q.New(BackupStorageVO.class)
                            .select(BackupStorageVO_.uuid, BackupStorageVO_.name)
                            .listTuple();

                    rows.forEach(row -> {
                        String uuid = row.get(0, String.class);
                        String name = row.get(1, String.class);
                        backupStorageInfoMetric.addMetric(asList(
                                StringUtils.defaultString(uuid),
                                StringUtils.defaultString(name)
                        ), 1D);
                    });

                    samples.add(backupStorageInfoMetric);
                    return samples;
                }

                private List<Collector.MetricFamilySamples> createCapacitySamples() {
                    List<Collector.MetricFamilySamples> samples = new ArrayList<>();

                    List<BackupStorageVO> bss = Q.New(BackupStorageVO.class)
                            .notEq(BackupStorageVO_.status, BackupStorageStatus.Connecting)
                            .list();

                    GaugeMetricFamily AvailableCapacityInBytes = createMetric(BackupStorageNamespace.AvailableCapacityInBytes);
                    samples.add(AvailableCapacityInBytes);

                    GaugeMetricFamily AvailableCapacityInPercent = createMetric(BackupStorageNamespace.AvailableCapacityInPercent);
                    samples.add(AvailableCapacityInPercent);

                    GaugeMetricFamily UsedCapacityInBytes = createMetric(BackupStorageNamespace.UsedCapacityInBytes);
                    samples.add(UsedCapacityInBytes);

                    GaugeMetricFamily UsedCapacityInPercent = createMetric(BackupStorageNamespace.UsedCapacityInPercent);
                    samples.add(UsedCapacityInPercent);

                    bss.forEach(bs -> {
                        String name = StringUtils.defaultString(bs.getName());
                        total += bs.getTotalCapacity();
                        if (bs.getState() == BackupStorageState.Enabled && bs.getStatus() == BackupStorageStatus.Connected) {
                            available += bs.getAvailableCapacity();
                        } else {
                            locked += bs.getAvailableCapacity();
                        }
                        used += bs.getTotalCapacity() - bs.getAvailableCapacity();

                        List<String> labels = asList(bs.getUuid(), bs.getType(), name);
                        AvailableCapacityInBytes.addMetric(labels, bs.getAvailableCapacity());
                        UsedCapacityInBytes.addMetric(labels, (double) (bs.getTotalCapacity() - bs.getAvailableCapacity()));

                        if (bs.getTotalCapacity() > 0) {
                            UsedCapacityInPercent.addMetric(labels, ((double) (bs.getTotalCapacity() - bs.getAvailableCapacity()) / bs.getTotalCapacity()) * 100);
                            AvailableCapacityInPercent.addMetric(labels, ((double) bs.getAvailableCapacity() / bs.getTotalCapacity()) * 100);
                        }

                    });

                    samples.add(new GaugeMetricFamily(seriesName(BackupStorageNamespace.TotalAvailableCapacityInBytes.getName()), "help for TotalAvailableCapacityInBytes", available));
                    samples.add(new GaugeMetricFamily(seriesName(BackupStorageNamespace.TotalAvailableCapacityInPercent.getName()), "help for TotalAvailableCapacityInPercent", total == 0 ? 0 : ((double) available / total) * 100));
                    samples.add(new GaugeMetricFamily(seriesName(BackupStorageNamespace.TotalUsedCapacityInBytes.getName()), "help for TotalUsedCapacityInBytes", used));
                    samples.add(new GaugeMetricFamily(seriesName(BackupStorageNamespace.TotalUsedCapacityInPercent.getName()), "help for TotalUsedCapacityInPercent", total == 0 ? 0 : ((double)used / total) * 100));
                    samples.add(new GaugeMetricFamily(seriesName(BackupStorageNamespace.TotalLockedCapacityInBytes.getName()), "help for TotalLockedCapacityInBytes", locked));
                    samples.add(new GaugeMetricFamily(seriesName(BackupStorageNamespace.TotalLockedCapacityInPercent.getName()), "help for TotalLockedCapacityInPercent", total == 0 ? 0 : ((double)locked / total) * 100));

                    return samples;
                }

                @Override
                protected List<Collector.MetricFamilySamples> scripts() {
                    List<Collector.MetricFamilySamples> samples = new ArrayList<>();

                    samples.addAll(createBackupStorageInfoMetric());
                    samples.addAll(createCapacitySamples());

                    return samples;
                }
            }.execute();
        }

        @Override
        public String getCollectorName() {
            return BackupStorageCollector.class.getName();
        }
    }

    @StaticInit
    static void staticInit() {
        PrometheusNamespace.namespacesClasses.put(BackupStorageNamespace.class, BackupStoragePrometheusNamespace.class);
        PrometheusCollector.registerMetricCollector(new BackupStorageCollector());
    }

    public BackupStoragePrometheusNamespace(Namespace namespace) {
        super(namespace);
    }

    @Override
    protected RecordingRule createRecordingRule(Metric metric) {
        RecordingRule r = new RecordingRule(makeSeriesName(metric.getName()));

        // Info metrics use BackupStorageUuid label directly in Prometheus (unified semantic label),
        // no mapping needed. Other metrics use backupStorageUuid label from external sources
        // and need mapping to BackupStorageUuid.
        if (metric instanceof InfoMetric) {
            r.setExpression(makeSeriesName(metric.getName()));
            r.setSkipRecord(true);
            return r;
        }

        if (metric == BackupStorageNamespace.NetworkInBytes) {
            r.setExpression("irate(collectd_interface_if_octets_0{backupStorageUuid!=\"\"}[10m])");
            r.labelMapping("interface", BackupStorageNamespace.LabelNames.NetworkDeviceLetter.toString());
        } else if (metric == BackupStorageNamespace.NetworkAllInBytes) {
            r.setExpression("sum(irate(host_network_all_in_bytes{backupStorageUuid!=\"\"}[10m])) by(backupStorageUuid)");
        } else if (metric == BackupStorageNamespace.NetworkInPackets) {
            r.setExpression("irate(collectd_interface_if_packets_0{backupStorageUuid!=\"\"}[10m])");
            r.labelMapping("interface", BackupStorageNamespace.LabelNames.NetworkDeviceLetter.toString());
        } else if (metric == BackupStorageNamespace.NetworkAllInPackets) {
            r.setExpression("sum(irate(host_network_all_in_packages{backupStorageUuid!=\"\"}[10m])) by(backupStorageUuid)");
        } else if (metric == BackupStorageNamespace.NetworkInErrors) {
            r.setExpression("irate(collectd_interface_if_errors_0{backupStorageUuid!=\"\"}[10m])");
            r.labelMapping("interface", BackupStorageNamespace.LabelNames.NetworkDeviceLetter.toString());
        } else if (metric == BackupStorageNamespace.NetworkAllInErrors) {
            r.setExpression("sum(irate(host_network_all_in_errors{backupStorageUuid!=\"\"}[10m])) by(backupStorageUuid)");
        } else if (metric == BackupStorageNamespace.NetworkOutBytes) {
            r.setExpression("irate(collectd_interface_if_octets_1{backupStorageUuid!=\"\"}[10m])");
            r.labelMapping("interface", BackupStorageNamespace.LabelNames.NetworkDeviceLetter.toString());
        } else if (metric == BackupStorageNamespace.NetworkAllOutBytes) {
            r.setExpression("sum(irate(host_network_all_out_bytes{backupStorageUuid!=\"\"}[10m])) by(backupStorageUuid)");
        } else if (metric == BackupStorageNamespace.NetworkOutPackets) {
            r.setExpression("irate(collectd_interface_if_packets_1{backupStorageUuid!=\"\"}[10m])");
            r.labelMapping("interface", BackupStorageNamespace.LabelNames.NetworkDeviceLetter.toString());
        } else if (metric == BackupStorageNamespace.NetworkAllOutPackets) {
            r.setExpression("sum(irate(host_network_all_out_packages{backupStorageUuid!=\"\"}[10m])) by(backupStorageUuid)");
        } else if (metric == BackupStorageNamespace.NetworkOutErrors) {
            r.setExpression("irate(collectd_interface_if_errors_1{backupStorageUuid!=\"\"}[10m])");
            r.labelMapping("interface", BackupStorageNamespace.LabelNames.NetworkDeviceLetter.toString());
        } else if (metric == BackupStorageNamespace.NetworkAllOutErrors) {
            r.setExpression("sum(irate(host_network_all_out_errors{backupStorageUuid!=\"\"}[10m])) by(backupStorageUuid)");
        } else if (metric == BackupStorageNamespace.CPUIdleUtilization) {
            r.setExpression("collectd_cpu_percent{type=\"idle\", backupStorageUuid!=\"\"}");
            r.labelMapping("cpu", BackupStorageNamespace.LabelNames.CPUNum.toString());
        } else if (metric == BackupStorageNamespace.CPUSystemUtilization) {
            r.setExpression("collectd_cpu_percent{type=\"system\", backupStorageUuid!=\"\"}");
            r.labelMapping("cpu", BackupStorageNamespace.LabelNames.CPUNum.toString());
        } else if (metric == BackupStorageNamespace.CPUUserUtilization) {
            r.setExpression("collectd_cpu_percent{type=\"user\", backupStorageUuid!=\"\"}");
            r.labelMapping("cpu", BackupStorageNamespace.LabelNames.CPUNum.toString());
        } else if (metric == BackupStorageNamespace.CPUWaitUtilization) {
            r.setExpression("collectd_cpu_percent{type=\"wait\", backupStorageUuid!=\"\"}");
            r.labelMapping("cpu", BackupStorageNamespace.LabelNames.CPUNum.toString());
        } else if (metric == BackupStorageNamespace.CPUAllIdleUtilization) {
            r.setExpression("(sum(collectd_cpu_percent{type=\"idle\", backupStorageUuid!=\"\"}) by(backupStorageUuid) / sum(collectd_cpu_percent{backupStorageUuid!=\"\"}) by(backupStorageUuid)) * 100");
        } else if (metric == BackupStorageNamespace.CPUUsedUtilization) {
            // collectd_cpu_percent might return 100.00000000000001
            // in order to avoid negative value use abs here
            r.setExpression("abs(100 - collectd_cpu_percent{type=\"idle\", backupStorageUuid!=\"\"})");
            r.labelMapping("cpu", BackupStorageNamespace.LabelNames.CPUNum.toString());
        } else if (metric == BackupStorageNamespace.CPUAllUsedUtilization) {
            r.setExpression("(sum(100 - collectd_cpu_percent{type=\"idle\", backupStorageUuid!=\"\"}) by(backupStorageUuid) / sum(collectd_cpu_percent{backupStorageUuid!=\"\"}) by(backupStorageUuid)) * 100");
        } else if (metric == BackupStorageNamespace.CPUAverageUsedUtilization) {
            r.setExpression("avg((sum(100 - collectd_cpu_percent{type=\"idle\", backupStorageUuid!=\"\"}) by(backupStorageUuid) / sum(collectd_cpu_percent{backupStorageUuid!=\"\"}) by(backupStorageUuid)) * 100) by (backupStorageUuid)");
        } else if (metric == BackupStorageNamespace.CPUAverageUserUtilization) {
            r.setExpression("avg((sum(collectd_cpu_percent{type=\"user\", backupStorageUuid!=\"\"}) by(backupStorageUuid) / sum(collectd_cpu_percent{backupStorageUuid!=\"\"}) by(backupStorageUuid)) * 100) by (backupStorageUuid)");
        } else if (metric == BackupStorageNamespace.CPUAverageWaitUtilization) {
            r.setExpression("avg((sum(collectd_cpu_percent{type=\"wait\", backupStorageUuid!=\"\"}) by(backupStorageUuid) / sum(collectd_cpu_percent{backupStorageUuid!=\"\"}) by(backupStorageUuid)) * 100) by (backupStorageUuid)");
        } else if (metric == BackupStorageNamespace.CPUAverageSystemUtilization) {
            r.setExpression("avg((sum(collectd_cpu_percent{type=\"system\", backupStorageUuid!=\"\"}) by(backupStorageUuid) / sum(collectd_cpu_percent{backupStorageUuid!=\"\"}) by(backupStorageUuid)) * 100) by (backupStorageUuid)");
        } else if (metric == BackupStorageNamespace.CPUAverageIdleUtilization) {
            r.setExpression("avg((sum(collectd_cpu_percent{type=\"idle\", backupStorageUuid!=\"\"}) by(backupStorageUuid) / sum(collectd_cpu_percent{backupStorageUuid!=\"\"}) by(backupStorageUuid)) * 100) by (backupStorageUuid)");
        } else if (metric == BackupStorageNamespace.DiskReadBytes) {
            r.setExpression("irate(collectd_disk_disk_octets_0{backupStorageUuid!=\"\"}[10m])");
            r.labelMapping("disk", BackupStorageNamespace.LabelNames.DiskDeviceLetter.toString());
        } else if (metric == BackupStorageNamespace.DiskAllReadBytes) {
            r.setExpression("sum(irate(collectd_disk_disk_octets_0{backupStorageUuid!=\"\"}[10m])) by(backupStorageUuid)");
        } else if (metric == BackupStorageNamespace.DiskReadOps) {
            r.setExpression("irate(collectd_disk_disk_ops_0{backupStorageUuid!=\"\"}[10m])");
            r.labelMapping("disk", BackupStorageNamespace.LabelNames.DiskDeviceLetter.toString());
        } else if (metric == BackupStorageNamespace.DiskAllReadOps) {
            r.setExpression("sum(irate(collectd_disk_disk_ops_0{backupStorageUuid!=\"\"}[10m])) by(backupStorageUuid)");
        } else if (metric == BackupStorageNamespace.DiskWriteBytes) {
            r.setExpression("irate(collectd_disk_disk_octets_1{backupStorageUuid!=\"\"}[10m])");
            r.labelMapping("disk", BackupStorageNamespace.LabelNames.DiskDeviceLetter.toString());
        } else if (metric == BackupStorageNamespace.DiskAllWriteBytes) {
            r.setExpression("sum(irate(collectd_disk_disk_octets_1{backupStorageUuid!=\"\"}[10m])) by(backupStorageUuid)");
        } else if (metric == BackupStorageNamespace.DiskWriteOps) {
            r.setExpression("irate(collectd_disk_disk_ops_1{backupStorageUuid!=\"\"}[10m])");
            r.labelMapping("disk", BackupStorageNamespace.LabelNames.DiskDeviceLetter.toString());
        } else if (metric == BackupStorageNamespace.DiskAllWriteOps) {
            r.setExpression("sum(irate(collectd_disk_disk_ops_1{backupStorageUuid!=\"\"}[10m])) by(backupStorageUuid)");
        } else if (metric == BackupStorageNamespace.MemoryFreeBytes) {
            r.setExpression("sum(collectd_memory{memory!=\"used\", backupStorageUuid!=\"\"}) by (backupStorageUuid)");
        } else if (metric == BackupStorageNamespace.MemoryFreeInPercent) {
            r.setExpression("(sum(collectd_memory{memory!=\"used\", backupStorageUuid!=\"\"}) by (backupStorageUuid) / on(backupStorageUuid) (sum(collectd_memory{backupStorageUuid!=\"\"}) by (backupStorageUuid))) * 100");
        } else if (metric == BackupStorageNamespace.MemoryUsedBytes) {
            r.setExpression("collectd_memory{memory=\"used\", backupStorageUuid!=\"\"}");
        } else if (metric == BackupStorageNamespace.MemoryUsedInPercent) {
            r.setExpression("(sum(collectd_memory{memory=\"used\", backupStorageUuid!=\"\"}) by (backupStorageUuid) / on(backupStorageUuid) (sum(collectd_memory{backupStorageUuid!=\"\"}) by (backupStorageUuid))) * 100");

        } else {
            r.setForLabelMappingOnly(true);
            metric.getLabelNames().forEach(l -> r.labelMapping(l, l));
            return r;
        }

        r.labelMapping("backupStorageUuid", BackupStorageNamespace.LabelNames.BackupStorageUuid.toString());
        return r;
    }
}
