package org.zstack.zwatch.prometheus;

import org.apache.commons.lang.time.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.util.UriComponentsBuilder;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.ansible.AnsibleGlobalProperty;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.thread.AsyncThread;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.imagestore.ImageStorageContinueConnectExtensionPoint;
import org.zstack.header.managementnode.ManagementNodeReadyExtensionPoint;
import org.zstack.header.rest.JsonAsyncRESTCallback;
import org.zstack.header.rest.RESTFacade;
import org.zstack.header.storage.backup.*;
import org.zstack.premium.externalservice.prometheus.PreparePrometheusConfigExtensionPoint;
import org.zstack.premium.externalservice.prometheus.Prometheus;
import org.zstack.premium.externalservice.prometheus.PrometheusConfig;
import org.zstack.storage.backup.imagestore.ImageStoreBackupStorageCommands;
import org.zstack.storage.backup.imagestore.ImageStoreBackupStorageGlobalProperty;
import org.zstack.storage.backup.imagestore.ImageStoreBackupStorageInventory;
import org.zstack.storage.backup.imagestore.ImageStoreBackupStorageSelector;
import org.zstack.utils.Bash;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.path.PathUtil;
import org.zstack.zwatch.ZWatchGlobalConfig;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.operr;

/**
 * Created by MaJin on 2020/5/12.
 */
public class ImageStoreScrape implements PreparePrometheusConfigExtensionPoint, ImageStorageContinueConnectExtensionPoint,
        ManagementNodeReadyExtensionPoint, BackupStorageDeleteExtensionPoint {
    private static final CLogger logger = Utils.getLogger(ImageStoreScrape.class);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private RESTFacade restf;
    @Autowired
    private PrometheusStaticConfigManager pscMgr;

    public static final String BACKUP_STORAGE_JOB = "backup-storage-exporter";
    public static final String DISCOVER_DIR = PathUtil.join(Prometheus.DISCORVERY_ROOT, "backupStorage");
    public static final String COLLECTD_PORT = "25827";
    public static final String WEB_PORT = "9104";

    public static final String COLLECTD_PATH = "/prometheus/collectdexporter/start";

    private final ImageStoreScrapePrometheusConfig iconfig;

    public ImageStoreScrape() {
        iconfig = new ImageStoreScrapePrometheusConfig();
        iconfig.setDiscoverDir(DISCOVER_DIR);
        iconfig.setJobName(BACKUP_STORAGE_JOB);

        new Bash() {
            @Override
            protected void scripts() {
                mkdirs(DISCOVER_DIR);
           }
        }.execute();
    }

    public static class StartCollectdExporterCmd extends ImageStoreBackupStorageCommands.AgentCommand {
        public String binaryPath;
        public String startupArguments;
        public int interval = 10;
        public String version;
    }

    public static class StartCollectdExporterRsp extends ImageStoreBackupStorageCommands.ImageStoreResponse {
    }

    @Override
    public void prepareConfig(PrometheusConfig config) {
        iconfig.update(config);
    }

    @Override
    public List<Flow> createImageStoreConnectingFlow(boolean newAdded, ImageStoreBackupStorageInventory inv) {
        List<Flow> flows = new ArrayList<>();
        if (!ImageStoreBackupStorageSelector.isRemote(inv.getUuid())) {
            Flow flow = new NoRollbackFlow() {
                String __name__ = "start-collectd-exporter";

                @Override
                public boolean skip(Map data) {
                    return PrometheusHelper.isPrometheusDisabled();
                }

                @Override
                public void run(FlowTrigger trigger, Map data) {
                    String bsIp = inv.hostname;
                    String bsUuid = inv.getUuid();

                    pscMgr.writeBackupStorageConfig(bsUuid, bsIp, PrometheusStaticConfigManager.createBackupStorageConfig(bsUuid, bsIp));

                    StartCollectdExporterCmd cmd = new StartCollectdExporterCmd();
                    cmd.binaryPath = PathUtil.join(AnsibleGlobalProperty.ZSTACK_ROOT, "imagestorebackupstorage/collectd_exporter");
                    cmd.startupArguments =
                            String.format(Prometheus.COLLECTD_EXPORTER_PORT_STARTUP_ARGUMENT_FORMAT, COLLECTD_PORT) +
                                    String.format(Prometheus.COLLECTD_EXPORTER_WEB_PORT_STARTUP_ARGUMENT_FORMAT, WEB_PORT);
                    cmd.interval = ZWatchGlobalConfig.SCRAPE_INTERVAL.value(Integer.class) / 2;
                    cmd.version = dbf.getDbVersion();

                    restf.asyncJsonPost(buildUrl(COLLECTD_PATH, bsIp), cmd, new JsonAsyncRESTCallback<StartCollectdExporterRsp>(trigger) {
                        @Override
                        public void fail(ErrorCode err) {
                            // only log it, see ZSTACK-33773
                            logger.warn(err.toString());
                            trigger.next();
                        }

                        @Override
                        public void success(StartCollectdExporterRsp rsp) {
                            if (!rsp.isSuccess()) {
                                trigger.fail(operr("%s", rsp.getError()));
                            } else {
                                trigger.next();
                            }
                        }

                        @Override
                        public Class<StartCollectdExporterRsp> getReturnClass() {
                            return StartCollectdExporterRsp.class;
                        }
                    }, TimeUnit.SECONDS, 60);
                }
            };
            flows.add(flow);
        }

        return flows;
    }

    private String buildUrl(String subPath, String hostname) {
        UriComponentsBuilder ub = UriComponentsBuilder.newInstance();
        ub.scheme(ImageStoreBackupStorageGlobalProperty.AGENT_URL_SCHEME);
        if (CoreGlobalProperty.UNIT_TEST_ON) {
            ub.host("localhost");
        } else {
            ub.host(hostname);
        }

        ub.port(ImageStoreBackupStorageGlobalProperty.AGENT_PORT);
        if (!"".equals(ImageStoreBackupStorageGlobalProperty.AGENT_URL_ROOT_PATH)) {
            ub.path(ImageStoreBackupStorageGlobalProperty.AGENT_URL_ROOT_PATH);
        }
        ub.path(subPath);
        return ub.build().toUriString();
    }

    @Override
    public void managementNodeReady() {
        cleanUpRedundantPrometheusJsonFile();
    }

    @AsyncThread
    private void cleanUpRedundantPrometheusJsonFile() {
        logger.debug("Starting to delete redundant backup storage prometheus json file");
        StopWatch watch = new StopWatch();
        watch.start();
        List<String> files = new ArrayList<>();
        PathUtil.scanFolder(files, DISCOVER_DIR);

        List<String> bsUuids = Q.New(BackupStorageVO.class).select(BackupStorageVO_.uuid).listValues();

        files = files.stream().filter(f -> bsUuids.stream().noneMatch(h -> f.startsWith(Paths.get(DISCOVER_DIR, h).toString()))).collect(Collectors.toList());

        files.parallelStream().forEach(PathUtil::forceRemoveFile);
        watch.stop();
        logger.debug(String.format("All redundant prometheus files have been deleted, cost: %sms", watch.getTime()));
    }

    @Override
    public void preDeleteSecondaryStorage(BackupStorageInventory inv) throws BackupStorageException {

    }

    @Override
    public void beforeDeleteSecondaryStorage(BackupStorageInventory inv) {

    }

    @Override
    public void afterDeleteSecondaryStorage(BackupStorageInventory inv) {
        pscMgr.deleteBackupStorageConfig(inv.getUuid());
    }
}
