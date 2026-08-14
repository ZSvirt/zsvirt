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
import org.zstack.zwatch.namespace.KVMHostNamespace;

import java.util.ArrayList;
import java.util.List;

import static org.zstack.kvm.KVMConstant.KVM_HYPERVISOR_TYPE;

/**
 * @author shenjin
 * @date 2022/8/26 11:22
 */
public class KVMHostPrometheusNamespace extends HostPrometheusNamespace {
    public static class KVMHostCollector extends HostCollector {
        @Override
        protected String getNamespaceName() {
            return KVMHostNamespace.NAME;
        }

        @Override
        public List<Collector.MetricFamilySamples> collect() {
            return new SQLBatchWithReturn<List<Collector.MetricFamilySamples>>() {
                @Override
                protected List<Collector.MetricFamilySamples> scripts() {
                    List<Collector.MetricFamilySamples> samples = new ArrayList<>();
                    samples.addAll(createHostInfoMetric(KVM_HYPERVISOR_TYPE));
                    samples.addAll(createKVMHostMetrics());
                    return samples;
                }

                private List<Collector.MetricFamilySamples> createKVMHostMetrics() {
                    List<Collector.MetricFamilySamples> samples = new ArrayList<>();
                    Long total = Q.New(HostVO.class).eq(HostVO_.hypervisorType, KVM_HYPERVISOR_TYPE).count();

                    Long connected = Q.New(HostVO.class).eq(HostVO_.hypervisorType, KVM_HYPERVISOR_TYPE)
                            .eq(HostVO_.status, HostStatus.Connected).count();
                    Long disconnected = Q.New(HostVO.class).eq(HostVO_.hypervisorType, KVM_HYPERVISOR_TYPE)
                            .eq(HostVO_.status, HostStatus.Disconnected).count();

                    samples.add(new GaugeMetricFamily(
                            seriesName(KVMHostNamespace.KVMHostTotal.getName()),
                            "help for KVMHostTotal", total.doubleValue()));
                    samples.add(new GaugeMetricFamily(
                            seriesName(KVMHostNamespace.KVMConnectedHostCount.getName()),
                            "help for KVMConnectedHostCount",
                            connected.doubleValue()));
                    samples.add(new GaugeMetricFamily(
                            seriesName(KVMHostNamespace.KVMDisconnectedHostCount.getName()),
                            "help for KVMDisconnectedHostCount",
                            disconnected.doubleValue()));
                    samples.add(new GaugeMetricFamily(
                            seriesName(KVMHostNamespace.KVMConnectedHostInPercent.getName()),
                            "help for KVMConnectedHostInPercent",
                            total == 0 ? 0 : connected.doubleValue() / total.doubleValue() * 100));
                    samples.add(new GaugeMetricFamily(
                            seriesName(KVMHostNamespace.KVMDisconnectedHostInPercent.getName()),
                            "help for KVMDisconnectedHostInPercent",
                            total == 0 ? 0 : disconnected.doubleValue() / total.doubleValue() * 100));
                    return samples;
                }

            }.execute();
        }

        @Override
        public String getCollectorName() {
            return KVMHostCollector.class.getName();
        }
    }

    @StaticInit
    static void staticInit() {
        PrometheusNamespace.namespacesClasses.put(KVMHostNamespace.class, KVMHostPrometheusNamespace.class);
        PrometheusCollector.registerMetricCollector(new KVMHostCollector());
    }

    public KVMHostPrometheusNamespace(Namespace namespace) {
        super(namespace);
    }
}
