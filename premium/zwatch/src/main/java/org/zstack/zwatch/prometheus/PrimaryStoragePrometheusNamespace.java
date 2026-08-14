package org.zstack.zwatch.prometheus;

import io.prometheus.client.Collector;
import io.prometheus.client.GaugeMetricFamily;
import org.apache.commons.lang.StringUtils;
import org.zstack.core.Platform;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQLBatchWithReturn;
import org.zstack.header.core.StaticInit;
import org.zstack.header.storage.primary.PrimaryStorageState;
import org.zstack.header.storage.primary.PrimaryStorageStatus;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.storage.primary.PrimaryStorageVO_;
import org.zstack.header.volume.VolumeType;
import org.zstack.resourceconfig.ResourceConfigFacade;
import org.zstack.storage.primary.PrimaryStorageGlobalConfig;
import org.zstack.storage.primary.PrimaryStorageUsageReport;
import org.zstack.zwatch.datatype.metric.InfoMetric;
import org.zstack.zwatch.datatype.metric.Metric;
import org.zstack.zwatch.datatype.Namespace;
import org.zstack.zwatch.namespace.PrimaryStorageNamespace;

import javax.persistence.Tuple;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static java.util.Arrays.asList;

public class PrimaryStoragePrometheusNamespace extends AbstractPrometheusNamespace {
    static PrimaryStorageUsageReport primaryStorageUsageReport;
    static ResourceConfigFacade rcf;

    public static class PrimaryStorageCollector extends BasicMetricCollector {
        @Override
        protected String getNamespaceName() {
            return PrimaryStorageNamespace.NAME;
        }

        @Override
        public List<Collector.MetricFamilySamples> collect() {
            return new SQLBatchWithReturn<List<Collector.MetricFamilySamples>>() {
                long allCapacity;
                long allAvailCapacity;
                long allUsedCapacity;
                long allLockedCapacity;

                private List<Collector.MetricFamilySamples> createCapacitySamples() {
                    List<Collector.MetricFamilySamples> samples = new ArrayList<>();
                    List<Tuple> ts = sql("select ps.uuid, ps.type, cap.totalCapacity, cap.availableCapacity," +
                            "cap.totalPhysicalCapacity, cap.availablePhysicalCapacity, ps.status, ps.state, ps.name from PrimaryStorageCapacityVO cap," +
                            "PrimaryStorageVO ps where ps.uuid = cap.uuid and ps.status != :status", Tuple.class)
                            .param("status", PrimaryStorageStatus.Connecting)
                            .list();

                    GaugeMetricFamily AvailableCapacityInBytes = createMetric(PrimaryStorageNamespace.AvailableCapacityInBytes);
                    samples.add(AvailableCapacityInBytes);
                    GaugeMetricFamily AvailableCapacityInPercent = createMetric(PrimaryStorageNamespace.AvailableCapacityInPercent);
                    samples.add(AvailableCapacityInPercent);
                    GaugeMetricFamily UsedCapacityInBytes = createMetric(PrimaryStorageNamespace.UsedCapacityInBytes);
                    samples.add(UsedCapacityInBytes);
                    GaugeMetricFamily UsedCapacityInPercent = createMetric(PrimaryStorageNamespace.UsedCapacityInPercent);
                    samples.add(UsedCapacityInPercent);
                    GaugeMetricFamily AvailablePhysicalCapacityInBytes = createMetric(PrimaryStorageNamespace.AvailablePhysicalCapacityInBytes);
                    samples.add(AvailablePhysicalCapacityInBytes);
                    GaugeMetricFamily AvailablePhysicalCapacityInPercent = createMetric(PrimaryStorageNamespace.AvailablePhysicalCapacityInPercent);
                    samples.add(AvailablePhysicalCapacityInPercent);
                    GaugeMetricFamily UsedPhysicalCapacityInBytes = createMetric(PrimaryStorageNamespace.UsedPhysicalCapacityInBytes);
                    samples.add(UsedPhysicalCapacityInBytes);
                    GaugeMetricFamily UsedPhysicalCapacityInPercent = createMetric(PrimaryStorageNamespace.UsedPhysicalCapacityInPercent);
                    samples.add(UsedPhysicalCapacityInPercent);
                    GaugeMetricFamily TotalPhysicalCapacityInBytes = createMetric(PrimaryStorageNamespace.TotalPhysicalCapacityInBytes);
                    samples.add(TotalPhysicalCapacityInBytes);
                    GaugeMetricFamily TimeDurationRequiredForPrimaryStorageForecastUsageExceedingThresholdUsage =
                            createMetric(PrimaryStorageNamespace.TimeDurationRequiredForPrimaryStorageForecastUsageExceedingThresholdUsage);
                    samples.add(TimeDurationRequiredForPrimaryStorageForecastUsageExceedingThresholdUsage);

                    ts.forEach(t -> {
                        String psUuid = t.get(0, String.class);
                        String type = t.get(1, String.class);
                        double totalCapacity = t.get(2, Long.class) != null ? t.get(2, Long.class).doubleValue() : 0.0;
                        double availableCapacity = t.get(3, Long.class) != null ? t.get(3, Long.class).doubleValue() : 0.0;
                        double totalPhysicalCapacity = t.get(4, Long.class) != null ? t.get(4, Long.class).doubleValue() : 0.0;
                        double availablePhysicalCapacity = t.get(5, Long.class) != null ? t.get(5, Long.class).doubleValue() : 0.0;
                        PrimaryStorageStatus status = t.get(6, PrimaryStorageStatus.class);
                        PrimaryStorageState state = t.get(7, PrimaryStorageState.class);
                        String psName = StringUtils.defaultString(t.get(8, String.class));

                        allCapacity += totalCapacity;
                        if (status == PrimaryStorageStatus.Connected && state == PrimaryStorageState.Enabled) {
                            allAvailCapacity += availableCapacity;
                        } else {
                            allLockedCapacity += availableCapacity;
                        }
                        allUsedCapacity += totalCapacity - availableCapacity;

                        List<String> labels = asList(psUuid, type, psName);
                        AvailableCapacityInBytes.addMetric(labels, availableCapacity);
                        UsedCapacityInBytes.addMetric(labels, totalCapacity - availableCapacity);
                        if (totalCapacity > 0) {
                            AvailableCapacityInPercent.addMetric(labels, (availableCapacity / totalCapacity) * 100);
                            UsedCapacityInPercent.addMetric(labels, ((totalCapacity - availableCapacity) / totalCapacity) * 100);
                        } else if (totalCapacity == 0) {
                            AvailableCapacityInPercent.addMetric(labels, 0);
                            UsedCapacityInPercent.addMetric(labels, 0);
                        }

                        AvailablePhysicalCapacityInBytes.addMetric(labels, availablePhysicalCapacity);
                        UsedPhysicalCapacityInBytes.addMetric(labels, totalPhysicalCapacity - availablePhysicalCapacity);
                        TotalPhysicalCapacityInBytes.addMetric(labels, totalPhysicalCapacity);
                        if (totalPhysicalCapacity > 0) {
                            AvailablePhysicalCapacityInPercent.addMetric(labels, (availablePhysicalCapacity / totalPhysicalCapacity) * 100);
                            UsedPhysicalCapacityInPercent.addMetric(labels, ((totalPhysicalCapacity - availablePhysicalCapacity) / totalPhysicalCapacity) * 100);
                        } else if (totalPhysicalCapacity == 0) {
                            AvailablePhysicalCapacityInPercent.addMetric(labels, 0);
                            UsedPhysicalCapacityInPercent.addMetric(labels, 0);
                        }

                        setTimeDurationForecastUsageExceedingThreshold(
                                TimeDurationRequiredForPrimaryStorageForecastUsageExceedingThresholdUsage, psUuid, type, psName);
                    });

                    samples.add(createMetric(PrimaryStorageNamespace.TotalCapacityInBytes, allCapacity));
                    samples.add(createMetric(PrimaryStorageNamespace.TotalAvailableCapacityInBytes, allAvailCapacity));

                    if (allCapacity != 0) {
                        samples.add(createMetric(PrimaryStorageNamespace.TotalAvailableCapacityInPercent, ((double) allAvailCapacity / allCapacity) * 100));
                        samples.add(createMetric(PrimaryStorageNamespace.TotalUsedCapacityInPercent,  ((double) allUsedCapacity / allCapacity) * 100));
                        samples.add(createMetric(PrimaryStorageNamespace.TotalLockedCapacityInPercent, ((double) allLockedCapacity / allCapacity) * 100));
                    }

                    samples.add(createMetric(PrimaryStorageNamespace.TotalUsedCapacityInBytes, allUsedCapacity));
                    samples.add(createMetric(PrimaryStorageNamespace.TotalLockedCapacityInBytes, allLockedCapacity));

                    return samples;
                }

                private void setTimeDurationForecastUsageExceedingThreshold(
                        GaugeMetricFamily metric, String psUuid, String type, String psName) {
                    if (primaryStorageUsageReport == null) {
                        primaryStorageUsageReport = Platform.getComponentLoader().getComponent(PrimaryStorageUsageReport.class);
                    }
                    if (rcf == null) {
                        rcf = Platform.getComponentLoader().getComponent(ResourceConfigFacade.class);
                    }

                    List<Double> percents = primaryStorageUsageReport.getFutureForecastsInPercent(psUuid);
                    if (percents.isEmpty()) {
                        return;
                    }

                    Double threshold = rcf.getResourceConfigValue(PrimaryStorageGlobalConfig.
                            PRIMARY_STORAGE_USED_PHYSICAL_CAPACITY_FORECAST_THRESHOLD, psUuid, Double.class);
                    long time = IntStream.range(1, percents.size() + 1)
                            .filter(i -> percents.get(i - 1) >= threshold).findFirst().orElse(Integer.MAX_VALUE);

                    List<String> labels = asList(psUuid, type, psName);
                    metric.addMetric(labels, time);
                }


                @Override
                protected List<Collector.MetricFamilySamples> scripts() {
                    List<Collector.MetricFamilySamples> samples = new ArrayList<>();
                    samples.addAll(createPrimaryStorageInfoMetric());
                    samples.addAll(createCapacitySamples());
                    samples.addAll(createBitsSamples());

                    return samples;
                }

                private List<Collector.MetricFamilySamples> createPrimaryStorageInfoMetric() {
                    List<Collector.MetricFamilySamples> samples = new ArrayList<>();
                    GaugeMetricFamily primaryStorageInfoMetric = createMetric(PrimaryStorageNamespace.PrimaryStorageInfo);

                    List<Tuple> rows = Q.New(PrimaryStorageVO.class)
                                    .select(PrimaryStorageVO_.uuid, PrimaryStorageVO_.name)
                                    .listTuple();

                    rows.forEach(row -> {
                        String uuid = row.get(0, String.class);
                        String name = row.get(1, String.class);
                        primaryStorageInfoMetric.addMetric(asList(
                                StringUtils.defaultString(uuid),
                                StringUtils.defaultString(name)
                        ), 1D);
                    });

                    samples.add(primaryStorageInfoMetric);
                    return samples;
                }

                private List<Collector.MetricFamilySamples> createBitsSamples() {
                    List<Collector.MetricFamilySamples> samples = new ArrayList<>();

                    GaugeMetricFamily RootVolumeCount = createMetric(PrimaryStorageNamespace.RootVolumeCount);
                    samples.add(RootVolumeCount);
                    GaugeMetricFamily DataVolumeCount = createMetric(PrimaryStorageNamespace.DataVolumeCount);
                    samples.add(DataVolumeCount);
                    GaugeMetricFamily SnapshotCount = createMetric(PrimaryStorageNamespace.SnapshotCount);
                    samples.add(SnapshotCount);

                    List<Tuple> ts = sql("select ps.uuid, ps.type, ps.name from PrimaryStorageVO ps", Tuple.class).list();
                    ts.forEach(t -> {
                        String psUuid = t.get(0, String.class);
                        String type = t.get(1, String.class);
                        String psName = StringUtils.defaultString(t.get(2, String.class));

                        List<String> labels = asList(psUuid, type, psName);
                        Long count = sql("select count(v) from VolumeVO v where v.type = :vtype and v.primaryStorageUuid = :psUuid", Long.class)
                                .param("vtype", VolumeType.Root).param("psUuid", psUuid).find();
                        RootVolumeCount.addMetric(labels, count.doubleValue());

                        count = sql("select count(v) from VolumeVO v where v.type = :vtype and v.primaryStorageUuid = :psUuid", Long.class)
                                .param("vtype", VolumeType.Data).param("psUuid", psUuid).find();
                        DataVolumeCount.addMetric(labels, count.doubleValue());

                        count = sql("select count(s) from VolumeSnapshotVO s where s.primaryStorageUuid = :psUuid", Long.class)
                                .param("psUuid", psUuid).find();
                        SnapshotCount.addMetric(labels, count.doubleValue());
                    });

                    return samples;
                }
            }.execute();
        }

        @Override
        public String getCollectorName() {
            return PrimaryStorageCollector.class.getName();
        }
    }

    @StaticInit
    static void staticInit() {
        PrometheusNamespace.namespacesClasses.put(PrimaryStorageNamespace.class, PrimaryStoragePrometheusNamespace.class);
        PrometheusCollector.registerMetricCollector(new PrimaryStorageCollector());
    }

    public PrimaryStoragePrometheusNamespace(Namespace namespace) {
        super(namespace);
    }

    @Override
    protected RecordingRule createRecordingRule(Metric metric) {
        // no mapping needed. Other metrics may use labels from external sources that need mapping.
        if (metric instanceof InfoMetric) {
            RecordingRule r = new RecordingRule(makeSeriesName(metric.getName()));
            r.setExpression(makeSeriesName(metric.getName()));
            r.setSkipRecord(true);
            return r;
        }

        RecordingRule r = new RecordingRule(makeSeriesName(metric.getName()));
        r.setExpression(makeSeriesName(metric.getName()));
        metric.getLabelNames().forEach(l->r.labelMapping(l, l));
        return r;
    }
}
