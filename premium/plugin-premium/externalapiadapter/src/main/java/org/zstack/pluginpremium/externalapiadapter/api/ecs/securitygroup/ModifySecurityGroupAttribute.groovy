package org.zstack.pluginpremium.externalapiadapter.api.ecs.securitygroup

import org.zstack.pluginpremium.externalapiadapter.api.BaseAPI
import org.zstack.sdk.UpdateSecurityGroupAction
import static org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants.*

/**
 * @Author: fubang
 * @Date: 2018/5/2
 */
class ModifySecurityGroupAttribute extends BaseAPI {
    @Override
    protected void configAPIConversionSpec() {
        spec = config {
            convertAPIParam {
                simpleConvert {
                    ecsParamName = ECS_SECURITY_GROUP_ID
                    zstackParamName = ZSTACK_UUID
                }
                simpleConvert {
                    ecsParamName = ECS_SECURITY_GROUP_NAME
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

    @Override
    Class getZStackAction() {
        return UpdateSecurityGroupAction.class
    }
}
