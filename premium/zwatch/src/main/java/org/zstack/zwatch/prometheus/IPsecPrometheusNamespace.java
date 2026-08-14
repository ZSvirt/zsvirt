package org.zstack.zwatch.prometheus;

import org.zstack.header.core.StaticInit;
import org.zstack.zwatch.datatype.Namespace;
import org.zstack.zwatch.datatype.metric.Metric;
import org.zstack.zwatch.namespace.IPsecNamespace;

public class IPsecPrometheusNamespace extends AbstractPrometheusNamespace {
    public IPsecPrometheusNamespace (Namespace namespace) {
        super(namespace);
    }

    @StaticInit
    static void staticInit() {
        PrometheusNamespace.namespacesClasses.put(IPsecNamespace.class, IPsecPrometheusNamespace.class);
    }

    @Override
    protected RecordingRule createRecordingRule(Metric metric) {
        RecordingRule rule = new RecordingRule(makeSeriesName(metric.getName()));
        if (metric == IPsecNamespace.IPSecConnectionBytes) {
            rule.setExpression("irate(zstack_ipsec_bytes[10m])");
        } else if (metric == IPsecNamespace.IPSecConnectionPackets) {
            rule.setExpression("irate(zstack_ipsec_packets[10m])");
        }

        rule.setSeriesName(makeSeriesName(metric.getName()));
        return rule;
    }
}
