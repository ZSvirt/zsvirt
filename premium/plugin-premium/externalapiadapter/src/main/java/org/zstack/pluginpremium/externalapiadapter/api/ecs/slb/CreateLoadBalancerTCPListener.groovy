package org.zstack.pluginpremium.externalapiadapter.api.ecs.slb

import org.zstack.pluginpremium.externalapiadapter.convert.param.ParameterConversionUtils

import static org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants.*

/**
 * Created by Qi Le on 2019-07-26
 */
class CreateLoadBalancerTCPListener extends LoadBalancerListenerBase {
    private static final String protocol = "tcp"

    @Override
    String convertHealthCheckType(Map ecsParamMap, Map zstackParamMap) {
        if (ecsParamMap[ECS_SLB_LISTENER_HEALTH_CHECK_TYPE] == ECS_SLB_LISTENER_HEALTH_CHECK_TYPE_HTTP) {
            ParameterConversionUtils.processHttpListener(ecsParamMap, zstackParamMap)
            return ECS_SLB_LISTENER_HEALTH_CHECK_TYPE_HTTP
        } else {
            return ECS_SLB_LISTENER_HEALTH_CHECK_TYPE_TCP
        }
    }
}
