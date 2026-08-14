package org.zstack.pluginpremium.externalapiadapter.api.ecs.instance

import org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants
import org.zstack.pluginpremium.externalapiadapter.api.BaseAsyncAPI
import org.zstack.sdk.RebootVmInstanceAction

class RebootInstance extends BaseAsyncAPI<RebootVmInstanceAction.Result> {
    @Override
    void configAPIConversionSpec() {
        spec = config {
            convertAPIParam {
                simpleConvert {
                    ecsParamName = ExternalAPIAdapterConstants.ECS_INSTANCE_ID
                    zstackParamName = ExternalAPIAdapterConstants.ZSTACK_UUID
                }
            }

            convertAPIResponse {}
        }
    }

    @Override
    Class getZStackAction() {
        return RebootVmInstanceAction.class
    }

    @Override
    int getAsyncWaitingTime() {
        return 1
    }
}
