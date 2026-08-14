package org.zstack.zwatch.prometheus;

import io.prometheus.client.Collector;
import io.prometheus.client.CounterMetricFamily;
import io.prometheus.client.GaugeMetricFamily;
import org.zstack.core.Platform;
import org.zstack.header.core.StaticInit;
import org.zstack.premium.externalservice.prometheus.Prometheus;
import org.zstack.utils.CollectionDSL;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.ProcessFinder;
import org.zstack.utils.path.PathUtil;
import org.zstack.zwatch.datatype.metric.Metric;
import org.zstack.zwatch.datatype.Namespace;
import org.zstack.zwatch.namespace.MonitorNamespace;

import java.util.*;

/**
 * Created by mingjian.deng on 2020/4/7.
 */
public class MonitorPrometheusNamespace extends AbstractPrometheusNamespace {
    public MonitorPrometheusNamespace(Namespace namespace) {
        super(namespace);
    }

    private static String mgmtIp;
    private static long loops;

    private static Map<String, Map<String, Double>> prometheusQueries = new HashMap<>();

    @StaticInit
    static void staticInit () {
        PrometheusNamespace.namespacesClasses.put(MonitorNamespace.class, MonitorPrometheusNamespace.class);
        PrometheusCollector.registerMetricCollector(new MonitorCollector());
    }

    private static Map<String, String> metricMap = new HashMap<>();

    static {
        metricMap.put(MonitorNamespace.PrometheusLasts.getName(), "prometheus_lasts");
        metricMap.put(MonitorNamespace.PrometheusSocketNum.getName(), "prometheus_socket_num");
        metricMap.put(MonitorNamespace.PrometheusExpends.getName(), "prometheus_resource_expends");
        metricMap.put(MonitorNamespace.PrometheusDiskSpace.getName(), "prometheus_disk_space");
        metricMap.put(MonitorNamespace.PrometheusQueries.getName(), "prometheus_queries");
    }

    private static CounterMetricFamily PrometheusDiskSpace = null;

    @Override
    protected RecordingRule createRecordingRule(Metric metric) {
        RecordingRule rule = new RecordingRule(makeSeriesName(metric.getName()));
        if (metricMap.get(metric.getName()) != null) {
            rule.setExpression(metricMap.get(metric.getName()));
        } else if (metric == MonitorNamespace.PrometheusDeltaQueries) {
            rule.setExpression("rate(prometheus_queries[1m])");
        }

        rule.setSeriesName(makeSeriesName(metric.getName()));
        return rule;
    }

    public static class MonitorCollector implements MetricCollector {
        @Override
        public boolean skipManagementNodeCheck() {
            return true;
        }

        @Override
        public List<Collector.MetricFamilySamples> collect() {
            loops += 1;
            List<Collector.MetricFamilySamples> samples = new ArrayList<>(prometheusStatusMetricsCollect());

            if (loops >= 10000) {
                loops = 0;
            }
            return samples;
        }

        @Override
        public String getCollectorName() {
            return MonitorCollector.class.getName();
        }

        private List<Collector.MetricFamilySamples> prometheusStatusMetricsCollect() {
            List<Collector.MetricFamilySamples> samples = new ArrayList<>();
            if (mgmtIp == null) {
                mgmtIp = Platform.getManagementServerIp();
            }

            samples.addAll(getPrometheusStatus());
            samples.addAll(getPrometheusDiskspace());
            samples.addAll(getPrometheusQueries());

            return samples;

        }

        private List<Collector.MetricFamilySamples> getPrometheusQueries() {
            List<Collector.MetricFamilySamples> samples = new ArrayList<>();
            CounterMetricFamily PrometheusQueries = createCounterMetric(MonitorNamespace.PrometheusQueries);
            prometheusQueries.forEach((k, v) -> {
                v.forEach((type, count) -> {
                    PrometheusQueries.addMetric(CollectionDSL.list(mgmtIp, k, type), count);
                });
            });

            samples.add(PrometheusQueries);

            return samples;
        }

        private List<Collector.MetricFamilySamples> getPrometheusDiskspace() {
            List<Collector.MetricFamilySamples> samples = new ArrayList<>();
            boolean needUpdate = (PrometheusDiskSpace == null || loops % 30 == 0);

            if (needUpdate) {
                PrometheusDiskSpace = createCounterMetric(MonitorNamespace.PrometheusDiskSpace);
                Double dataSpace;
                Double walSpace;
                dataSpace = ProgressMonitorHelper.getDiskSpaceMB(Prometheus.DATA_DIR_2);
                walSpace = ProgressMonitorHelper.getDiskSpaceMB(PathUtil.join(Prometheus.DATA_DIR_2, "wal"));
                PrometheusDiskSpace.addMetric(CollectionDSL.list(mgmtIp, "total"), dataSpace);
                PrometheusDiskSpace.addMetric(CollectionDSL.list(mgmtIp, "wal"), walSpace);
            }

            samples.add(PrometheusDiskSpace);

            return samples;
        }

        private List<Collector.MetricFamilySamples> getPrometheusStatus() {
            List<Collector.MetricFamilySamples> samples = new ArrayList<>();
            CounterMetricFamily PrometheusLasts = createCounterMetric(MonitorNamespace.PrometheusLasts);
            CounterMetricFamily PrometheusSocketNum = createCounterMetric(MonitorNamespace.PrometheusSocketNum);
            CounterMetricFamily PrometheusExpends = createCounterMetric(MonitorNamespace.PrometheusExpends);

            int pid = getPrometheusPID();
            ProgressStatus status = ProgressMonitorHelper.getProgressStatus(String.valueOf(pid));
            PrometheusLasts.addMetric(Collections.singletonList(mgmtIp), status.lasts);
            PrometheusSocketNum.addMetric(Collections.singletonList(mgmtIp), status.sockets);
            PrometheusExpends.addMetric(Arrays.asList(mgmtIp, "cpu"), status.cpuExpends);
            PrometheusExpends.addMetric(Arrays.asList(mgmtIp, "memrss"), status.memoryExpends);


            samples.add(PrometheusLasts);
            samples.add(PrometheusSocketNum);
            samples.add(PrometheusExpends);


            return samples;
        }

        private CounterMetricFamily createCounterMetric(Metric m) {
            DebugUtils.Assert(metricMap.get(m.getName()) != null, String.format("cannot find this metrics: %s", m.getName()));
            return new CounterMetricFamily(metricMap.get(m.getName()), String.format("help for %s", m.getName()), m.getLabelNames());
        }

        private GaugeMetricFamily createGaugeMetric(Metric m) {
            DebugUtils.Assert(metricMap.get(m.getName()) != null, String.format("cannot find this metrics: %s", m.getName()));
            return new GaugeMetricFamily(metricMap.get(m.getName()), String.format("help for %s", m.getName()), m.getLabelNames());
        }

        private int getPrometheusPID() {
            return new ProcessFinder().findByCommandLineKeywords("prometheus2", "zstack", "--config.file", "storage.tsdb.path");
        }
    }

    public static void beforePrometheusApicall(String ip, String url) {
        prometheusQueries.compute(ip, (k, v) ->
                v == null ? addPrometheusQuery(url, new HashMap<>()) : addPrometheusQuery(url, v)
        );
    }

    private static Map<String, Double> addPrometheusQuery(String url, Map<String, Double> old) {
        String type = null;
        if (url.contains(Prometheus.HTTP_RANGE_API_PATH)) {
            type = "range_query";
        } else if (url.contains(Prometheus.HTTP_REMOTE_READ_PATH)) {
            type = "remote_query";
        } else if (url.contains(Prometheus.HTTP_API_PATH)) {
            type = "query";
        }
        if (type != null) {
            old.compute(type, (k,v) -> v == null ? 1 : ++v);
        }
        return old;
    }
}
