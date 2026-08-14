package org.zstack.zwatch.prometheus;

import org.zstack.header.core.StaticInit;
import org.zstack.zwatch.datatype.metric.Metric;
import org.zstack.zwatch.datatype.Namespace;
import org.zstack.zwatch.namespace.VipNamespace;

public class VipPrometheusNamespace extends AbstractPrometheusNamespace {
    public VipPrometheusNamespace(Namespace namespace) {
        super(namespace);
    }

    @StaticInit
    static void staticInit() {
        PrometheusNamespace.namespacesClasses.put(VipNamespace.class, VipPrometheusNamespace.class);
    }

    @Override
    protected RecordingRule createRecordingRule(Metric metric) {
        RecordingRule rule = new RecordingRule(makeSeriesName(metric.getName()));
        if (metric == VipNamespace.VIPInBoundTrafficInBytes) {
            rule.setExpression("irate(zstack_vip_in_bytes[10m])");
        } else if (metric == VipNamespace.VIPInBoundTrafficInPackages) {
            rule.setExpression("irate(zstack_vip_in_packages[10m])");
        } else if (metric == VipNamespace.VIPOutBoundTrafficInBytes) {
            rule.setExpression("irate(zstack_vip_out_bytes[10m])");
        } else if (metric == VipNamespace.VIPOutBoundTrafficInPackages) {
            rule.setExpression("irate(zstack_vip_out_packages[10m])");
        } else if(metric == VipNamespace.VIPTotalInBytesIn5Min) {
            rule.setExpression("increase(zstack_vip_in_bytes[5m])");
        } else if(metric == VipNamespace.VIPTotalInPacketsIn5Min) {
            rule.setExpression("increase(zstack_vip_in_packages[5m])");
        } else if(metric == VipNamespace.VIPTotalOutBytesIn5Min) {
            rule.setExpression("increase(zstack_vip_out_bytes[5m])");
        } else if(metric == VipNamespace.VIPTotalOutPacketsIn5Min) {
            rule.setExpression("increase(zstack_vip_out_packages[5m])");
        } else if(metric == VipNamespace.VIPTotalInBytesIn1Min) {
            rule.setExpression("increase(zstack_vip_in_bytes[1m])");
        } else if(metric == VipNamespace.VIPTotalInPacketsIn1Min) {
            rule.setExpression("increase(zstack_vip_in_packages[1m])");
        } else if(metric == VipNamespace.VIPTotalOutBytesIn1Min) {
            rule.setExpression("increase(zstack_vip_out_bytes[1m])");
        } else if(metric == VipNamespace.VIPTotalOutPacketsIn1Min) {
            rule.setExpression("increase(zstack_vip_out_packages[1m])");
        }

        rule.setSeriesName(makeSeriesName(metric.getName()));
        return rule;
    }
}
