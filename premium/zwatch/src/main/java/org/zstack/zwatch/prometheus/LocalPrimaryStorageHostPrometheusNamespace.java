package org.zstack.zwatch.prometheus;

import io.prometheus.client.Collector;
import io.prometheus.client.GaugeMetricFamily;
import org.zstack.core.Platform;
import org.zstack.core.db.SQLBatchWithReturn;
import org.zstack.header.core.StaticInit;
import org.zstack.header.storage.primary.PrimaryStorageStatus;
import org.zstack.resourceconfig.ResourceConfigFacade;
import org.zstack.storage.primary.PrimaryStorageGlobalConfig;
import org.zstack.storage.primary.local.LocalStorageHostUsageReport;
import org.zstack.zwatch.datatype.Namespace;
import org.zstack.zwatch.datatype.metric.Metric;
import org.zstack.zwatch.namespace.LocalPrimaryStorageHostNamespace;
import org.zstack.zwatch.namespace.PrimaryStorageNamespace;

import javax.persistence.Tuple;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static java.util.Arrays.asList;
import static org.zstack.zwatch.prometheus.PrimaryStoragePrometheusNamespace.rcf;

public class LocalPrimaryStorageHostPrometheusNamespace {
    static LocalStorageHostUsageReport localStorageHostUsageReport;

    public static class LocalPrimaryStorageHostCollector implements MetricCollector {
        private String seriesName(String metricName) {
            return PrometheusNamespace.makeSeriesName(Namespace.zstackNamespaceName(PrimaryStorageNamespace.NAME), metricName);
        }

        @Override
        public boolean skipManagementNodeCheck() {
            return false;
        }

        @Override
        public List<Collector.MetricFamilySamples> collect() {
            return new SQLBatchWithReturn<List<Collector.MetricFamilySamples>>() {
                private GaugeMetricFamily createMetric(Metric m) {
                    return new GaugeMetricFamily(seriesName(m.getName()), String.format("help for %s", m.getName()), m.getLabelNames());
                }

                private List<Collector.MetricFamilySamples> createLocalHostCapacitySamples() {
                    List<Collector.MetricFamilySamples> samples = new ArrayList<>();
                    List<Tuple> ts = sql("select ref.primaryStorageUuid, ref.hostUuid " +
                            "from LocalStorageHostRefVO ref, PrimaryStorageVO ps " +
                            "where ps.status != :status", Tuple.class)
                            .param("status", PrimaryStorageStatus.Connecting).list();

                    GaugeMetricFamily TimeDurationRequiredForLocalStorageHostForecastUsageExceedingThresholdUsage =
                            createMetric(LocalPrimaryStorageHostNamespace.TimeDurationRequiredForLocalStorageHostForecastUsageExceedingThresholdUsage);
                    samples.add(TimeDurationRequiredForLocalStorageHostForecastUsageExceedingThresholdUsage);

                    ts.forEach(t -> {
                        String psUuid = t.get(0, String.class);
                        String hostUuid = t.get(1, String.class);
                        if (psUuid == null) {
                            return;
                        }

                        setTimeDurationForecastUsageExceedingThreshold(
                                TimeDurationRequiredForLocalStorageHostForecastUsageExceedingThresholdUsage,hostUuid, psUuid);

                    });
                    return samples;
                }

                private void setTimeDurationForecastUsageExceedingThreshold(
                        GaugeMetricFamily metric, String hostUuid, String psUuid) {
                    if (localStorageHostUsageReport == null) {
                        localStorageHostUsageReport = Platform.getComponentLoader().getComponent(LocalStorageHostUsageReport.class);
                    }
                    if (rcf == null) {
                        rcf = Platform.getComponentLoader().getComponent(ResourceConfigFacade.class);
                    }

                    List<Double> percents = localStorageHostUsageReport.getFutureForecastsInPercent(hostUuid);
                    if (percents.isEmpty()) {
                        return;
                    }

                    Double threshold = rcf.getResourceConfigValue(PrimaryStorageGlobalConfig
                            .PRIMARY_STORAGE_USED_PHYSICAL_CAPACITY_FORECAST_THRESHOLD, psUuid, Double.class);
                    long time = IntStream.range(1, percents.size() + 1)
                            .filter(i -> percents.get(i - 1) >= threshold).findFirst().orElse(Integer.MAX_VALUE);

                    metric.addMetric(asList(psUuid, hostUuid), time);
                }

                @Override
                protected List<Collector.MetricFamilySamples> scripts() {
                    List<Collector.MetricFamilySamples> samples = new ArrayList<>();
                    samples.addAll(createLocalHostCapacitySamples());
                    return samples;
                }
            }.execute();
        }

        @Override
        public String getCollectorName() {
            return LocalPrimaryStorageHostCollector.class.getName();
        }
    }

    @StaticInit
    static void staticInit() {
        PrometheusCollector.registerMetricCollector(new LocalPrimaryStorageHostCollector());
    }
}
