package org.zstack.pluginpremium.externalapiadapter.api.ecs.instance


import org.zstack.pluginpremium.externalapiadapter.api.BaseAPI
import org.zstack.sdk.SetVmConsolePasswordAction

import static org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants.ECS_INSTANCE_ID
import static org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants.ZSTACK_UUID

/**
 * @Author: fubang* @Date: 2018/4/30
 */
class ModifyInstanceVncPasswd extends BaseAPI {
    @Override
    protected void configAPIConversionSpec() {
        spec = config {
            convertAPIParam {
                simpleConvert {
                    ecsParamName = ECS_INSTANCE_ID
                    zstackParamName = ZSTACK_UUID
                }
                simpleConvert {
                    ecsParamName = "VncPassword"
                    zstackParamName = "consolePassword"
                }
            }

            convertAPIResponse {

            }
        }
    }

    @Override
    Class getZStackAction() {
        return SetVmConsolePasswordAction.class
    }
}
