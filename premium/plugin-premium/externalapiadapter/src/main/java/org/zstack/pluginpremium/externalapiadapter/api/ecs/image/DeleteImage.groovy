package org.zstack.pluginpremium.externalapiadapter.api.ecs.image


import org.zstack.pluginpremium.externalapiadapter.api.BaseAPI
import org.zstack.sdk.DeleteImageAction

import static org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants.ECS_IMAGE_ID
import static org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants.ZSTACK_UUID

/**
 * @Author: fubang* @Date: 2018/4/25
 */
class DeleteImage extends BaseAPI {
    @Override
    void configAPIConversionSpec() {
        spec = config {
            convertAPIParam {
                simpleConvert {
                    ecsParamName = ECS_IMAGE_ID
                    zstackParamName = ZSTACK_UUID
                }
            }
            convertAPIResponse {

            }
        }
    }

    @Override
    Class getZStackAction() {
        return DeleteImageAction.class
    }
}
