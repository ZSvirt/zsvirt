package org.zstack.pluginpremium.externalapiadapter.api.ecs.instance

import org.zstack.pluginpremium.externalapiadapter.api.BaseAsyncAPI
import org.zstack.pluginpremium.externalapiadapter.exception.APIParamConvertException
import org.zstack.sdk.StopVmInstanceAction

import static org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants.*

class StopInstance extends BaseAsyncAPI<StopVmInstanceAction.Result> {

    @Override
    Class getZStackAction() {
        return StopVmInstanceAction.class
    }

    void configAPIConversionSpec() {
        spec = config {
            convertAPIParam {

                simpleConvert {
                    ecsParamName = ECS_INSTANCE_ID
                    zstackParamName = ZSTACK_UUID
                }

                zstackNeedParam {
                    zstackParamName = "stopHA"
                    getZstackValue = { Map ecsParamMap, Map zstackParamMap ->
                        return "true"
                    }
                }

                simpleConvert {
                    ecsParamName = "ForceStop"
                    zstackParamName = ZSTACK_API_TYPE_KEY
                    putZstackParamValue = { zstackParamMap, zstackParamValue ->
                        def tmp = null
                        if (zstackParamValue == "true") {
                            tmp = "cold"
                        } else if (zstackParamValue == "false")
                            tmp = "grace"

                        if (tmp == null){
                            throw new APIParamConvertException(ecsParamName, "${ecsParamName}[value: $zstackParamValue] is not valid".toString())
                        }
                        zstackParamMap.put(zstackParamName, tmp)
                    }
                }
            }

            convertAPIResponse {}
        }
    }

    @Override
    int getAsyncWaitingTime() {
        return 1
    }
}
