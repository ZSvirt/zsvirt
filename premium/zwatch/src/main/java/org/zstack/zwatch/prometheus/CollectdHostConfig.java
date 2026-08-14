package org.zstack.zwatch.prometheus;

import org.zstack.premium.externalservice.prometheus.Prometheus;
import org.zstack.premium.externalservice.prometheus.PrometheusConfig;
import org.zstack.utils.path.PathUtil;
import org.zstack.zwatch.ZWatchGlobalProperty;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;

/**
 * @Author: qiuyu.zhang
 * @Date: 2024/5/20 16:58
 */
public class CollectdHostConfig implements HostConfigExtensionPoint {

    @Override
    public List<PrometheusConfig.StaticConfig> getHostConfig(String hostUuid, String hostIp) {
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

    @Override
    public String getHostConfigPath() {
        return PathUtil.join(Prometheus.DISCORVERY_ROOT, "hosts");
    }
}
