package org.zstack.pluginpremium.externalapiadapter.api.ecs.disk

import org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterUtils
import org.zstack.pluginpremium.externalapiadapter.api.BaseAsyncAPI
import org.zstack.sdk.ChangeVmImageAction

import static org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants.*

class ReplaceSystemDisk extends BaseAsyncAPI<ChangeVmImageAction.Result> {
    @Override
    void configAPIConversionSpec() {
        spec = config {
            convertAPIParam {
                simpleConvert {
                    ecsParamName = ECS_INSTANCE_ID
                    zstackParamName = ZSTACK_VM_INSTANCE_UUID
                }

                simpleConvert {
                    ecsParamName = ECS_IMAGE_ID
                    zstackParamName = ZSTACK_IMAGE_ID
                }

                zstackNeedParam {
                    zstackParamName = ZSTACK_RESOURCEUUID_KEY
                    getZstackValue = {Map ecsParamMap, Map zstackParamMap ->
                        String clientToken = ecsParamMap.get(ECS_API_CLIENTTOKEN_KEY)
                        return ExternalAPIAdapterUtils.randomUUID(clientToken)
                    }
                }
            }

            convertAPIResponse {
                convertResponseAttribute {
                    ecsAttributeName = ECS_DISK_ID

                    getZstackAttributeValue = {
                        return zstackAPIReq.get(ZSTACK_RESOURCEUUID_KEY)
                    }

                    addEcsValueToEcsAPIRsp = { ecsAPIRsp ->
                        ecsAPIRsp.put(ecsAttributeName, zstackAttributeValue)
                    }
                }
            }
        }
    }

    @Override
    Class getZStackAction() {
        return ChangeVmImageAction.class
    }
}
