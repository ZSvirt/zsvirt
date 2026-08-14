package org.zstack.zwatch.prometheus;

import io.prometheus.client.GaugeMetricFamily;
import org.zstack.zwatch.datatype.Namespace;
import org.zstack.zwatch.datatype.metric.Metric;
import org.zstack.zwatch.namespace.CustomNamespace;

public abstract class BasicMetricCollector implements MetricCollector {
    @Override
    public boolean skipManagementNodeCheck() {
        return false;
    }

    protected String getNamespaceName() {
        return CustomNamespace.NAME;
    }

    protected String seriesName(String metricName) {
        return PrometheusNamespace.makeSeriesName(Namespace.zstackNamespaceName(getNamespaceName()), metricName);
    }

    protected GaugeMetricFamily createMetric(Metric m) {
        return new GaugeMetricFamily(seriesName(m.getName()), String.format("help for %s", m.getName()), m.getLabelNames());
    }

    protected GaugeMetricFamily createMetric(Metric m, double v) {
        return new GaugeMetricFamily(seriesName(m.getName()), String.format("help for %s", m.getName()), v);
    }

}
