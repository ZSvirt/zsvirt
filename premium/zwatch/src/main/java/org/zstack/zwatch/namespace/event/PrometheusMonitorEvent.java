package org.zstack.zwatch.namespace.event;

import org.zstack.premium.externalservice.prometheus.PrometheusDriverExtensionPoint;
import org.zstack.zwatch.prometheus.MonitorPrometheusNamespace;

/**
 * Created by mingjian.deng on 2020/4/8.
 */
public class PrometheusMonitorEvent implements PrometheusDriverExtensionPoint {
    public PrometheusMonitorEvent() {
    }

    @Override
    public void beforePrometheusApicall(String ip, String url) {
        MonitorPrometheusNamespace.beforePrometheusApicall(ip, url);
    }
}
