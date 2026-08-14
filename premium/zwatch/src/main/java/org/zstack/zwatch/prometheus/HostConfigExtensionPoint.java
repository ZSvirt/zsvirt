package org.zstack.zwatch.prometheus;

import org.zstack.premium.externalservice.prometheus.PrometheusConfig;

import java.util.List;

/**
 * @Author: qiuyu.zhang
 * @Date: 2024/5/20 16:49
 */
public interface HostConfigExtensionPoint {
    List<PrometheusConfig.StaticConfig> getHostConfig(String hostUuid, String ip);

    String getHostConfigPath();
}
