package org.zstack.zwatch.prometheus;

import io.prometheus.client.Collector;
import io.prometheus.client.GaugeMetricFamily;
import org.zstack.core.Platform;
import org.zstack.core.db.SQLBatchWithReturn;
import org.zstack.header.core.StaticInit;
import org.zstack.header.storage.primary.PrimaryStorageStatus;
import org.zstack.resourceconfig.ResourceConfigFacade;
import org.zstack.storage.ceph.primary.PoolUsageReport;
import org.zstack.storage.primary.PrimaryStorageGlobalConfig;
import org.zstack.zwatch.datatype.metric.Metric;
import org.zstack.zwatch.datatype.Namespace;
import org.zstack.zwatch.namespace.CephPrimaryStoragePoolNamespace;
import org.zstack.zwatch.namespace.PrimaryStorageNamespace;

import javax.persistence.Tuple;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static java.util.Arrays.asList;
import static org.zstack.zwatch.prometheus.PrimaryStoragePrometheusNamespace.rcf;

public class CephPrimaryStoragePoolPrometheusNamespace {
    static PoolUsageReport poolUsageForecaster;

    public static class CephPrimaryStoragePoolCollector implements MetricCollector {
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

                private List<Collector.MetricFamilySamples> createCephPoolCapacitySamples() {
                    List<Collector.MetricFamilySamples> samples = new ArrayList<>();
                    List<Tuple> ts = sql("select ps.uuid, pool.poolName, pool.uuid, pool.totalCapacity, " +
                            "pool.usedCapacity, osd.availableCapacity, osd.uuid " +
                            "from CephPrimaryStoragePoolVO pool " +
                            "LEFT JOIN PrimaryStorageVO ps " +
                            "ON (pool.primaryStorageUuid = ps.uuid and ps.status != :status) " +
                            "LEFT JOIN CephOsdGroupVO osd " +
                            "ON pool.osdGroup.uuid = osd.uuid", Tuple.class)
                            .param("status", PrimaryStorageStatus.Connecting)
                            .list();

                    GaugeMetricFamily PoolAvailableCapacityInPercent = createMetric(CephPrimaryStoragePoolNamespace.PoolAvailableCapacityInPercent);
                    samples.add(PoolAvailableCapacityInPercent);
                    GaugeMetricFamily PoolUsedCapacityInPercent = createMetric(CephPrimaryStoragePoolNamespace.PoolUsedCapacityInPercent);
                    samples.add(PoolUsedCapacityInPercent);
                    GaugeMetricFamily PoolVirtualAvailableCapacityInPercent = createMetric(CephPrimaryStoragePoolNamespace.PoolVirtualAvailableCapacityInPercent);
                    samples.add(PoolVirtualAvailableCapacityInPercent);
                    GaugeMetricFamily TimeDurationRequiredForCephPoolForecastUsageExceedingThresholdUsage =
                            createMetric(CephPrimaryStoragePoolNamespace.TimeDurationRequiredForCephPoolForecastUsageExceedingThresholdUsage);
                    samples.add(TimeDurationRequiredForCephPoolForecastUsageExceedingThresholdUsage);

                    ts.forEach(t -> {
                        String psUuid = t.get(0, String.class);
                        String poolName = t.get(1, String.class);
                        String poolUuid = t.get(2, String.class);
                        double totalCapacity = t.get(3, Long.class) != null ? t.get(3, Long.class).doubleValue() : 0.0;
                        double usedPhysicalCapacity = t.get(4, Long.class) != null ? t.get(4, Long.class).doubleValue() : 0.0;
                        double virtualAvailableCapacity = t.get(5, Long.class) != null ? t.get(5, Long.class).doubleValue() : 0.0;
                        String osdGroupUuid = t.get(6, String.class);
                        if (psUuid == null) {
                            return;
                        }

                        if (totalCapacity > 0) {
                            PoolAvailableCapacityInPercent.addMetric(asList(psUuid, poolName, poolUuid), ((totalCapacity - usedPhysicalCapacity) / totalCapacity) * 100);
                            PoolUsedCapacityInPercent.addMetric(asList(psUuid, poolName, poolUuid), (usedPhysicalCapacity / totalCapacity) * 100);
                            PoolVirtualAvailableCapacityInPercent.addMetric(asList(psUuid, poolName, poolUuid), (virtualAvailableCapacity / totalCapacity) * 100);
                        } else if (totalCapacity == 0) {
                            PoolAvailableCapacityInPercent.addMetric(asList(psUuid, poolName, poolUuid), 0);
                            PoolUsedCapacityInPercent.addMetric(asList(psUuid, poolName, poolUuid), 0);
                            PoolVirtualAvailableCapacityInPercent.addMetric(asList(psUuid, poolName, poolUuid), 0);
                        }
                        setTimeDurationForecastUsageExceedingThreshold(
                                TimeDurationRequiredForCephPoolForecastUsageExceedingThresholdUsage, osdGroupUuid, psUuid, poolUuid, poolName);
                    });
                    return samples;
                }

                private void setTimeDurationForecastUsageExceedingThreshold(
                        GaugeMetricFamily metric, String osdGroupUuid, String psUuid, String poolUuid, String poolName) {
                    if (poolUsageForecaster == null) {
                        poolUsageForecaster = Platform.getComponentLoader().getComponent(PoolUsageReport.class);
                    }
                    if (rcf == null) {
                        rcf = Platform.getComponentLoader().getComponent(ResourceConfigFacade.class);
                    }

                    List<Double> percents = poolUsageForecaster.getFutureForecastsInPercent(osdGroupUuid);
                    if (percents.isEmpty()) {
                        return;
                    }

                    Double threshold = rcf.getResourceConfigValue(PrimaryStorageGlobalConfig.
                            PRIMARY_STORAGE_USED_PHYSICAL_CAPACITY_FORECAST_THRESHOLD, psUuid, Double.class);
                    long time = IntStream.range(1, percents.size() + 1)
                            .filter(i -> percents.get(i - 1) >= threshold).findFirst().orElse(Integer.MAX_VALUE);

                    metric.addMetric(asList(psUuid, poolName, poolUuid), time);
                }

                @Override
                protected List<Collector.MetricFamilySamples> scripts() {
                    List<Collector.MetricFamilySamples> samples = new ArrayList<>();
                    samples.addAll(createCephPoolCapacitySamples());
                    return samples;
                }
            }.execute();
        }

        @Override
        public String getCollectorName() {
            return CephPrimaryStoragePoolCollector.class.getName();
        }
    }

    @StaticInit
    static void staticInit() {
        PrometheusCollector.registerMetricCollector(new CephPrimaryStoragePoolCollector());
    }
}
