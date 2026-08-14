package org.zstack.pluginpremium.externalapiadapter.api.ecs.vpc

import org.zstack.pluginpremium.externalapiadapter.api.BaseAPI
import org.zstack.pluginpremium.externalapiadapter.exception.APIAdapterSpecifiedErrorException
import org.zstack.sdk.AliyunProxyVSwitchInventory
import org.zstack.sdk.QueryAliyunProxyVSwitchAction
import org.zstack.sdk.UpdateL3NetworkAction

import static org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants.*

/**
 * Created by Qi Le on 2021/1/8
 */
class ModifyVSwitchAttribute extends BaseAPI {
    private static final String VSWITCH_NOT_FOUND_CODE = "InvalidVSwitchId.NotFound"
    private static final String VSWITCH_NOT_FOUND_MESSAGE = "The specified virtual switch does not exists."

    @Override
    Class getZStackAction() {
        return UpdateL3NetworkAction.class
    }

    @Override
    protected void configAPIConversionSpec() {
        spec = config {
            convertAPIParam {
                simpleConvert {
                    ecsParamName = ECS_VPC_VSWITCH_ID
                    zstackParamName = ZSTACK_UUID

                    putZstackParamValue = { Map zstackParamMap, String ecsParamValue ->
                        QueryAliyunProxyVSwitchAction queryVSW = new QueryAliyunProxyVSwitchAction(
                                sessionId: sessionId,
                                conditions: ["uuid=$ecsParamValue".toString()]
                        )
                        QueryAliyunProxyVSwitchAction.Result vswRes = queryVSW.call()
                        if (vswRes.error != null || vswRes.value.inventories.isEmpty()) {
                            throw new APIAdapterSpecifiedErrorException(VSWITCH_NOT_FOUND_CODE, VSWITCH_NOT_FOUND_MESSAGE)
                        }
                        AliyunProxyVSwitchInventory vsw = vswRes.value.inventories.first()
                        zstackParamMap[zstackParamName] = vsw.vpcL3NetworkUuid
                    }
                }

                simpleConvert {
                    ecsParamName = ECS_VPC_VSWITCH_NAME
                    zstackParamName = ZSTACK_NAME
                }

                simpleConvert {
                    ecsParamName = ECS_API_DESCRIPTION_KEY
                    zstackParamName = ZSTACK_API_DESCRIPTION_KEY
                }
            }

            convertAPIResponse {}
        }
    }
}
