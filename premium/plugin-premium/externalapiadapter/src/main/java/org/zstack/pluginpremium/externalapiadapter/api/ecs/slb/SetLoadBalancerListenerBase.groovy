package org.zstack.pluginpremium.externalapiadapter.api.ecs.slb


import org.zstack.pluginpremium.externalapiadapter.exception.APIParamConvertException
import org.zstack.sdk.ChangeLoadBalancerListenerAction
import org.zstack.sdk.LoadBalancerListenerInventory
import org.zstack.sdk.QueryLoadBalancerListenerAction
import org.zstack.sdk.UpdateLoadBalancerListenerAction

import static org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants.*
/**
 * Created by Qi Le on 2019-07-29
 */
abstract class SetLoadBalancerListenerBase extends LoadBalancerListenerBase {

    private static final String protocol = "undefined"

    @Override
    Class getZStackAction() {
        return UpdateLoadBalancerListenerAction.class
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
                                        "loadBalancerPort=${ecsParamMap.get("ListenerPort")}".toString(),
                                        "protocol=${protocol}".toString()
                                ]
                        )
                        QueryLoadBalancerListenerAction.Result listenerResult = queryListener.call()
                        listenerResult.throwExceptionIfError()
                        if (listenerResult.value.inventories.size() == 0) {
                            throw new APIParamConvertException("LoadBalancerId/ListenerPort",
                                    "Load balancer ${protocol} listener [LoadBalancerId:${ecsParamMap.get("LoadBalancerId")}, LoadBalancerPort:${ecsParamMap.get("ListenerPort")}] not found.".toString())
                        }
                        return ((LoadBalancerListenerInventory) listenerResult.value.inventories.get(0)).uuid
                    }
                }

                simpleConvert {
                    ecsParamName = ECS_API_DESCRIPTION_KEY
                    zstackParamName = ZSTACK_API_DESCRIPTION_KEY
                }
            }

            convertAPIResponse {}
        }
    }

    @Override
    boolean setChangeListenerParam(ChangeLoadBalancerListenerAction action) {
        boolean needDoAction = super.setChangeListenerParam(action)
        boolean needDoActionThis = false
        String uri = ecsAPIParamMap[ECS_SLB_LISTENER_HEALTH_CHECK_URI]
        if (uri != null) {
            action.healthCheckURI = uri
            needDoActionThis = true
        }
        String code = ecsAPIParamMap[ECS_SLB_LISTENER_HEALTH_CHECK_HTTP_CODE]
        if (code != null) {
            action.healthCheckHttpCode = code
            needDoActionThis = true
        }
        return needDoActionThis || needDoAction
    }

    @Override
    void addBackendServersToListener(String listenerUuid) {
        //do nothing
    }
}
