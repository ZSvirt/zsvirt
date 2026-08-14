package org.zstack.pluginpremium.externalapiadapter.api.ecs.deploymentset

import org.zstack.pluginpremium.externalapiadapter.api.BaseAPI
import org.zstack.sdk.UpdateAffinityGroupAction

import static org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants.*

/**
 * Created by lining on 2018/4/29.
 */
class ModifyDeploymentSetAttribute extends BaseAPI {
    @Override
    Class getZStackAction() {
        return UpdateAffinityGroupAction.class
    }

    @Override
    protected void configAPIConversionSpec() {
        spec = config {
            convertAPIParam {
                simpleConvert {
                    ecsParamName = ECS_DEPLOYMENT_SET_ID
                    zstackParamName = ZSTACK_UUID
                }

                simpleConvert {
                    ecsParamName = ECS_DEPLOYMENT_SET_NAME
                    zstackParamName = ZSTACK_NAME
                }

                simpleConvert {
                    ecsParamName = ECS_API_DESCRIPTION_KEY
                    zstackParamName = ZSTACK_API_DESCRIPTION_KEY
                }

                simpleConvert {
                    ecsParamName = ECS_DEPLOYMENT_SET_DESCRIPTION
                    zstackParamName = ZSTACK_API_DESCRIPTION_KEY
                }
            }

            convertAPIResponse {}
        }
    }
}
