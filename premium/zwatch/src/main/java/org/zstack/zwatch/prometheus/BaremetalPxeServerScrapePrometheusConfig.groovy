package org.zstack.zwatch.prometheus

import org.zstack.premium.externalservice.prometheus.PrometheusConfig

class BaremetalPxeServerScrapePrometheusConfig {
    String discoverDir
    String jobName
    String collectedRulePath

    void update(PrometheusConfig config) {
        config.update {
            scrapeConfig {
                job_name = jobName

                fileSDConfig {
                    file("${discoverDir}/*.json")
                    refresh_interval = "10s"
                }
            }

            ruleFile(collectedRulePath)
        }
    }
}
