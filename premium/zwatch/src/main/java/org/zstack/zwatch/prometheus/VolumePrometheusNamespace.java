package org.zstack.zwatch.prometheus;

import io.prometheus.client.Collector;
import io.prometheus.client.GaugeMetricFamily;
import org.zstack.compute.host.HostXfsFragReader;
import org.zstack.core.db.SQLBatchWithReturn;
import org.zstack.header.core.StaticInit;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO_;
import org.zstack.header.volume.*;
import org.zstack.zwatch.datatype.metric.Metric;
import org.zstack.zwatch.datatype.Namespace;
import org.zstack.zwatch.namespace.VolumeNamespace;

import javax.persistence.Tuple;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;

public class VolumePrometheusNamespace extends AbstractPrometheusNamespace {
    @StaticInit
    static void staticInit() {
        PrometheusNamespace.namespacesClasses.put(VolumeNamespace.class, VolumePrometheusNamespace.class);
        PrometheusCollector.registerMetricCollector(new VolumeCollector());
    }

    public static class VolumeCollector implements MetricCollector {
        @Override
        public boolean skipManagementNodeCheck() {
            return false;
        }

        private GaugeMetricFamily createMetric(Metric m, double v) {
            return new GaugeMetricFamily(seriesName(m.getName()), String.format("help for %s", m.getName()), v);
        }

        private GaugeMetricFamily createPerVolumeMettric(Metric m) {
            return new GaugeMetricFamily(seriesName(m.getName()), String.format("help for %s", m.getName()), m.getLabelNames());
        }

        private String seriesName(String metricName) {
            return PrometheusNamespace.makeSeriesName(Namespace.zstackNamespaceName(VolumeNamespace.NAME), metricName);
        }

        @Override
        public List<Collector.MetricFamilySamples> collect() {
            return new SQLBatchWithReturn<List<Collector.MetricFamilySamples>>() {
                private long totalVolume;
                @Override
                protected List<Collector.MetricFamilySamples> scripts() {
                    List<Collector.MetricFamilySamples> samples = new ArrayList<>();
                    samples.addAll(getVolumeMetrics());
                    samples.addAll(getPerVolumeMetrics());

                    return samples;
                }

                private List<Collector.MetricFamilySamples> getVolumeMetrics(){
                    List<Collector.MetricFamilySamples> samples = new ArrayList<>();

                    long rootVolume = q(VolumeVO.class).eq(VolumeVO_.type, VolumeType.Root).count();
                    long dataVolume = q(VolumeVO.class).eq(VolumeVO_.type, VolumeType.Data).count();
                    totalVolume = rootVolume + dataVolume;
                    long readyDataVolume = q(VolumeVO.class).eq(VolumeVO_.type, VolumeType.Data)
                            .eq(VolumeVO_.state, VolumeState.Enabled)
                            .in(VolumeVO_.status, asList(VolumeStatus.Ready, VolumeStatus.NotInstantiated)).count();
                    long rootSnapshot = q(VolumeSnapshotVO.class).eq(VolumeSnapshotVO_.volumeType, VolumeType.Root.toString()).count();
                    long dataSnapshot = q(VolumeSnapshotVO.class).eq(VolumeSnapshotVO_.volumeType, VolumeType.Data.toString()).count();
                    long totalSnapshot = rootSnapshot + dataSnapshot;

                    samples.add(createMetric(VolumeNamespace.TotalVolumeCount, totalVolume));
                    samples.add(createMetric(VolumeNamespace.RootVolumeCount, rootVolume));
                    samples.add(createMetric(VolumeNamespace.RootVolumeInPercent, totalVolume == 0 ? 0 : ((double)rootVolume / totalVolume) * 100));
                    samples.add(createMetric(VolumeNamespace.DataVolumeCount, dataVolume));
                    samples.add(createMetric(VolumeNamespace.DataVolumeInPercent, totalVolume == 0 ? 0 : ((double)dataVolume / totalVolume) * 100));
                    samples.add(createMetric(VolumeNamespace.ReadyDataVolumeCount, readyDataVolume));
                    samples.add(createMetric(VolumeNamespace.ReadyDataVolumeInPercent, dataVolume == 0 ? 0 : ((double)readyDataVolume / dataVolume) * 100));
                    samples.add(createMetric(VolumeNamespace.TotalVolumeSnapshotCount, totalSnapshot));
                    samples.add(createMetric(VolumeNamespace.RootVolumeSnapshotCount, rootSnapshot));
                    samples.add(createMetric(VolumeNamespace.RootVolumeSnapshotInPercent, totalSnapshot == 0 ? 0 : ((double)rootSnapshot / totalSnapshot) * 100));
                    samples.add(createMetric(VolumeNamespace.DataVolumeSnapshotCount, dataSnapshot));
                    samples.add(createMetric(VolumeNamespace.DataVolumeSnapshotInPercent, totalSnapshot == 0 ? 0 : ((double)dataSnapshot / totalSnapshot) * 100));
                    return samples;
                }

                private List<Collector.MetricFamilySamples> getPerVolumeMetrics(){
                    List<Collector.MetricFamilySamples> samples = new ArrayList<>();
                    GaugeMetricFamily VolumeActualSizeInPercent = createPerVolumeMettric(VolumeNamespace.VolumeActualSizeInPercent);
                    samples.add(VolumeActualSizeInPercent);

                    sql("select v.uuid, v.actualSize, v.size from VolumeVO v where v.status != :status and v.actualSize is not NULL", Tuple.class)
                            .param("status", VolumeStatus.NotInstantiated)
                            .limit(10000).paginate(totalVolume, (List<Tuple> ts) -> ts.forEach(it -> {
                                String uuid = it.get(0, String.class);
                                long actualSize = it.get(1, Long.class);
                                long size = it.get(2, Long.class);
                                VolumeActualSizeInPercent.addMetric(asList(uuid), size == 0 ? 0 :((double)actualSize / size) * 100);
                            })
                    );

                    GaugeMetricFamily VolumeXfsFragCount = createPerVolumeMettric(VolumeNamespace.VolumeXfsFragCount);
                    Map<String, String> xfsMap = HostXfsFragReader.getVolumeXfsFrag();
                    for(Map.Entry<String, String> entry : xfsMap.entrySet()) {
                        VolumeXfsFragCount.addMetric(asList(entry.getKey()), Double.parseDouble(entry.getValue()));
                    }
                    samples.add(VolumeXfsFragCount);
                    return samples;
                }
            }.execute();
        }

        @Override
        public String getCollectorName() {
            return VolumeCollector.class.getName();
        }
    }

    public VolumePrometheusNamespace(Namespace namespace) {
        super(namespace);
    }

    @Override
    protected RecordingRule createRecordingRule(Metric metric) {
        RecordingRule r = new RecordingRule(true);
        metric.getLabelNames().forEach(l->r.labelMapping(l, l));
        return r;
    }
}
