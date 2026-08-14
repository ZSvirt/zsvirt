package org.zstack.premium.externalservice.prometheus;

import org.zstack.core.GlobalProperty;
import org.zstack.core.GlobalPropertyDefinition;

/**
 * Created by xing5 on 2016/7/6.
 */
@GlobalPropertyDefinition
public class PrometheusGlobalProperty {
    @GlobalProperty(name="Prometheus.webListenAddress", defaultValue = "AUTO")
    public static String LISTEN_ADDRESS;
    @GlobalProperty(name="Prometheus.remoteWrite.url", defaultValue = "")
    public static String REMOTE_WRITE_URL;
    @GlobalProperty(name="Prometheus.port", defaultValue = "9090")
    public static int PORT;
    @GlobalProperty(name="Prometheus.readonly.port", defaultValue = "9089")
    public static int READONLY_PORT;
    @GlobalProperty(name="Prometheus.remoteRead.enable", defaultValue = "false")
    public static boolean ENABLE_REMOTE_READ;
    @GlobalProperty(name="Prometheus.recording.infoLabelEnrich.enable", defaultValue = "false")
    public static boolean ENABLE_INFO_LABEL_ENRICHMENT;
    @GlobalProperty(name="Prometheus.serverPath", defaultValue = "AUTO")
    public static String SERVER_PATH;
    @GlobalProperty(name="Prometheus.scrapeInterval", defaultValue = "15")
    public static int SCRAPE_INTERVAL;
    @GlobalProperty(name="Prometheus.evaluationInterval", defaultValue = "15")
    public static int EVALUATION_INTERVAL;
    @GlobalProperty(name="Prometheus.queryMaxConcurrency", defaultValue = "20")
    public static int MAX_CONCURRENCY;
    @GlobalProperty(name="Prometheus.queryTimeout", defaultValue = "2m0s")
    public static String QUERY_TIMEOUT;
    @GlobalProperty(name="Prometheus.storageLocalPath", defaultValue = "AUTO")
    public static String STORAGE_LOCAL_PATH;
    @GlobalProperty(name="deleteOldZstackAgentProcess", defaultValue = "false")
    public static boolean DELETE_OLD_ZSTACK_AGENT_PROCESS;
    @GlobalProperty(name="Prometheus.startupTimeout", defaultValue = "20")
    public static int STARTUP_TIMEOUT;
    @GlobalProperty(name="Prometheus.exporterPort", defaultValue = "8081")
    public static int EXPORTER_PORT;
    @GlobalProperty(name="Prometheus.zsha2ExporterPort", defaultValue = "18081")
    public static int ZSHA2_EXPORTER_PORT;
    @GlobalProperty(name="Prometheus.retention", defaultValue = "8760h0m0s")
    public static String RETENTION;
    @GlobalProperty(name="Prometheus.heapSize", defaultValue = "AUTO")
    public static String HEAP_SIZE;
    @GlobalProperty(name="Prometheus.useClusterRead", defaultValue = "false")
    public static boolean CLUSTER_READ;
    /**
     * available values are 2.x, none
     * (1.8.2 and 2.x-compatible is prohibited)
     *
     * 1.8.2 means only using 1.8.2       (Prohibited in ZSphere)
     * 2.x means only using 2.x
     * 2.x-compatible means using 2.x as primary and start 1.8.2 as readonly instance,
     * for keeping back-compatibility     (Prohibited in ZSphere)
     * none means stop prometheus service
     */
    @GlobalProperty(name="Prometheus.versionMode", defaultValue = "2.x")
    public static String VERSION_MODE;
    @GlobalProperty(name="Support.thanos", defaultValue = "false")
    public static boolean SUPPORT_THANOS;
    @GlobalProperty(name="Thanos.query.port", defaultValue = "11901")
    public static int THANOS_QUERY_PORT;

    public static int getPrometheusPort() {
        if (SUPPORT_THANOS) {
            return THANOS_QUERY_PORT;
        }
        return PORT;
    }

    /**
     * blacklist, use 'namespace1,namespace2', e.g: "Host,Vm" means 'Host' and 'Vm' is skipped
     */
    @GlobalProperty(name="evaluation.blacklist")
    public static String PROMETHEUS_EVALUATION_BLACKLIST;
}
