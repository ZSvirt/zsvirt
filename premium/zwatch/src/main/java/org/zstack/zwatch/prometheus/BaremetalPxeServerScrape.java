package org.zstack.zwatch.prometheus;

import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.EventCallback;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.Component;
import org.zstack.header.baremetal.pxeserver.BaremetalPxeServerCanonicalEvents;
import org.zstack.header.baremetal.pxeserver.BaremetalPxeServerGlobalProperty;
import org.zstack.header.baremetal.pxeserver.BaremetalPxeServerStatus;
import org.zstack.premium.externalservice.prometheus.PreparePrometheusConfigExtensionPoint;
import org.zstack.premium.externalservice.prometheus.Prometheus;
import org.zstack.premium.externalservice.prometheus.PrometheusConfig;
import org.zstack.utils.Bash;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.path.PathUtil;

import java.io.File;
import java.util.HashSet;
import java.util.Map;

/**
 * Created by shixin on 2019/11/15.
 */
public class BaremetalPxeServerScrape implements PreparePrometheusConfigExtensionPoint, Component {

    private static final CLogger logger = Utils.getLogger(BaremetalPxeServerScrape.class);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private PrometheusStaticConfigManager pscMgr;
    @Autowired
    protected EventFacade evf;

    private static final String PXESERVER_JOB = "baremetal-pxeserver-exporter";
    private static final String DISCOVER_DIR = PathUtil.join(Prometheus.DISCORVERY_ROOT, "pxeserver");
    private BaremetalPxeServerScrapePrometheusConfig sconfig;

    public BaremetalPxeServerScrape() {
        sconfig =  new BaremetalPxeServerScrapePrometheusConfig();
        sconfig.setDiscoverDir(DISCOVER_DIR);
        sconfig.setJobName(PXESERVER_JOB);

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

    private void createBaremetalPxeServerConfig(String uuid, String hostname) {
        PrometheusConfig.StaticConfig sc = new PrometheusConfig.StaticConfig();
        HashSet<String> targets = new HashSet<>();
        targets.add(String.format("%s:%s", hostname, BaremetalPxeServerGlobalProperty.PUSHGATEWAY_PORT));
        sc.targets = targets;
        pscMgr.writeBaremetaPxeServerConfig(uuid, hostname, sc);
    }

    private void deleteBaremetalPxeServerConfig(String uuid) {
        File[] files = new File(DISCOVER_DIR).listFiles();
        if (files == null) {
            return;
        }

        for (File file : files) {
            if (file.isFile() && file.getName().startsWith(uuid)) {
                try {
                    FileUtils.forceDelete(file);
                    logger.info(String.format("delete file %s success after delete baremetal pxe server", file.getName()));
                }catch (Exception e) {
                    logger.error(String.format("delete file %s fail after delete baremetal pxe server because %s", file.getName(), e.getMessage()));
                }
            }
        }
    }

    @Override
    public boolean start() {
        evf.on(BaremetalPxeServerCanonicalEvents.BAREMETAL_PXE_SERVER_STATUS_CHANGE, new EventCallback() {
            @Override
            protected void run(Map tokens, Object data) {
                if (!evf.isFromThisManagementNode(tokens)) {
                    return;
                }

                BaremetalPxeServerCanonicalEvents.BaremetalPxeServerStatusChangeData d =
                        (BaremetalPxeServerCanonicalEvents.BaremetalPxeServerStatusChangeData) data;
                if (d.getNewStatus().equals(BaremetalPxeServerStatus.Connected.toString())) {
                    createBaremetalPxeServerConfig(d.getPxeServerUuid(), d.getPxeServerHostName());
                }
            }
        });

        evf.on(BaremetalPxeServerCanonicalEvents.DELETE_BAREMETAL_PXE_SERVER, new EventCallback() {
            @Override
            protected void run(Map tokens, Object data) {
                if (!evf.isFromThisManagementNode(tokens)) {
                    return;
                }

                String uuid = (String) data;
                deleteBaremetalPxeServerConfig(uuid);
            }
        });

        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }
}
