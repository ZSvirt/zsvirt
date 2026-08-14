package org.zstack.pluginpremium.externalapiadapter.api.ecs.instance.networkinterface

import org.apache.commons.lang.StringUtils
import org.zstack.pluginpremium.externalapiadapter.EcsSystemTags
import org.zstack.pluginpremium.externalapiadapter.api.BaseAPI
import org.zstack.pluginpremium.externalapiadapter.exception.APIParamConvertException
import org.zstack.sdk.CreateVmNicAction
import org.zstack.sdk.QueryAliyunProxyVSwitchAction
import org.zstack.sdk.QuerySecurityGroupAction
import org.zstack.sdk.SecurityGroupInventory

import static org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants.*

/**
 * Created by lining on 2018/5/20.
 */
class CreateNetworkInterface extends BaseAPI {
    @Override
    Class getZStackAction() {
        return CreateVmNicAction.class
    }

    @Override
    void configAPIConversionSpec() {
        spec = config {
            convertAPIParam {

                complexConvert {
                    ecsParamName = ECS_VPC_VSWITCH_ID
                    zstackParamName = "l3NetworkUuid"

                    getZstackValue = { Map ecsParamMap, String ecsParamValue ->
                        QueryAliyunProxyVSwitchAction query = new QueryAliyunProxyVSwitchAction(
                                sessionId: sessionId,
                                conditions: ["uuid=${ecsParamValue}".toString()]
                        )
                        QueryAliyunProxyVSwitchAction.Result result = query.call()
                        result.throwExceptionIfError()
                        if (result.value.inventories.size() == 0) {
                            throw new APIParamConvertException(ecsParamName, "VPC VSwitch[id:${ecsParamValue}] not found.".toString())
                        }
                        String vpcNetUuid = result.value.inventories.get(0).vpcL3NetworkUuid

                        String sgUuid = ecsParamMap.get(ECS_SECURITY_GROUP_ID)
                        QuerySecurityGroupAction querySG = new QuerySecurityGroupAction(
                                sessionId: sessionId,
                                conditions: ["uuid=${sgUuid}".toString()]
                        )
                        QuerySecurityGroupAction.Result sgResult = querySG.call()
                        sgResult.throwExceptionIfError()
                        if (sgResult.value.inventories.size() == 0) {
                            throw new APIParamConvertException(ECS_SECURITY_GROUP_ID, "SecurityGroup[id:${sgUuid}] not found".toString())
                        }

                        SecurityGroupInventory sgInv = sgResult.value.inventories.get(0)
                        if (!sgInv.attachedL3NetworkUuids.contains(vpcNetUuid)) {
                            throw new APIParamConvertException("VSwitchId/SecurityGroupId", "VSwitch[id:${ecsParamValue}] and SecurityGroup[id:${sgUuid}] are not under the same VPC")
                        }

                        return vpcNetUuid
                    }
                }

                simpleConvert {
                    ecsParamName = ECS_INSTANCE_PRIMARY_IP
                    zstackParamName = ZSTACK_NIC_IP

                    putZstackParamValue = { Map zstackParamMap, String ecsParamValue ->
                        if (StringUtils.isNotBlank(ecsParamValue)) {
                            zstackParamMap[zstackParamName] = ecsParamValue
                        }
                    }
                }

	            systemTagConvert {
                    ecsParamName = ECS_VPC_VSWITCH_ID

                    getTag = { ecsParamValue ->
                        return EcsSystemTags.VSWITCH_ID.instantiateTag([(EcsSystemTags.VSWITCH_ID_TOKEN): ecsParamValue])
                    }
                }

	            systemTagConvert {
		            ecsParamName = ECS_SECURITY_GROUP_ID

		            getTag = { ecsParamValue ->
			            return EcsSystemTags.SECURITYGROUP_ID.instantiateTag([(EcsSystemTags.SECURITYGROUP_ID_TOKEN): ecsParamValue])
		            }
	            }
            }

            convertAPIResponse {

                convertResponseAttribute {
                    ecsAttributeName = "NetworkInterfaceId"

                    getZstackAttributeValue = {
                        return zstackAPIRsp.value.inventory.uuid
                    }

                    addEcsValueToEcsAPIRsp = { ecsAPIRsp ->
                        ecsAPIRsp.put(ecsAttributeName, zstackAttributeValue)
                    }
                }
            }
        }
    }
}
