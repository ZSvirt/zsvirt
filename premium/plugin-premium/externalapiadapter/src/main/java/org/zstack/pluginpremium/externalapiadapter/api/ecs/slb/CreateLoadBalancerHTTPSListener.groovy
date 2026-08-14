package org.zstack.pluginpremium.externalapiadapter.api.ecs.slb

import org.zstack.pluginpremium.externalapiadapter.convert.param.ParameterConversionUtils

import static org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants.ECS_SLB_LISTENER_HEALTH_CHECK_TYPE_HTTP

/**
 * Created by Qi Le on 2019-07-26
 */
class CreateLoadBalancerHTTPSListener extends LoadBalancerListenerBase {
    private static final String protocol = "https"

    @Override
    String convertHealthCheckType(Map ecsParamMap, Map zstackParamMap) {
        ParameterConversionUtils.processHttpListener(ecsParamMap, zstackParamMap)
        return ECS_SLB_LISTENER_HEALTH_CHECK_TYPE_HTTP
    }
}
