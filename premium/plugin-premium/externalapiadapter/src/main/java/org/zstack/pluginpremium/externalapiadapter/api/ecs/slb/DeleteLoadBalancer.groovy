package org.zstack.pluginpremium.externalapiadapter.api.ecs.slb


import org.zstack.pluginpremium.externalapiadapter.api.BaseAsyncAPI
import org.zstack.pluginpremium.externalapiadapter.exception.InvalidParameterException
import org.zstack.sdk.DeleteVipAction
import org.zstack.sdk.ErrorCode
import org.zstack.sdk.LoadBalancerInventory
import org.zstack.sdk.QueryLoadBalancerAction

import static org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants.ECSErrorCode.InvalidParameter
import static org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants.ZSTACK_UUID

/**
 * Created by Qi Le on 2019-07-26
 */
class DeleteLoadBalancer extends BaseAsyncAPI {
    @Override
    Class getZStackAction() {
        return DeleteVipAction.class
    }

    @Override
    protected void configAPIConversionSpec() {
        spec = config {
            convertAPIParam {
                complexConvert {
                    ecsParamName = "LoadBalancerId"
                    zstackParamName = ZSTACK_UUID

                    getZstackValue = { String loadBalancerId ->
                        QueryLoadBalancerAction qAction = new QueryLoadBalancerAction(
                                sessionId: sessionId,
                                conditions: ["uuid=${loadBalancerId}".toString()]
                        )
                        QueryLoadBalancerAction.Result qResult = qAction.call()
                        qResult.throwExceptionIfError()
                        if (qResult.value.inventories.size() == 0) {
                            throw new InvalidParameterException("LoadBalancerId", new ErrorCode(
                                    code: InvalidParameter,
                                    details: "LoadBalancer [id:${loadBalancerId}] not found.".toString()))
                        }
                        return (qResult.value.inventories.first() as LoadBalancerInventory).vipUuid
                    }
                }
            }

            convertAPIResponse {}
        }
    }
}
