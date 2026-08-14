package org.zstack.zwatch.prometheus;

import org.zstack.header.core.StaticInit;
import org.zstack.zwatch.datatype.metric.Metric;
import org.zstack.zwatch.datatype.Namespace;
import org.zstack.zwatch.namespace.LoadBalancerNamespace;

public class LoadBalancerPrometheusNamespace extends AbstractPrometheusNamespace {
    public LoadBalancerPrometheusNamespace(Namespace namespace) {
        super(namespace);
    }

    @StaticInit
    static void staticInit() {
        PrometheusNamespace.namespacesClasses.put(LoadBalancerNamespace.class, LoadBalancerPrometheusNamespace.class);
    }

    @Override
    protected RecordingRule createRecordingRule(Metric metric) {
        RecordingRule rule = new RecordingRule(makeSeriesName(metric.getName()));
        if (metric == LoadBalancerNamespace.LoadBalancerBackendTrafficInBytes) {
            rule.setExpression("irate(zstack_lb_in_bytes[10m])");
        } else if (metric == LoadBalancerNamespace.LoadBalancerBackendTrafficOutBytes) {
            rule.setExpression("irate(zstack_lb_out_bytes[10m])");
        } else if (metric == LoadBalancerNamespace.LoadBalancerBackendStatus) {
            rule.setExpression("zstack_lb_status");
            rule.labelMapping("LbUuid", LoadBalancerNamespace.LabelNames.LoadBalancerUuid.toString());
        } else if (metric == LoadBalancerNamespace.LoadBalancerBackendSessionNumber) {
            rule.setExpression("zstack_lb_cur_session_num");
        } else if (metric == LoadBalancerNamespace.LoadBalancerBackendHttp1xxResponses) {
            rule.setExpression("zstack_lb_hrsp1xx");
        } else if (metric == LoadBalancerNamespace.LoadBalancerBackendHttp2xxResponses) {
            rule.setExpression("zstack_lb_hrsp2xx");
        } else if (metric == LoadBalancerNamespace.LoadBalancerBackendHttp3xxResponses) {
            rule.setExpression("zstack_lb_hrsp3xx");
        } else if (metric == LoadBalancerNamespace.LoadBalancerBackendHttp4xxResponses) {
            rule.setExpression("zstack_lb_hrsp4xx");
        } else if (metric == LoadBalancerNamespace.LoadBalancerBackendHttp5xxResponses) {
            rule.setExpression("zstack_lb_hrsp5xx");
        } else if (metric == LoadBalancerNamespace.LoadBalancerBackendHttpOtherResponses) {
            rule.setExpression("zstack_lb_hrspOther");
        }  else if (metric == LoadBalancerNamespace.LoadBalancerStatus) {
            rule.setExpression("sum(zstack_lb_status) by (ListenerUuid)");
        } else if (metric == LoadBalancerNamespace.LoadBalancerSessionNumber) {
            rule.setExpression("sum(zstack_lb_cur_session_num) by (ListenerUuid)");
        } else if (metric == LoadBalancerNamespace.LoadBalancerTrafficInBytes) {
            rule.setExpression("sum(irate(zstack_lb_in_bytes[10m])) by (ListenerUuid)");
        } else if (metric == LoadBalancerNamespace.LoadBalancerTrafficOutBytes) {
            rule.setExpression("sum(irate(zstack_lb_out_bytes[10m])) by (ListenerUuid)");
        }else if (metric == LoadBalancerNamespace.LoadBalancerHttp1xxResponses ) {
            rule.setExpression("sum(zstack_lb_hrsp1xx) by (ListenerUuid)");
        } else if (metric == LoadBalancerNamespace.LoadBalancerHttp2xxResponses ) {
            rule.setExpression("sum(zstack_lb_hrsp2xx) by (ListenerUuid)");
        } else if (metric == LoadBalancerNamespace.LoadBalancerHttp3xxResponses ) {
            rule.setExpression("sum(zstack_lb_hrsp3xx) by (ListenerUuid)");
        } else if (metric == LoadBalancerNamespace.LoadBalancerHttp4xxResponses ) {
            rule.setExpression("sum(zstack_lb_hrsp4xx) by (ListenerUuid)");
        } else if (metric == LoadBalancerNamespace.LoadBalancerHttp5xxResponses ) {
            rule.setExpression("sum(zstack_lb_hrsp5xx) by (ListenerUuid)");
        } else if (metric == LoadBalancerNamespace.LoadBalancerHttpOtherResponses ) {
            rule.setExpression("sum(zstack_lb_hrspOther) by (ListenerUuid)");
        } else if (metric == LoadBalancerNamespace.LoadBalancerSessionUsage) {
            rule.setExpression("zstack_lb_cur_session_usage");
        } else if (metric == LoadBalancerNamespace.LoadBalancerRefusedSessionNumber ) {
            rule.setExpression("sum(zstack_lb_refused_session_num) by (ListenerUuid)");
        } else if (metric == LoadBalancerNamespace.LoadBalancerConcurrentSessionNumber) {
            rule.setExpression("sum(zstack_lb_concurrent_session_num) by (ListenerUuid)");
        } else if (metric == LoadBalancerNamespace.LoadBalancerBackendConcurrentSessionNumber) {
            rule.setExpression("zstack_lb_concurrent_session_num");
        } else if (metric == LoadBalancerNamespace.LoadBalancerNewSessionNumber) {
            rule.setExpression("round(sum(irate(zstack_lb_total_session_num[5m])) by (ListenerUuid))");
        } else if (metric == LoadBalancerNamespace.LoadBalancerBackendNewSessionNumber) {
            rule.setExpression("round(irate(zstack_lb_total_session_num[5m]))");
        } else if (metric == LoadBalancerNamespace.LoadBalancerTotalSessionNumber) {
            rule.setExpression("sum(zstack_lb_total_session_num) by (ListenerUuid)");
        } else if (metric == LoadBalancerNamespace.LoadBalancerBackendTotalSessionNumber) {
            rule.setExpression("zstack_lb_total_session_num");
        } else if (metric == LoadBalancerNamespace.LoadBalancerBackendRefusedSessionNumber) {
            rule.setExpression("zstack_lb_refused_session_num");
        }



        rule.setSeriesName(makeSeriesName(metric.getName()));
        return rule;
    }
}
