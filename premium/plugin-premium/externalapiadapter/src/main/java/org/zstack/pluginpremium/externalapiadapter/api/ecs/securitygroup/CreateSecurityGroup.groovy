package org.zstack.pluginpremium.externalapiadapter.api.ecs.securitygroup

import org.zstack.aliyunproxy.vpc.AliyunProxyVpcVO
import org.zstack.pluginpremium.externalapiadapter.EcsSystemTags
import org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterGlobalProperty
import org.zstack.pluginpremium.externalapiadapter.exception.APIParamConvertException
import org.zstack.sdk.AliyunProxyVSwitchInventory
import org.zstack.sdk.AliyunProxyVpcInventory
import org.zstack.sdk.AttachSecurityGroupToL3NetworkAction
import org.zstack.sdk.CreateSecurityGroupAction
import org.zstack.sdk.CreateSystemTagAction
import org.zstack.sdk.DeleteSecurityGroupAction
import org.zstack.sdk.QueryAliyunProxyVpcAction
import org.zstack.sdk.SecurityGroupInventory

import static org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants.*
import org.zstack.pluginpremium.externalapiadapter.api.BaseAPI

/**
 * Created by lining on 2018/4/30.
 */
class CreateSecurityGroup extends BaseAPI{
    @Override
    Class getZStackAction() {
        return CreateSecurityGroupAction.class
    }

    @Override
    protected void configAPIConversionSpec() {
        spec = config {
            convertAPIParam {
                simpleConvert {
                    ecsParamName = ECS_SECURITY_GROUP_NAME
                    zstackParamName = ZSTACK_NAME
                    stillConvertParamWhenEcsParamValueIsNull = true

                    putZstackParamValue = { zstackParamMap, zstackParamValue ->
                        zstackParamValue = zstackParamValue != null ? zstackParamValue : "UntitledSecurityGroup"
                        zstackParamMap.put(zstackParamName, zstackParamValue)
                    }
                }

                simpleConvert {
                    ecsParamName = ECS_API_DESCRIPTION_KEY
                    zstackParamName = ZSTACK_API_DESCRIPTION_KEY
                }
            }

            convertAPIResponse {
                convertResponseAttribute {
                    ecsAttributeName = ECS_SECURITY_GROUP_ID

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

    @Override
    void afterCallZStackAction(Object zstackActionResult) {
        super.afterCallZStackAction(zstackActionResult)

        CreateSecurityGroupAction.Result result = zstackActionResult
        SecurityGroupInventory sgInventory = result.value.inventory

        try {
            attachSecurityGroupToVpc(sgInventory.uuid)
        } catch (Exception e) {
            deleteSecurityGroup(sgInventory.uuid)
            throw e
        }
    }

    private void attachSecurityGroupToVpc(String sgUuid) {
        String paramName = ECS_VPC_VPC_ID
        String vpcUuid
        if (ecsAPIParamMap.containsKey(paramName)) {
            vpcUuid = ecsAPIParamMap.get(paramName)

            QueryAliyunProxyVpcAction query = new QueryAliyunProxyVpcAction(
                    sessionId: sessionId,
                    conditions: ["uuid=${vpcUuid}".toString()]
            )
            QueryAliyunProxyVpcAction.Result result = query.call()
            result.throwExceptionIfError()

            if (result.value.inventories.size() == 0) {
                throw new APIParamConvertException(paramName, "VPC[id:${vpcUuid}] not found")
            }
            AliyunProxyVpcInventory vpcInventory = result.value.inventories.get(0)

            doAttachSecurityGroupToVpc(sgUuid, vpcInventory)
        } else {
            attachSecurityGroupToL3Network(sgUuid, ExternalAPIAdapterGlobalProperty.PUBLICL3NETWORKUUID)
        }
    }

    private void doAttachSecurityGroupToVpc(String sgUuid, AliyunProxyVpcInventory vpcInventory) {
        CreateSystemTagAction action = new CreateSystemTagAction(
                sessionId: sessionId,
                resourceType: AliyunProxyVpcVO.class.getSimpleName(),
                resourceUuid: vpcInventory.uuid,
                tag: EcsSystemTags.VPC_SECURITYGROUP_ID.instantiateTag([(EcsSystemTags.SECURITYGROUP_ID_TOKEN): sgUuid])
        )
        CreateSystemTagAction.Result aResult = action.call()
        aResult.throwExceptionIfError()

        if (vpcInventory.aliyunProxyVSwitches.size() != 0) {
            for (AliyunProxyVSwitchInventory vSwitchInventory : vpcInventory.aliyunProxyVSwitches) {
                attachSecurityGroupToL3Network(sgUuid, vSwitchInventory.vpcL3NetworkUuid)
            }
        }
    }

    void attachSecurityGroupToL3Network(String sgUuid, String l3NetUuid) {
        AttachSecurityGroupToL3NetworkAction action = new AttachSecurityGroupToL3NetworkAction(
                sessionId: sessionId,
                securityGroupUuid: sgUuid,
                l3NetworkUuid: l3NetUuid
        )
        AttachSecurityGroupToL3NetworkAction.Result result = action.call()
        result.throwExceptionIfError()
    }

    private void deleteSecurityGroup(String sgUuid) {
        DeleteSecurityGroupAction delete = new DeleteSecurityGroupAction(
                sessionId: sessionId,
                uuid: sgUuid
        )
        delete.call()
    }
}
