package org.zstack.pluginpremium.externalapiadapter.api.ecs.eip


import org.zstack.pluginpremium.externalapiadapter.api.BaseAPI
import org.zstack.sdk.DetachEipAction

import static org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants.ECS_EIP_ALLOCATION_ID
import static org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants.ZSTACK_UUID

/**
 * @Author: fubang* @Date: 2018/5/28
 */
class UnassociateEipAddress extends BaseAPI {
    @Override
    protected void configAPIConversionSpec() {
        spec = config {
            convertAPIParam {
                simpleConvert {
                    ecsParamName = ECS_EIP_ALLOCATION_ID
                    zstackParamName = ZSTACK_UUID
                }
            }

            convertAPIResponse {}
        }
    }

    @Override
    Class getZStackAction() {
        return DetachEipAction.class
    }
}
