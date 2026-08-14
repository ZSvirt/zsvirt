package org.zstack.pluginpremium.externalapiadapter.api.ecs.instance

import org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants
import org.zstack.pluginpremium.externalapiadapter.api.BaseAsyncAPI
import org.zstack.sdk.ReimageVmInstanceAction
import org.zstack.sdk.StartVmInstanceAction

/**
 * Created by lining on 2018/4/15.
 */
class StartInstance extends BaseAsyncAPI<StartVmInstanceAction.Result> {

    @Override
    Class getZStackAction() {
        return StartVmInstanceAction.class
    }

    void configAPIConversionSpec() {
        spec = config {
            convertAPIParam {

                simpleConvert {
                    ecsParamName = ExternalAPIAdapterConstants.ECS_INSTANCE_ID
                    zstackParamName = ExternalAPIAdapterConstants.ZSTACK_UUID
                }

                simpleConvert {
                    ecsParamName = "InitLocalDisk"
                    putZstackParamValue = { zstackParamMap, zstackParamValue ->
                        def ecsParamValue = ecsAPIParamMap.get(ecsParamName)
                        if (ecsParamValue == "true") {
                            ReimageVmInstanceAction action = new ReimageVmInstanceAction()
                            action.sessionId = sessionId
                            action.vmInstanceUuid = ecsAPIParamMap.get(ExternalAPIAdapterConstants.ECS_INSTANCE_ID)

                            ReimageVmInstanceAction.Result result = action.call()
                            result.throwExceptionIfError()
                        }
                        return null
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
