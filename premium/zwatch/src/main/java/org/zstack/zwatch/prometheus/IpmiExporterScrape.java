package org.zstack.zwatch.prometheus;

import org.zstack.premium.externalservice.prometheus.PreparePrometheusConfigExtensionPoint;
import org.zstack.premium.externalservice.prometheus.Prometheus;
import org.zstack.premium.externalservice.prometheus.PrometheusConfig;
import org.zstack.utils.Bash;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.path.PathUtil;

/**
 * @Author: qiuyu.zhang
 * @Date: 2024/5/16 18:01
 */
public class IpmiExporterScrape implements PreparePrometheusConfigExtensionPoint {
    private static final CLogger logger = Utils.getLogger(IpmiExporterScrape.class);

    private static final String VROUTER_JOB = "ipmi-exporter";
    private static final String DISCOVER_DIR = PathUtil.join(Prometheus.DISCORVERY_ROOT, "ipmi");

    private IpmiExporterScrapePrometheusConfig sconfig;

    public IpmiExporterScrape() {
        sconfig =  new IpmiExporterScrapePrometheusConfig();
        sconfig.setDiscoverDir(DISCOVER_DIR);
        sconfig.setJobName(VROUTER_JOB);

        new Bash() {
            @Override
            protected void scripts() {
                mkdirs(DISCOVER_DIR);
            }
        }.execute();
    }

    @Override
    public void prepareConfig(PrometheusConfig config) {
        sconfig.update(config);
    }
}
