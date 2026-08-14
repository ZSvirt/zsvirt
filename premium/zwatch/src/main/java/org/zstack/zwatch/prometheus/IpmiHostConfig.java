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
 * @Date: 2024/5/21 12:02
 */
public class IpmiHostConfig implements HostConfigExtensionPoint {
    @Override
    public List<PrometheusConfig.StaticConfig> getHostConfig(String hostUuid, String ip) {
        List<PrometheusConfig.StaticConfig> configs = new ArrayList<>();

        PrometheusConfig.StaticConfig sc = new PrometheusConfig.StaticConfig();
        HashSet<String> targets = new HashSet<>();
        targets.add(String.format("%s:%s", ip, ZWatchGlobalProperty.KVMHOST_IPMI_LISTEN_PORT));
        sc.targets = targets;
        sc.labels = map(
                e("hostUuid", hostUuid)
        );
        configs.add(sc);

        return configs;
    }

    @Override
    public String getHostConfigPath() {
        return PathUtil.join(Prometheus.DISCORVERY_ROOT, "ipmi");
    }
}
