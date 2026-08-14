package org.zstack.pluginpremium.externalapiadapter.api.ecs.slb

/**
 * Created by Qi Le on 2019-07-27
 */
class CreateLoadBalancerUDPListener extends LoadBalancerListenerBase {
    private static final String protocol = "udp"

    @Override
    String convertHealthCheckType(Map ecsParamMap, Map zstackParamMap) {
        return protocol
    }
}
