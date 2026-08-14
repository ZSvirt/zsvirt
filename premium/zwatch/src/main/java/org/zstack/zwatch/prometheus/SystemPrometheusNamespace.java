package org.zstack.zwatch.prometheus;

import io.prometheus.client.Collector;
import io.prometheus.client.GaugeMetricFamily;
import org.apache.commons.lang.StringUtils;
import org.zstack.core.Platform;
import org.zstack.header.core.StaticInit;
import org.zstack.utils.Bash;
import org.zstack.zwatch.ZWatchGlobalConfig;
import org.zstack.zwatch.datatype.metric.Metric;
import org.zstack.zwatch.datatype.Namespace;
import org.zstack.zwatch.namespace.SystemNamespace;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;

public class SystemPrometheusNamespace extends AbstractPrometheusNamespace  {
    public SystemPrometheusNamespace(Namespace namespace) {
        super(namespace);
    }

    public static class SystemCollector implements MetricCollector {
        private String seriesName(String metricName) {
            return PrometheusNamespace.makeSeriesName(Namespace.zstackNamespaceName(SystemNamespace.NAME), metricName);
        }

        @Override
        public boolean skipManagementNodeCheck() {
            return false;
        }

        @Override
        public List<Collector.MetricFamilySamples> collect() {
            List<Collector.MetricFamilySamples> samples = new ArrayList<>();
            samples.addAll(managementServerDirsMetrics());
            return samples;
        }

        @Override
        public String getCollectorName() {
            return SystemCollector.class.getName();
        }

        private GaugeMetricFamily createMetric(Metric m) {
            return new GaugeMetricFamily(seriesName(m.getName()), String.format("help for %s", m.getName()), m.getLabelNames());
        }

        private List<Collector.MetricFamilySamples> managementServerDirsMetrics() {
            List<Collector.MetricFamilySamples> samples = new ArrayList<>();

            List<String> directories = new ArrayList<>();
            Collections.addAll(directories, ZWatchGlobalConfig.MANAGEMENT_NODE_DIR_TO_MONITOR.value().split(","));
            if (directories.isEmpty()) {
                return samples;
            }

            new Bash() {
                @Override
                protected void scripts() {
                    List<String> dirs = directories.stream().map(String::trim).collect(Collectors.toList());
                    dirs = dirs.stream().filter(d -> new File(d).exists()).collect(Collectors.toList());
                    setE();
                    sudoRun("df -B 1 %s | tail -%s", StringUtils.join(dirs, " "), dirs.size());
                    List<String> lines = new ArrayList<>();
                    Collections.addAll(lines, stdout().split("\n"));

                    GaugeMetricFamily DataDirFreeCapacityInBytes = createMetric(SystemNamespace.ManagementServerDirFreeCapacityInBytes);
                    samples.add(DataDirFreeCapacityInBytes);
                    GaugeMetricFamily DataDirFreeCapacityInPercent = createMetric(SystemNamespace.ManagementServerDirFreeCapacityInPercent);
                    samples.add(DataDirFreeCapacityInPercent);
                    GaugeMetricFamily DataDirUsedCapacityInBytes = createMetric(SystemNamespace.ManagementServerDirUsedCapacityInBytes);
                    samples.add(DataDirUsedCapacityInBytes);
                    GaugeMetricFamily DataDirUsedCapacityInPercent = createMetric(SystemNamespace.ManagementServerDirUsedCapacityInPercent);
                    samples.add(DataDirUsedCapacityInPercent);

                    for (int i=0; i<lines.size(); i++) {
                        String l = lines.get(i).trim();
                        if (l.isEmpty()) {
                            continue;
                        }

                        String[] values = l.split(" +");
                        String t = values[1].trim();
                        String u = values[2].trim();
                        String f = values[3].trim();

                        Double total = new Double(t);
                        Double used = new Double(u);
                        Double free = new Double(f);

                        String mgmtIp = Platform.getManagementServerIp();
                        String dir = dirs.get(i);

                        DataDirFreeCapacityInBytes.addMetric(asList(mgmtIp, dir), free);
                        DataDirFreeCapacityInPercent.addMetric(asList(mgmtIp, dir), (free / total) * 100);
                        DataDirUsedCapacityInBytes.addMetric(asList(mgmtIp, dir), used);
                        DataDirUsedCapacityInPercent.addMetric(asList(mgmtIp, dir), (used / total) * 100);
                    }
                }
            }.execute();

            return samples;
        }
    }

    @StaticInit
    static void staticInit() {
        PrometheusNamespace.namespacesClasses.put(SystemNamespace.class, SystemPrometheusNamespace.class);
        PrometheusCollector.registerMetricCollector(new SystemCollector());
    }

    @Override
    protected RecordingRule createRecordingRule(Metric metric) {
        RecordingRule rule = new RecordingRule(makeSeriesName(metric.getName()));
        rule.setForLabelMappingOnly(true);
        metric.getLabelNames().forEach(l->rule.labelMapping(l, l));
        return rule;
    }
}
