package org.zstack.zwatch.prometheus;

import io.prometheus.client.Collector;
import io.prometheus.client.GaugeMetricFamily;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQLBatchWithReturn;
import org.zstack.header.core.StaticInit;
import org.zstack.header.host.HostStatus;
import org.zstack.header.host.HostVO;
import org.zstack.header.host.HostVO_;
import org.zstack.zwatch.datatype.Namespace;
import org.zstack.zwatch.namespace.XDragonHostNamespace;

import java.util.ArrayList;
import java.util.List;

import static org.zstack.xdragon.XDragonConstant.HYPERVISOR_TYPE;

/**
 * @author shenjin
 * @date 2022/8/26 11:24
 */
public class XDragonHostPrometheusNamespace extends HostPrometheusNamespace {
    public static class XDragonHostCollector extends HostCollector {
        @Override
        protected String getNamespaceName() {
            return XDragonHostNamespace.NAME;
        }

        @Override
        public List<Collector.MetricFamilySamples> collect() {
            return new SQLBatchWithReturn<List<Collector.MetricFamilySamples>>() {
                @Override
                protected List<Collector.MetricFamilySamples> scripts() {
                    List<Collector.MetricFamilySamples> samples = new ArrayList<>();
                    samples.addAll(createHostInfoMetric(HYPERVISOR_TYPE));
                    samples.addAll(createXDragonHostMetrics());
                    return samples;
                }

                private List<Collector.MetricFamilySamples> createXDragonHostMetrics() {
                    List<Collector.MetricFamilySamples> samples = new ArrayList<>();
                    Long total = Q.New(HostVO.class).eq(HostVO_.hypervisorType, HYPERVISOR_TYPE).count();

                    Long connected = Q.New(HostVO.class).eq(HostVO_.hypervisorType, HYPERVISOR_TYPE)
                            .eq(HostVO_.status, HostStatus.Connected).count();
                    Long disconnected = Q.New(HostVO.class).eq(HostVO_.hypervisorType, HYPERVISOR_TYPE)
                            .eq(HostVO_.status, HostStatus.Disconnected).count();

                    samples.add(new GaugeMetricFamily(
                            seriesName(XDragonHostNamespace.XDragonHostTotal.getName()),
                            "help for XDragonHostTotal",
                            total.doubleValue()));
                    samples.add(new GaugeMetricFamily(
                            seriesName(XDragonHostNamespace.XDragonConnectedHostCount.getName()),
                            "help for XDragonConnectedHostCount",
                            connected.doubleValue()));
                    samples.add(new GaugeMetricFamily(
                            seriesName(XDragonHostNamespace.XDragonDisconnectedHostCount.getName()),
                            "help for XDragonDisconnectedHostCount",
                            disconnected.doubleValue()));
                    samples.add(new GaugeMetricFamily(
                            seriesName(XDragonHostNamespace.XDragonConnectedHostInPercent.getName()),
                            "help for XDragonConnectedHostInPercent",
                            total == 0 ? 0 : connected.doubleValue() / total.doubleValue() * 100));
                    samples.add(new GaugeMetricFamily(
                            seriesName(XDragonHostNamespace.XDragonDisconnectedHostInPercent.getName()),
                            "help for XDragonDisconnectedHostInPercent",
                            total == 0 ? 0 : disconnected.doubleValue() / total.doubleValue() * 100));
                    return samples;
                }

            }.execute();
        }

        @Override
        public String getCollectorName() {
            return XDragonHostCollector.class.getName();
        }
    }

    @StaticInit
    static void staticInit() {
        PrometheusNamespace.namespacesClasses.put(XDragonHostNamespace.class, XDragonHostPrometheusNamespace.class);
        PrometheusCollector.registerMetricCollector(new XDragonHostCollector());
    }

    public XDragonHostPrometheusNamespace(Namespace namespace) {
        super(namespace);
    }
}
