package org.zstack.premium.externalservice.prometheus.nodeexporter;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.externalservice.ExternalServiceFactory;
import org.zstack.core.externalservice.ExternalServiceManager;
import org.zstack.core.externalservice.ExternalServiceType;

public class NodeExporterFactory implements ExternalServiceFactory {
    public static final ExternalServiceType type = new ExternalServiceType("PrometheusNodeExporter");

    @Autowired
    private ExternalServiceManager manager;

    @Override
    public String getExternalServiceType() {
        return type.toString();
    }

    public LocalNodeExporter getLocalNodeExporter(LocalNodeExporterParam param) {
        LocalNodeExporter exporter = new LocalNodeExporterImpl(param);
        return (LocalNodeExporter) manager.getService(exporter.getName(), () -> exporter);
    }
}
