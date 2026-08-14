package org.zstack.zwatch.prometheus

import org.zstack.core.Platform
import org.zstack.premium.externalservice.prometheus.Prometheus
import org.zstack.premium.externalservice.prometheus.PrometheusConfig
import org.zstack.premium.externalservice.prometheus.PrometheusGlobalProperty
import org.zstack.utils.Bash
import org.zstack.utils.gson.JSONObjectUtil

class ManagementNodeExporterPrometheusConfig {
    void config(PrometheusConfig config) {
        String managementNodeDiscoveryDir = [Prometheus.DISCORVERY_ROOT, "management-node"].join("/")
        String managementNodeDiscoveryPath = [managementNodeDiscoveryDir, "*.json"].join("/")

        new Bash() {
            @Override
            protected void scripts() {
                mkdirs(dirname(managementNodeDiscoveryPath))

                PrometheusConfig.StaticConfig serverConfig = new PrometheusConfig.StaticConfig()
                serverConfig.targets = [
                        "${Platform.getManagementServerIp()}:${PrometheusGlobalProperty.EXPORTER_PORT}".toString(),
                        "127.0.0.1:${PrometheusGlobalProperty.ZSHA2_EXPORTER_PORT}".toString(),
                ]
                serverConfig.labels = [:]

                String managementServerExporterStaticConfigFile = [managementNodeDiscoveryDir, "management-server-exporter.json"].join("/")
                writeFile(managementServerExporterStaticConfigFile, JSONObjectUtil.toJsonString([serverConfig]))
            }
        }.execute()

        config.scrapeConfig {
            job_name = "management-server-exporter"
            fileSDConfig {
                file(managementNodeDiscoveryPath)
            }
        }
    }
}
