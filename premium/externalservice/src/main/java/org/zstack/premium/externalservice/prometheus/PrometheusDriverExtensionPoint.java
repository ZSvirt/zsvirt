package org.zstack.premium.externalservice.prometheus;

/**
 * Created by mingjian.deng on 2020/4/8.
 */
public interface PrometheusDriverExtensionPoint {
    void beforePrometheusApicall(String ip, String url);
}
