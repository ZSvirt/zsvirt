package org.zstack.pluginpremium.externalapiadapter.api.ecs.vpc

import org.zstack.pluginpremium.externalapiadapter.api.BaseAPI
import org.zstack.sdk.UpdateAliyunProxyVpcAction

import static org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants.*

/**
 * Created by Qi Le on 2019-05-29
 */
class ModifyVpcAttribute extends BaseAPI {
    @Override
    Class getZStackAction() {
        return UpdateAliyunProxyVpcAction.class
    }

    @Override
    protected void configAPIConversionSpec() {
        spec = config {
            convertAPIParam {
                simpleConvert {
                    ecsParamName = ECS_VPC_VPC_ID
                    zstackParamName = ZSTACK_UUID
                }

                simpleConvert {
                    ecsParamName = ECS_VPC_VPC_NAME
                    zstackParamName = ZSTACK_NAME
                }

                simpleConvert {
                    ecsParamName = ECS_API_DESCRIPTION_KEY
                    zstackParamName = ZSTACK_API_DESCRIPTION_KEY
                }

                simpleConvert {
                    ecsParamName = "IsDefault"
                    zstackParamName = "isDefault"
                }
            }

            convertAPIResponse {}
        }
    }

}
