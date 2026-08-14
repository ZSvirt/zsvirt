package org.zstack.pluginpremium.externalapiadapter.api.ecs.deploymentset


import org.zstack.pluginpremium.externalapiadapter.api.BaseAPI
import org.zstack.sdk.DeleteAffinityGroupAction

import static org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants.ECS_DEPLOYMENT_SET_ID
import static org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants.ZSTACK_UUID

/**
 * Created by lining on 2018/4/29.
 */
class DeleteDeploymentSet extends BaseAPI {
    @Override
    Class getZStackAction() {
        return DeleteAffinityGroupAction.class
    }

    @Override
    protected void configAPIConversionSpec() {
        spec = config {
            convertAPIParam {
                simpleConvert {
                    ecsParamName = ECS_DEPLOYMENT_SET_ID
                    zstackParamName = ZSTACK_UUID
                }
            }

            convertAPIResponse {

            }
        }
    }
}
