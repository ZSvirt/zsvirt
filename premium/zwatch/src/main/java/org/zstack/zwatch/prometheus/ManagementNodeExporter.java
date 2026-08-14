package org.zstack.zwatch.prometheus;

import org.zstack.premium.externalservice.prometheus.PreparePrometheusConfigExtensionPoint;
import org.zstack.premium.externalservice.prometheus.PrometheusConfig;

public class ManagementNodeExporter implements PreparePrometheusConfigExtensionPoint {
    @Override
    public void prepareConfig(PrometheusConfig config) {
        new ManagementNodeExporterPrometheusConfig().config(config);
    }
}
