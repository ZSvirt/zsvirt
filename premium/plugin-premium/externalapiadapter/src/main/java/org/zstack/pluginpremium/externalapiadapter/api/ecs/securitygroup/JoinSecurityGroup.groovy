package org.zstack.pluginpremium.externalapiadapter.api.ecs.securitygroup

import org.zstack.pluginpremium.externalapiadapter.api.BaseAPI
import org.zstack.pluginpremium.externalapiadapter.exception.APIAdapterSpecifiedErrorException
import org.zstack.pluginpremium.externalapiadapter.exception.APIParamConvertException
import org.zstack.sdk.*

import java.util.stream.Collectors

import static org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants.*

/**
 * Created by lining on 2018/4/30.
 */
class JoinSecurityGroup extends BaseAPI{
    static final String SECURITY_GROUP_NOT_FOUND_CODE = "InvalidSecurityGroupId.NotFound"
    static final String SECURITY_GROUP_NOT_FOUND_MESSAGE = "The specified SecurityGroupId does not exist."
    static final String INSTANCE_NOT_FOUND_CODE = "InvalidInstanceId.NotFound"
    static final String INSTANCE_NOT_FOUND_MESSAGE = "The specified InstanceId does not exist."

    @Override
    Class getZStackAction() {
        return AddVmNicToSecurityGroupAction.class
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
                    ecsParamType = String.class
                    zstackParamName = ZSTACK_VM_NIC_UUIDS
                    zstackParamType = List.class

                    getZstackValue = { String ecsParamValue ->
                        //vSwitch should already bound to sg at this time, so we can safely use sg to do the check
                        QueryVmInstanceAction queryVmInstanceAction = new QueryVmInstanceAction(
                                sessionId: sessionId,
                                conditions: ["uuid=$ecsParamValue".toString()]
                        )
                        QueryVmInstanceAction.Result result = queryVmInstanceAction.call()
                        if (result.error != null || result.value.inventories.size() == 0) {
                            throw new APIAdapterSpecifiedErrorException(
                                    INSTANCE_NOT_FOUND_CODE,
                                    INSTANCE_NOT_FOUND_MESSAGE
                            )
                        }

                        String sgUuid = ecsAPIParamMap[ECS_SECURITY_GROUP_ID]
                        QuerySecurityGroupAction querySecurityGroupAction = new QuerySecurityGroupAction(
                                sessionId: sessionId,
                                conditions: ["uuid=$sgUuid".toString()]
                        )
                        QuerySecurityGroupAction.Result securityGroupResult = querySecurityGroupAction.call()
                        if (securityGroupResult.error != null || securityGroupResult.value.inventories.size() == 0) {
                            throw new APIAdapterSpecifiedErrorException(
                                    SECURITY_GROUP_NOT_FOUND_CODE,
                                    SECURITY_GROUP_NOT_FOUND_MESSAGE
                            )
                        }

                        SecurityGroupInventory sg = securityGroupResult.value.inventories[0] as SecurityGroupInventory
                        VmInstanceInventory vm = result.value.inventories[0] as VmInstanceInventory

                        List vmNicUuids = vm.getVmNics().stream().filter{VmNicInventory nic ->
                            sg.attachedL3NetworkUuids.contains(nic.l3NetworkUuid)
                        }.map{VmNicInventory nic -> nic.uuid}.collect(Collectors.toList())
                        if (vmNicUuids.size() == 0) {
                            throw new APIParamConvertException(ecsParamName,
                                    "No nic attached to this instance found are under the same vpc network with this security group")
                        }
                        return vmNicUuids
                    }
                }
            }

            convertAPIResponse {
                convertResponseAttribute {}
            }
        }
    }
}
