package org.zstack.pluginpremium.externalapiadapter.api.ecs.slb


import org.zstack.pluginpremium.externalapiadapter.api.BaseAPI
import org.zstack.sdk.UpdateLoadBalancerAction

import static org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants.ZSTACK_NAME
import static org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants.ZSTACK_UUID

/**
 * Created by Qi Le on 2019/10/9
 */
class SetLoadBalancerName extends BaseAPI {
    @Override
    Class getZStackAction() {
        return UpdateLoadBalancerAction.class
    }

    @Override
    protected void configAPIConversionSpec() {
        spec = config {
            convertAPIParam {
                simpleConvert {
                    ecsParamName = "LoadBalancerId"
                    zstackParamName = ZSTACK_UUID
                }

                simpleConvert {
                    ecsParamName = "LoadBalancerName"
                    zstackParamName = ZSTACK_NAME
                }
            }

            convertAPIResponse {}
        }
    }
}
