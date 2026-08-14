package org.zstack.premium.externalservice.prometheus;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.Platform;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.config.GlobalConfigException;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.externalservice.ExternalServiceFactory;
import org.zstack.core.externalservice.ExternalServiceManager;
import org.zstack.core.externalservice.ExternalServiceType;
import org.zstack.core.thread.PeriodicTask;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.Component;
import org.zstack.header.core.BypassWhenUnitTest;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.managementnode.ManagementNodeChangeListener;
import org.zstack.header.managementnode.ManagementNodeInventory;
import org.zstack.header.managementnode.ManagementNodeVO;
import org.zstack.utils.*;
import org.zstack.utils.data.NumberUtils;
import org.zstack.utils.data.SizeUnit;
import org.zstack.utils.data.UnitNumber;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.path.PathUtil;
import org.zstack.utils.zsha2.ZSha2Helper;

import javax.persistence.Query;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class PrometheusFactory implements ExternalServiceFactory, Component, ManagementNodeChangeListener {
    private final static CLogger logger = Utils.getLogger(PrometheusFactory.class);

    public static final ExternalServiceType type = new ExternalServiceType("Prometheus");

    @Autowired
    private ExternalServiceManager manager;
    @Autowired
    private PluginRegistry pluginRegistry;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ThreadFacade thdf;

    @Override
    public String getExternalServiceType() {
        return type.toString();
    }

    private Prometheus instance;
    private final List<Prometheus> needCloseInstances = new ArrayList<>();

    private final Map<String, NodeInfo> remoteNodeInfoMap = new ConcurrentHashMap<>();

    public Prometheus getPrometheus() {
        if (instance == null && !CoreGlobalProperty.UNIT_TEST_ON) {
            throw new CloudRuntimeException("no prometheus instance has been created");
        }

        if (PrometheusGlobalProperty.CLUSTER_READ) {
            return new MultiNodePrometheus(instance, remoteNodeInfoMap.values().stream().map(NodeInfo::getHostname).collect(Collectors.toList()));
        } else {
            return instance;
        }
    }

    private long getHeapSize() {
        long size;
        if ("AUTO".equals(PrometheusGlobalProperty.HEAP_SIZE)) {
            size = calculateHeapSizeBySystemMemory();
        } else {
            size = SizeUtils.sizeStringToBytes(PrometheusGlobalProperty.HEAP_SIZE);
        }

        if (size < SizeUnit.MEGABYTE.toByte(512)) {
            throw new CloudRuntimeException(String.format("too small heap size[%s] for prometheus, please set Prometheus.heapSize larger than 512m in zstack.properties",
                    PrometheusGlobalProperty.HEAP_SIZE));
        }

        return size;
    }

    private long calculateHeapSizeBySystemMemory() {
        ShellResult ret = ShellUtils.runAndReturn("free -b | awk '/^Mem:/{print $2}'");
        ret.raiseExceptionIfFail();
        long total = Long.parseLong(ret.getStdout().trim().replaceAll("\n", "")
                .replaceAll("\r", "").replaceAll("\t", ""));
        if (total <= SizeUnit.GIGABYTE.toByte(6)) {
            return SizeUnit.MEGABYTE.toByte(512);
        } else if (total <= SizeUnit.GIGABYTE.toByte(8)) {
            return SizeUnit.GIGABYTE.toByte(1);
        } else if (total <= SizeUnit.GIGABYTE.toByte(16)) {
            return SizeUnit.GIGABYTE.toByte(3);
        } else if (total <= SizeUnit.GIGABYTE.toByte(32)) {
            return SizeUnit.GIGABYTE.toByte(6);
        } else {
            return SizeUnit.GIGABYTE.toByte(8);
        }
    }

    private String toHoursString(long month) {
        return month * TimeUnit.DAYS.toHours(30) + "h";
    }

    private void initialPrometheus2() {
        PrometheusGlobalConfig.STORAGE_LOCAL_RETENTION_SIZE.installValidateExtension((category, name, oldValue, newValue) -> {
            UnitNumber numeric = NumberUtils.ofUnitNumber(newValue);

            if (numeric == null || !SizeUtils.isSize2(numeric)) {
                throw new GlobalConfigException(String.format("%s is not a size string; a size string consists of a number ending with suffix KB/MB/GB/TB or without suffix; for example, 64GB", newValue));
            }
        });
        startPrometheus2();
    }

    @BypassWhenUnitTest
    private void startPrometheus2() {
        PrometheusGlobalConfig.STORAGE_LOCAL_RETENTION.installUpdateExtension((oldConfig, newConfig) -> {
            Prometheus prometheus = getPrometheus();
            prometheus.getParams().setRetention(toHoursString(newConfig.value(Long.class)));
            prometheus.restart();
        });

        PrometheusGlobalConfig.STORAGE_LOCAL_RETENTION_SIZE.installUpdateExtension((oldConfig, newConfig) -> {
            Prometheus prometheus = getPrometheus();
            prometheus.getParams().setRetentionSize(newConfig.value(String.class));
            prometheus.restart();
        });

        PrometheusGlobalConfig.WAL_MINIMUM_DURATION_BEFORE_PERSISTED.installUpdateExtension((oldConfig, newConfig) -> {
            Prometheus prometheus = getPrometheus();
            prometheus.getParams().setWalBlockMinRetention(newConfig.value(String.class));
            prometheus.restart();
        });

        PrometheusGlobalConfig.ENABLE_BASIC_AUTH.installUpdateExtension((oldConfig, newConfig) -> {
            Prometheus prometheus = getPrometheus();
            prometheus.getParams().setBasicAuthSwitch(newConfig.value(Boolean.class));
            executeBasicAuthConfigBash(prometheus.getParams());
            prometheus.restart();
        });

        PrometheusGlobalConfig.BASIC_AUTH_USERNAME.installUpdateExtension((oldConfig, newConfig) -> {
            Prometheus prometheus = getPrometheus();
            prometheus.getParams().setBasicAuthUsername(newConfig.value(String.class));
            executeBasicAuthConfigBash(prometheus.getParams());
            prometheus.restart();
        });

        PrometheusGlobalConfig.BASIC_AUTH_PASSWORD.installUpdateExtension((oldConfig, newConfig) -> {
            Prometheus prometheus = getPrometheus();
            prometheus.getParams().setBasicAuthPassword(newConfig.value(String.class));
            executeBasicAuthConfigBash(prometheus.getParams());
            prometheus.restart();
        });

        PrometheusParam param = new PrometheusParam();

        param.setBinaryPath(PathUtil.findFileOnClassPath(Prometheus.SERVER2_PATH, true).getAbsolutePath());
        param.setIp(Platform.getManagementServerIp());
        param.setPort(PrometheusGlobalProperty.PORT);
        param.setStoragePath(Prometheus.DATA_DIR_2);
        param.setServicePath(PrometheusConstant.SERVICE2_FILE_PATH);
        param.setRetention(toHoursString(PrometheusGlobalConfig.STORAGE_LOCAL_RETENTION.value(Long.class)));
        param.setRetentionSize(PrometheusGlobalConfig.STORAGE_LOCAL_RETENTION_SIZE.value(String.class));
        param.setWalBlockMinRetention(PrometheusGlobalConfig.WAL_MINIMUM_DURATION_BEFORE_PERSISTED.value(String.class));
        param.setConfPath(PathUtil.join(Prometheus.HOME_DIR, "conf.yaml"));
        param.setLogPath(Prometheus.LOG_PATH_2);
        param.setStartUpTimeout(PrometheusGlobalProperty.STARTUP_TIMEOUT);
        List<String> parameters = new ArrayList<>();
        parameters.add(String.format("--query.max-concurrency %s", PrometheusGlobalProperty.MAX_CONCURRENCY));
        parameters.add(String.format("--query.timeout %s", getQueryTimeout()));
        param.setParameters(parameters);
        param.setBasicAuthSwitch(PrometheusGlobalConfig.ENABLE_BASIC_AUTH.value(Boolean.class));

        new Bash() {
            @Override
            protected void scripts() {
                mkdirs(dirname(param.getConfPath()));
                mkdirs(param.getStoragePath());
                mkdirs(dirname(param.getLogPath()));
            }
        }.execute();
        executeBasicAuthConfigBash(param);

        instance = new PrometheusImpl2(param);
        instance = (Prometheus) manager.getService(instance.getName(), () -> instance);
    }

    private void executeBasicAuthConfigBash(PrometheusParam param) {
        new Bash() {
            @Override
            protected void scripts() {
                String basicAuthConfFullPath = PathUtil.join(Prometheus.HOME_DIR, "basic_auth_conf.yaml");
                String basicAuthParamStr = String.format("--web.config.file=%s", basicAuthConfFullPath);
                if (param.getBasicAuthSwitch()) {
                    // write basic auth info
                    // https://prometheus.io/docs/guides/basic-auth/#securing-prometheus-api-and-ui-endpoints-using-basic-auth
                    sudoRun("htpasswd -nb -B %s %s | sed 's/\\(:\\)\\(.*\\)/\\1 \\2/'", PrometheusGlobalConfig.BASIC_AUTH_USERNAME.value(), PrometheusGlobalConfig.BASIC_AUTH_PASSWORD.value());
                    if (lastReturnCode == 0) {
                        param.getParameters().add(basicAuthParamStr);

                        /**
                         * basic_auth_users:
                         *   admin: $2y$10$EYZguU5VrQNgboW8oXBPHuLLFxWj76mfXFyNHRuLTKmnygx5CzcV.
                         */
                        String content = "basic_auth_users:\n";
                        content += "  " + stdout();
                        writeFile(basicAuthConfFullPath, content);
                    } else {
                        logger.warn(String.format("executeBasicAuthConfigBash failed, because htpasswd command failed: %s", stderr()));
                    }
                } else {
                    param.getParameters().remove(basicAuthParamStr);
                }
            }
        }.execute();
    }

    @BypassWhenUnitTest
    private void closePrometheus2() {
        PrometheusParam param = new PrometheusParam();
        param.setBinaryPath(PathUtil.findFileOnClassPath(Prometheus.SERVER2_PATH, true).getAbsolutePath());
        param.setServicePath(PrometheusConstant.SERVICE2_FILE_PATH);

        Prometheus needCloseInstance = new PrometheusImpl2(param);
        needCloseInstances.add((Prometheus) manager.getService(needCloseInstance.getName(), () -> needCloseInstance));
    }

    private String getQueryTimeout() {
        String timeout = PrometheusGlobalProperty.QUERY_TIMEOUT;
        return timeout.substring(0, timeout.indexOf("m") + 1);
    }

    @Override
    public boolean start() {
        String mode = PrometheusGlobalProperty.VERSION_MODE;
        switch (mode) {
            case "2.x": {
                initialPrometheus2();
                break;
            }
            case "none": {
                closePrometheus2();
                break;
            }
            default: {
                throw new CloudRuntimeException(String.format("invalid value [%s] of GlobalProperty Prometheus.versionMode has been found!", mode));
            }
        }

        startPrometheus(mode);
        return true;
    }

    @BypassWhenUnitTest
    private void startPrometheus(String mode) {
        if (!needCloseInstances.isEmpty()) {
            needCloseInstances.forEach(Prometheus::stop);
        }

        if (instance != null) {
            pluginRegistry.getExtensionList(PreparePrometheusConfigExtensionPoint.class).forEach(ext -> ext.prepareConfig(instance.getConfig()));
            instance.tolerateFailureStart();
        }

        startCleanStalePrometheusRemoteNodeInfoTask();
    }

    private void startCleanStalePrometheusRemoteNodeInfoTask() {
        thdf.submitPeriodicTask(new PeriodicTask() {
            @Override
            public TimeUnit getTimeUnit() {
                return TimeUnit.SECONDS;
            }

            @Override
            public long getInterval() {
                return 10;
            }

            @Override
            public String getName() {
                return "clean-stale-prometheus-remote-node-info";
            }

            @Override
            public void run() {
                remoteNodeInfoMap.entrySet().removeIf(entry -> entry.getValue() == null);
                List<Map.Entry<String, NodeInfo>> staleNodes = CollectionUtils.filter(remoteNodeInfoMap.entrySet(),
                        entry -> entry.getValue().getExpiredDate() != null);
                if (staleNodes.isEmpty()) {
                    return;
                }

                Query query = dbf.getEntityManager().createNativeQuery("select current_timestamp()");
                Timestamp ts = (Timestamp) query.getSingleResult();
                for (Map.Entry<String, NodeInfo> entry : staleNodes) {
                    String nodeUuid = entry.getKey();

                    if (dbf.isExist(nodeUuid, ManagementNodeVO.class)) {
                        // prometheus remote node[%s] detected in DB, skip remove it
                        continue;
                    }

                    if (!ZSha2Helper.checkConnectivityOfPeerNode()) {
                        remoteNodeInfoMap.remove(nodeUuid);
                        logger.debug(String.format("Remove prometheus node[uuid=%s]: MN unreachable", nodeUuid));
                        continue;
                    }

                    if (entry.getValue().getExpiredDate().after(ts)) {
                        continue;
                    }

                    logger.debug(String.format("Remove prometheus node[uuid=%s]: Up to the expiration date", nodeUuid));
                    remoteNodeInfoMap.remove(nodeUuid);
                }
            }
        });
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public void nodeJoin(ManagementNodeInventory inv) {
        NodeInfo nodeInfo = new NodeInfo();
        nodeInfo.setHostname(inv.getHostName());
        remoteNodeInfoMap.put(inv.getUuid(), nodeInfo);
        logger.debug(String.format("Management node[uuid=%s] joins, add remote node info", inv.getUuid()));
    }

    @Override
    public void nodeLeft(ManagementNodeInventory inv) {
        NodeInfo nodeInfo = remoteNodeInfoMap.get(inv.getUuid());

        if (nodeInfo == null) {
            return;
        }

        Query query = dbf.getEntityManager().createNativeQuery("select current_timestamp()");
        Timestamp ts = (Timestamp) query.getSingleResult();
        nodeInfo.setExpiredDate(new Timestamp(TimeUnit.MINUTES.toMillis(30) + ts.getTime()));
        logger.debug(String.format("Management node[uuid=%s] left, mark remote node info expired after 30min", inv.getUuid()));
    }

    @Override
    public void iAmDead(ManagementNodeInventory inv) {

    }

    @Override
    public void iJoin(ManagementNodeInventory inv) {

    }
}
