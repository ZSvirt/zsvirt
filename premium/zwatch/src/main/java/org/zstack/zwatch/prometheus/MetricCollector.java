package org.zstack.zwatch.prometheus;

import io.prometheus.client.Collector;

import java.util.List;

public interface MetricCollector {
    boolean skipManagementNodeCheck();

    List<Collector.MetricFamilySamples> collect();

    String getCollectorName();
}
