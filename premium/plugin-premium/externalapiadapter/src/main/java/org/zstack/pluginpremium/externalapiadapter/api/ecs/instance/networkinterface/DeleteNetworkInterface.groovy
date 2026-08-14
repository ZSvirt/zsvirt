package org.zstack.pluginpremium.externalapiadapter.api.ecs.instance.networkinterface


import org.zstack.pluginpremium.externalapiadapter.api.BaseAPI
import org.zstack.sdk.DeleteVmNicAction

import static org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants.ZSTACK_UUID

/**
 * Created by lining on 2018/5/20.
 */
class DeleteNetworkInterface extends BaseAPI {
    @Override
    Class getZStackAction() {
        return DeleteVmNicAction.class
    }

    @Override
    void configAPIConversionSpec() {
        spec = config {
            convertAPIParam {
                simpleConvert {
                    ecsParamName = "NetworkInterfaceId"
                    zstackParamName = ZSTACK_UUID
                }
            }

            convertAPIResponse {
            }
        }
    }

}
