package org.zstack.guesttools.kvm;

import org.zstack.header.core.ReturnValueCompletion;

public interface PrometheusQueryHandler {
    /**
     * Query value from Prometheus.
     * @param completion
     *   return value:
     *   - If query Prometheus success, return the newest value;
     *   - If Prometheus return empty results, return defaultValue;
     *   - If query Prometheus fail, completion.fail() will be invoked.
     */
    void query(String vmUuid, String metricName, Double defaultValue, ReturnValueCompletion<Double> completion);
}
