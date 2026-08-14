package org.zstack.pluginpremium.externalapiadapter.api.ecs.eip

import org.zstack.pluginpremium.externalapiadapter.api.BaseAPI
import org.zstack.pluginpremium.externalapiadapter.exception.APIAdapterSpecifiedErrorException
import org.zstack.sdk.DeleteVipAction
import org.zstack.sdk.EipInventory
import org.zstack.sdk.QueryEipAction

import static org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants.ECS_EIP_ALLOCATION_ID
import static org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants.ZSTACK_UUID
/**
 * @Author: fubang* @Date: 2018/5/28
 */
class ReleaseEipAddress extends BaseAPI {
    static final String EIP_NOT_FOUND_CODE = "InvalidAllocationId.NotFound"
    static final String EIP_NOT_FOUND_MESSAGE = "Specified allocation ID is not found."

    @Override
    protected void configAPIConversionSpec() {
        spec = config {
            convertAPIParam {
                simpleConvert {
                    ecsParamName = ECS_EIP_ALLOCATION_ID
                    zstackParamName = ZSTACK_UUID

                    putZstackParamValue = {Map zstackParamMap, String ecsParamValue ->
                        QueryEipAction queryEip = new QueryEipAction(
                                sessionId: sessionId,
                                conditions: ["uuid=$ecsParamValue".toString()]
                        )
                        QueryEipAction.Result eipRes = queryEip.call()
                        if (eipRes.error != null || eipRes.value.inventories.isEmpty()) {
                            throw new APIAdapterSpecifiedErrorException(
                                    EIP_NOT_FOUND_CODE,
                                    EIP_NOT_FOUND_MESSAGE
                            )
                        }
                        EipInventory eip = eipRes.value.inventories.first()
                        zstackParamMap[zstackParamName] = eip.vipUuid
                    }
                }
            }

            convertAPIResponse {}
        }
    }

    @Override
    Class getZStackAction() {
        return DeleteVipAction.class
    }
}
