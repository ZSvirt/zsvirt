package org.zstack.premium.externalservice.prometheus;

/**
 * Created by xing5 on 2016/7/9.
 */
public interface PreparePrometheusConfigExtensionPoint {
     void prepareConfig(PrometheusConfig config);
}
