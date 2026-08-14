package org.zstack.pluginpremium.externalapiadapter.api.ecs.instance


import org.zstack.pluginpremium.externalapiadapter.api.BaseAPI
import org.zstack.sdk.ConsoleInventory
import org.zstack.sdk.RequestConsoleAccessAction

import static org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants.ECS_INSTANCE_ID
import static org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants.ZSTACK_VM_INSTANCE_UUID

/**
 * @Author: fubang* @Date: 2018/4/30
 */
class DescribeInstanceVncUrl extends BaseAPI {
    @Override
    protected void configAPIConversionSpec() {
        spec = config {
            convertAPIParam {
                simpleConvert {
                    ecsParamName = ECS_INSTANCE_ID
                    zstackParamName = ZSTACK_VM_INSTANCE_UUID
                }
            }
            convertAPIResponse {
                convertResponseAttribute {
                    ecsAttributeName = "VncUrl"
                    addEcsValueToEcsAPIRsp = { rsp ->
                        def inventory = zstackAPIRsp.value.inventory as ConsoleInventory
                        String value = "host=${inventory.hostname}&port=${inventory.port}&token=${inventory.token}".toString()
                        rsp.put(ecsAttributeName, value)
                    }
                }
            }
        }
    }

    @Override
    Class getZStackAction() {
        return RequestConsoleAccessAction.class
    }
}
