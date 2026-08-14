package org.zstack.zwatch.prometheus;

import org.zstack.premium.externalservice.prometheus.PrometheusConfig;
import org.zstack.zwatch.ZWatchGlobalProperty;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;

public interface PrometheusStaticConfigManager {
    void writeHostConfig(String hostUuid, String ip, List<PrometheusConfig.StaticConfig> config, String directory);

    void deleteHostConfig(String hostUuid);

    void writeVRConfig(String vrUuid, String mgmtIp, PrometheusConfig.StaticConfig config);

    void deleteVRConfig(String vrUuid);

    void writeBackupStorageConfig(String bsUuid, String bsIp, PrometheusConfig.StaticConfig config);

    void deleteBackupStorageConfig(String bsUuid);

    void writeBaremetaPxeServerConfig(String uuid, String ip, PrometheusConfig.StaticConfig config);

    static PrometheusConfig.StaticConfig createVirtualRouterConfig(String vrUuid, String mgmtIp) {
        PrometheusConfig.StaticConfig sc = new PrometheusConfig.StaticConfig();
        HashSet<String> targets = new HashSet<>();
        targets.add(String.format("%s:7272", mgmtIp));
        sc.targets = targets;
        sc.labels = map(
                e("vrouter", vrUuid)
        );

        return sc;
    }

    static PrometheusConfig.StaticConfig createBackupStorageConfig(String bsUuid, String bsIp) {
        PrometheusConfig.StaticConfig sc = new PrometheusConfig.StaticConfig();
        HashSet<String> targets = new HashSet<>();
        targets.add(String.format("%s:%s", bsIp, ImageStoreScrape.WEB_PORT));
        sc.targets = targets;
        sc.labels = map(
                e("backupStorageUuid", bsUuid)
        );

        return sc;
    }

    static List<PrometheusConfig.StaticConfig> createHostConfig(String hostUuid, String hostIp) {
        List<PrometheusConfig.StaticConfig> configs = new ArrayList<>();

        PrometheusConfig.StaticConfig sc = new PrometheusConfig.StaticConfig();
        HashSet<String> targets = new HashSet<>();
        targets.add(String.format("%s:9103", hostIp));
        targets.add(String.format("%s:9100", hostIp));
        targets.add(String.format("%s:7069", hostIp));
        sc.targets = targets;
        sc.labels = map(
                e("hostUuid", hostUuid)
        );
        configs.add(sc);

        sc = new PrometheusConfig.StaticConfig();
        HashSet<String> target = new HashSet<>();
        target.add(String.format("%s:%s", hostIp, ZWatchGlobalProperty.KVMHOST_PUSHGATEWAY_LISTEN_PORT));
        sc.targets = target;
        sc.labels = map();
        configs.add(sc);

        return configs;
    }
}
