package org.zstack.pluginpremium.externalapiadapter.api.ecs.slb


import org.zstack.pluginpremium.externalapiadapter.api.BaseAPI
import org.zstack.pluginpremium.externalapiadapter.exception.APIParamConvertException
import org.zstack.sdk.DeleteLoadBalancerListenerAction
import org.zstack.sdk.LoadBalancerListenerInventory
import org.zstack.sdk.QueryLoadBalancerListenerAction

import static org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants.ZSTACK_UUID

/**
 * Created by Qi Le on 2019/10/9
 */
class DeleteLoadBalancerListener extends BaseAPI {
    @Override
    Class getZStackAction() {
        return DeleteLoadBalancerListenerAction.class
    }

    @Override
    protected void configAPIConversionSpec() {
        spec = config {
            convertAPIParam {
                zstackNeedParam {
                    zstackParamName = ZSTACK_UUID

                    getZstackValue = { Map ecsParamMap, zstackParamMap ->
                        QueryLoadBalancerListenerAction queryListener = new QueryLoadBalancerListenerAction(
                                sessionId: sessionId,
                                conditions: [
                                        "loadBalancerUuid=${ecsParamMap.get("LoadBalancerId")}".toString(),
                                        "loadBalancerPort=${ecsParamMap.get("ListenerPort")}".toString()
                                ]
                        )
                        if (ecsParamMap.containsKey("ListenerProtocol")) {
                            queryListener.conditions.add("protocol=${ecsParamMap.get("ListenerProtocol")}".toString())
                        }
                        QueryLoadBalancerListenerAction.Result listenerResult = queryListener.call()
                        listenerResult.throwExceptionIfError()
                        if (listenerResult.value.inventories.size() == 0) {
                            throw new APIParamConvertException("LoadBalancerId/ListenerPort",
                                    "Load balancer listener [LoadBalancerId:${ecsParamMap.get("LoadBalancerId")}, LoadBalancerPort:${ecsParamMap.get("ListenerPort")}] not found.".toString())
                        }
                        return ((LoadBalancerListenerInventory) listenerResult.value.inventories.get(0)).uuid
                    }
                }
            }

            convertAPIResponse {}
        }
    }
}
