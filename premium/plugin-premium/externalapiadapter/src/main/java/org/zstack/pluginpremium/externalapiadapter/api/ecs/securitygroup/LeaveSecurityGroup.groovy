package org.zstack.pluginpremium.externalapiadapter.api.ecs.securitygroup

import org.zstack.pluginpremium.externalapiadapter.api.BaseAPI
import org.zstack.sdk.DeleteVmNicFromSecurityGroupAction
import org.zstack.sdk.QueryVmNicAction

import static org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants.*

/**
 * @Author: fubang
 * @Date: 2018/5/2
 */
class LeaveSecurityGroup extends BaseAPI {
    @Override
    Class getZStackAction() {
        return DeleteVmNicFromSecurityGroupAction.class
    }

    @Override
    protected void configAPIConversionSpec() {
        spec = config {
            convertAPIParam {
                simpleConvert {
                    ecsParamName = ECS_SECURITY_GROUP_ID
                    zstackParamName = ZSTACK_SECURITY_GROUP_ID
                }

                complexConvert {
                    ecsParamName = ECS_INSTANCE_ID
                    zstackParamName = ZSTACK_VM_NIC_UUIDS
                    zstackParamType = List.class
                    getZstackValue = { String ecsParamValue ->
                        QueryVmNicAction action = new QueryVmNicAction(
                                sessionId: sessionId,
                                conditions: ["vmInstanceUuid=$ecsParamValue".toString()]
                        )

                        QueryVmNicAction.Result result = action.call()
                        result.throwExceptionIfError()

                        return result.value.inventories.collect { it.uuid }
                    }
                }
            }

            convertAPIResponse {}
        }
    }
}
