package org.zstack.zwatch.prometheus;

import io.prometheus.client.Collector;
import io.prometheus.client.GaugeMetricFamily;
import org.zstack.core.db.SQLBatchWithReturn;
import org.zstack.header.core.StaticInit;
import org.zstack.header.network.l3.L3NetworkState;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.utils.network.NetworkUtils;
import org.zstack.zwatch.datatype.metric.Metric;
import org.zstack.zwatch.datatype.Namespace;
import org.zstack.zwatch.namespace.L3NetworkNamespace;

import javax.persistence.Tuple;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;

public class L3NetworkPrometheusNamespace extends AbstractPrometheusNamespace {
    @StaticInit
    static void staticInit() {
        PrometheusNamespace.namespacesClasses.put(L3NetworkNamespace.class, L3NetworkPrometheusNamespace.class);
        PrometheusCollector.registerMetricCollector(new L3NetworkCollector());
    }

    public static class L3NetworkCollector implements MetricCollector {
        private String seriesName(String metricName) {
            return PrometheusNamespace.makeSeriesName(Namespace.zstackNamespaceName(L3NetworkNamespace.NAME), metricName);
        }

        private GaugeMetricFamily createMetric(Metric m) {
            return new GaugeMetricFamily(seriesName(m.getName()), String.format("help for %s", m.getName()), m.getLabelNames());
        }

        @Override
        public boolean skipManagementNodeCheck() {
            return false;
        }

        @Override
        public List<Collector.MetricFamilySamples> collect() {
            return new SQLBatchWithReturn<List<Collector.MetricFamilySamples>>() {
                long allL3Total;
                long allL3Avail;
                long allL3Used;
                long allL3Locked;

                @Override
                protected List<Collector.MetricFamilySamples> scripts() {
                    List<Collector.MetricFamilySamples> samples = new ArrayList<>();

                    GaugeMetricFamily AvailableIPCount = createMetric(L3NetworkNamespace.AvailableIPCount);
                    samples.add(AvailableIPCount);
                    GaugeMetricFamily AvailableIPInPercent = createMetric(L3NetworkNamespace.AvailableIPInPercent);
                    samples.add(AvailableIPInPercent);
                    GaugeMetricFamily UsedIPCount = createMetric(L3NetworkNamespace.UsedIPCount);
                    samples.add(UsedIPCount);
                    GaugeMetricFamily UsedIPInPercent = createMetric(L3NetworkNamespace.UsedIPInPercent);
                    samples.add(UsedIPInPercent);

                    long totalL3 = q(L3NetworkVO.class).count();

                    class L3Info {
                        String type;
                        L3NetworkState state;

                        L3Info(String type, L3NetworkState state) {
                            this.type = type;
                            this.state = state;
                        }
                    }

                    sql("select l3.uuid, l3.state, l3.type, ipr.startIp, ipr.endIp from L3NetworkVO l3, IpRangeVO ipr where l3.uuid = ipr.l3NetworkUuid and ipr.ipVersion = 4", Tuple.class)
                            .limit(500).paginate(totalL3, (List<Tuple> tss)-> {
                                Map<String, Long> l3TotalIPCounts = new HashMap<>();
                                Map<String, L3Info> info = new HashMap<>();

                                tss.forEach(ts -> {
                                    String l3Uuid = ts.get(0, String.class);
                                    L3NetworkState state = ts.get(1, L3NetworkState.class);
                                    String type = ts.get(2, String.class);
                                    String startIp = ts.get(3, String.class);
                                    String endIp = ts.get(4, String.class);

                                    Long l3TotalIPCount = l3TotalIPCounts.getOrDefault(l3Uuid, 0L);
                                    l3TotalIPCount += NetworkUtils.getTotalIpInRange(startIp, endIp);
                                    l3TotalIPCounts.put(l3Uuid, l3TotalIPCount);

                                    info.putIfAbsent(l3Uuid, new L3Info(type, state));
                                });

                                l3TotalIPCounts.forEach((l3Uuid, l3TotalIPCount)-> {
                                    /* gateway ip is not in the iprange */
                                    long used = sql("select count(distinct u.ip) from UsedIpVO u where u.gateway != u.ip and u.l3NetworkUuid = :l3Uuid and u.ipVersion = 4", Long.class)
                                            .param("l3Uuid", l3Uuid).find();

                                    L3Info l3Info = info.get(l3Uuid);
                                    long avail = 0;
                                    long locked = 0;
                                    if (l3Info.state == L3NetworkState.Enabled) {
                                        avail = l3TotalIPCount - used;
                                    } else {
                                        locked = l3TotalIPCount - used;
                                    }

                                    allL3Total += l3TotalIPCount;
                                    allL3Avail += avail;
                                    allL3Used += used;
                                    allL3Locked += locked;

                                    AvailableIPCount.addMetric(asList(l3Uuid, l3Info.type), avail);
                                    AvailableIPInPercent.addMetric(asList(l3Uuid, l3Info.type), l3TotalIPCount == 0 ? 0 : ((double) avail / l3TotalIPCount) * 100);
                                    UsedIPCount.addMetric(asList(l3Uuid, l3Info.type), used);
                                    UsedIPInPercent.addMetric(asList(l3Uuid, l3Info.type), l3TotalIPCount == 0 ? 0 : ((double) used / l3TotalIPCount) * 100);
                                });
                    });

                    samples.add(new GaugeMetricFamily(seriesName(L3NetworkNamespace.TotalAvailableIPCount.getName()), "help for TotalAvailableIPCount", allL3Avail));
                    samples.add(new GaugeMetricFamily(seriesName(L3NetworkNamespace.TotalAvailableIPInPercent.getName()), "help for TotalAvailableIPInPercent", allL3Total == 0 ? 0 : ((double) allL3Avail / allL3Total) * 100));
                    samples.add(new GaugeMetricFamily(seriesName(L3NetworkNamespace.TotalUsedIPCount.getName()), "help for TotalUsedIPCount", allL3Used));
                    samples.add(new GaugeMetricFamily(seriesName(L3NetworkNamespace.TotalUsedIPInPercent.getName()), "help for TotalUsedIPInPercent", allL3Total == 0 ? 0 : ((double) allL3Used / allL3Total) * 100));
                    samples.add(new GaugeMetricFamily(seriesName(L3NetworkNamespace.TotalLockedIPCount.getName()), "help for TotalLockedIPCount", allL3Locked));
                    samples.add(new GaugeMetricFamily(seriesName(L3NetworkNamespace.TotalLockedIPInPercent.getName()), "help for TotalLockedIPInPercent", allL3Total == 0 ? 0 : ((double) allL3Locked / allL3Total) * 100));

                    return samples;
                }
            }.execute();
        }

        @Override
        public String getCollectorName() {
            return L3NetworkCollector.class.getName();
        }
    }

    public L3NetworkPrometheusNamespace(Namespace namespace) {
        super(namespace);
    }

    @Override
    protected RecordingRule createRecordingRule(Metric metric) {
        RecordingRule r = new RecordingRule(true);
        metric.getLabelNames().forEach(l->r.labelMapping(l, l));
        return r;
    }
}
