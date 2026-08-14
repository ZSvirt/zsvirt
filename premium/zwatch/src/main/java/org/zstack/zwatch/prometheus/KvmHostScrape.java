package org.zstack.zwatch.prometheus;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.ansible.AnsibleGlobalProperty;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.thread.AsyncThread;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.host.*;
import org.zstack.header.managementnode.ManagementNodeReadyExtensionPoint;
import org.zstack.kvm.*;
import org.zstack.premium.externalservice.prometheus.PreparePrometheusConfigExtensionPoint;
import org.zstack.premium.externalservice.prometheus.Prometheus;
import org.zstack.premium.externalservice.prometheus.PrometheusConfig;
import org.zstack.premium.externalservice.prometheus.PrometheusConstant;
import org.zstack.utils.Bash;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.path.PathUtil;
import org.zstack.zwatch.ZWatchGlobalConfig;
import org.zstack.zwatch.ZWatchGlobalProperty;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.operr;

/**
 * Created by xing5 on 2016/7/9.
 */
public class KvmHostScrape implements KVMHostConnectExtensionPoint, PreparePrometheusConfigExtensionPoint
        , HostDeleteExtensionPoint, ManagementNodeReadyExtensionPoint {

    private static final CLogger logger = Utils.getLogger(KvmHostScrape.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private PrometheusStaticConfigManager pscMgr;
    @Autowired
    private PluginRegistry pluginRgty;

    public static final String COLLECTD = "collectd";
    public static final String DISCOVER_DIR = PathUtil.join(Prometheus.DISCORVERY_ROOT, "hosts");

    public static final String COLLECTD_PATH = "/prometheus/collectdexporter/start";
    public static final String HOST_RULES_DIR = PathUtil.join(Prometheus.RULES_ROOT, "hosts");
    public static final String COLLECTD_RULE_PATH = PathUtil.join(HOST_RULES_DIR, PrometheusConstant.COLLECTD_RULE_NAME);


    private KvmHostScrapePrometheusConfig kconfig;

    public KvmHostScrape() {
        kconfig =  new KvmHostScrapePrometheusConfig();
        kconfig.setDiscoverDir(DISCOVER_DIR);
        kconfig.setCollectedRulePath(COLLECTD_RULE_PATH);
        kconfig.setJobName(COLLECTD);

        new Bash() {
            @Override
            protected void scripts() {
                mkdirs(HOST_RULES_DIR);
                mkdirs(DISCOVER_DIR);
                copyDir(PathUtil.findFolderOnClassPath("mevoco/prometheus/rules/hosts", true).getAbsolutePath(), HOST_RULES_DIR);
            }
        }.execute();
    }

    @Override
    public void prepareConfig(PrometheusConfig config) {
        kconfig.update(config);
    }

    @Override
    public void managementNodeReady() {
        cleanUpRedundantPrometheusJsonFile();
    }

    @AsyncThread
    private void cleanUpRedundantPrometheusJsonFile() {
        logger.debug("Starting to delete redundant prometheus json file");
        StopWatch watch = new StopWatch();
        watch.start();
        List<String> files = new ArrayList<>();
        PathUtil.scanFolder(files, DISCOVER_DIR);

        List<String> hostUuids = Q.New(HostVO.class).select(HostVO_.uuid).listValues();

        files = files.stream().filter(f -> hostUuids.stream().noneMatch(h -> f.startsWith(Paths.get(DISCOVER_DIR, h).toString()))).collect(Collectors.toList());

        files.parallelStream().forEach(PathUtil::forceRemoveFile);
        watch.stop();
        logger.debug(String.format("All redundant prometheus files have been deleted, cost: %sms", watch.getTime()));
    }

    public static class CollectdExporterCmd extends KVMAgentCommands.AgentCommand {
        public String binaryPath;
        public String startupArguments;
        public int interval = 10;
        public String version;
    }

    public static class StartCollectdExporterCmd extends KVMAgentCommands.AgentCommand {
        public List<CollectdExporterCmd> cmds;
    }

    public static class StartCollectdExporterRsp extends KVMAgentCommands.AgentResponse {
    }

    @Override
    public Flow createKvmHostConnectingFlow(KVMHostConnectedContext context) {
        return new NoRollbackFlow() {
            String __name__ = "run-prometheus-collectd-exporter";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                if (PrometheusHelper.isPrometheusDisabled()) {
                    trigger.next();
                    return;
                }

                String hostIp = context.getInventory().getManagementIp();
                String hostUuid = context.getInventory().getUuid();

                pluginRgty.getExtensionList(HostConfigExtensionPoint.class).forEach(e -> {
                    pscMgr.writeHostConfig(hostUuid, hostIp, e.getHostConfig(hostUuid, hostIp), e.getHostConfigPath());
                });

                StartCollectdExporterCmd startCollectdExporterCmd = new StartCollectdExporterCmd();
                startCollectdExporterCmd.cmds = new ArrayList<>(3);
                CollectdExporterCmd collectdExporterCmd = new CollectdExporterCmd();
                collectdExporterCmd.binaryPath = PathUtil.join(AnsibleGlobalProperty.ZSTACK_ROOT, "kvm/collectd_exporter");
                collectdExporterCmd.startupArguments = Prometheus.COLLECTD_EXPORTER_STARTUP_ARGUMENT_0;
                collectdExporterCmd.interval = ZWatchGlobalConfig.SCRAPE_INTERVAL.value(Integer.class) / 2;
                startCollectdExporterCmd.cmds.add(collectdExporterCmd);

                CollectdExporterCmd nodeExporterCmd = new CollectdExporterCmd();
                nodeExporterCmd.binaryPath = PathUtil.join(AnsibleGlobalProperty.ZSTACK_ROOT, "kvm/node_exporter");
                nodeExporterCmd.interval = ZWatchGlobalConfig.SCRAPE_INTERVAL.value(Integer.class) / 2;
                startCollectdExporterCmd.cmds.add(nodeExporterCmd);

                CollectdExporterCmd pushgatewayCmd = new CollectdExporterCmd();
                pushgatewayCmd.binaryPath = PathUtil.join(AnsibleGlobalProperty.ZSTACK_ROOT, "kvm/pushgateway");
                pushgatewayCmd.startupArguments = getPushGatewayStartupArguments();
                pushgatewayCmd.interval = ZWatchGlobalConfig.SCRAPE_INTERVAL.value(Integer.class) / 2;
                startCollectdExporterCmd.cmds.add(pushgatewayCmd);
                startCollectdExporterCmd.cmds.forEach(it -> it.version = dbf.getDbVersion());

                CollectdExporterCmd ipmitoolExporterCmd = new CollectdExporterCmd();
                ipmitoolExporterCmd.binaryPath = PathUtil.join(AnsibleGlobalProperty.ZSTACK_ROOT, "kvm/ipmi_exporter");
                ipmitoolExporterCmd.startupArguments = getIpmiToolStartupArguments();
                startCollectdExporterCmd.cmds.add(ipmitoolExporterCmd);

                new KvmCommandSender(hostUuid, true).send(startCollectdExporterCmd, COLLECTD_PATH, new KvmCommandFailureChecker() {
                    @Override
                    public ErrorCode getError(KvmResponseWrapper wrapper) {
                        StartCollectdExporterRsp rsp = wrapper.getResponse(StartCollectdExporterRsp.class);
                        return rsp.isSuccess() ? null : operr("%s", rsp.getError());
                    }
                }, new ReturnValueCompletion<KvmResponseWrapper>(trigger) {
                    @Override
                    public void success(KvmResponseWrapper returnValue) {
                        logger.info(String.format("host[%s] start collectd_exporter success", hostUuid));
                        trigger.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        logger.error(String.format("host[%s] start collectd_exporter fail, %s", hostUuid, errorCode.getDetails()));
                        trigger.fail(errorCode);
                    }
                });
            }
        };
    }

    public String getPushGatewayStartupArguments() {
        String dataFile = ZWatchGlobalProperty.KVMHOST_PUSHGATEWAY_PERSISTENCE_FILE;
        if (dataFile.equals("AUTO")) {
            dataFile =  PathUtil.join(CoreGlobalProperty.DATA_DIR, "/prometheus/host_pushgateway/persistence.data");
        }

        List<String> args = new ArrayList<>();

        args.add("--persistence.file");
        args.add(dataFile);

        args.add("--persistence.interval");
        args.add(ZWatchGlobalProperty.KVMHOST_PUSHGATEWAY_PERSISTENCE_INTERVAL);

        args.add("--web.listen-address");
        args.add(":" + ZWatchGlobalProperty.KVMHOST_PUSHGATEWAY_LISTEN_PORT);

        args.add("--metric.timetolive");
        args.add(ZWatchGlobalProperty.KVMHOST_PUSHGATEWAY_METRIC_TIME_TO_LIVE);

        return StringUtils.join(args, ' ');
    }

    public String getIpmiToolStartupArguments() {
        List<String> args = new ArrayList<>();
        args.add("--config.file=" + PathUtil.join(AnsibleGlobalProperty.ZSTACK_ROOT, "kvm") + "/ipmi.yml");
        args.add(String.format("--web.listen-address=:%s", ZWatchGlobalProperty.KVMHOST_IPMI_LISTEN_PORT));
        return StringUtils.join(args, ' ');
    }


    @Override
    public void preDeleteHost(HostInventory inventory) throws HostException {
        // do nothing
    }

    @Override
    public void beforeDeleteHost(HostInventory inventory) {
        // do nothing
    }

    @Override
    public void afterDeleteHost(HostInventory inventory) {
        pscMgr.deleteHostConfig(inventory.getUuid());
    }
}
