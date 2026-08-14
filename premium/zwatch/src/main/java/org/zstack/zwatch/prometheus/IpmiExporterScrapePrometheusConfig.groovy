package org.zstack.zwatch.prometheus

import org.zstack.premium.externalservice.prometheus.PrometheusConfig

/**
 * @Author: qiuyu.zhang
 * @Date: 2024/5/16 18:04
 */
class IpmiExporterScrapePrometheusConfig {
    String discoverDir
    String jobName
    String collectedRulePath

    void update(PrometheusConfig config) {
        config.update {
            scrapeConfig {
                job_name = jobName
                scrape_interval = "30s"
                scrape_timeout = "10s"

                fileSDConfig {
                    file("${discoverDir}/*.json")
                    refresh_interval = "10s"
                }
            }

            ruleFile(collectedRulePath)
        }
    }
}
