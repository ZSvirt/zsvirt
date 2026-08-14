package org.zstack.pluginpremium.externalapiadapter.api.ecs.securitygroup

import org.zstack.aliyunproxy.vpc.AliyunProxyVpcVO
import org.zstack.pluginpremium.externalapiadapter.EcsSystemTags
import org.zstack.pluginpremium.externalapiadapter.api.BaseAPI
import org.zstack.sdk.DeleteSecurityGroupAction
import org.zstack.sdk.DeleteTagAction
import org.zstack.sdk.QuerySystemTagAction
import org.zstack.sdk.SystemTagInventory

import static org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants.ECS_SECURITY_GROUP_ID
import static org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants.ZSTACK_UUID

/**
 * @Author: fubang* @Date: 2018/5/2
 */
class DeleteSecurityGroup extends BaseAPI {
    @Override
    protected void configAPIConversionSpec() {
        spec = config {
            convertAPIParam {
                simpleConvert {
                    ecsParamName = ECS_SECURITY_GROUP_ID
                    zstackParamName = ZSTACK_UUID
                }
            }
            convertAPIResponse {}
        }
    }

    @Override
    Class getZStackAction() {
        return DeleteSecurityGroupAction.class
    }

    @Override
    void afterCallZStackAction(Object zstackActionResult) {
        super.afterCallZStackAction(zstackActionResult)

        String paraValue = ecsAPIParamMap[ECS_SECURITY_GROUP_ID]

        QuerySystemTagAction query = new QuerySystemTagAction(
                sessionId: sessionId,
                conditions: ["resourceType=${AliyunProxyVpcVO.class.getSimpleName()}".toString()]
        )
        QuerySystemTagAction.Result result = query.call()
        result.throwExceptionIfError()
        if (result.value.inventories.size() == 0) {
            return
        }

        List systemTags = result.value.inventories
        String dTag = EcsSystemTags.VPC_SECURITYGROUP_ID.instantiateTag([(EcsSystemTags.SECURITYGROUP_ID_TOKEN): paraValue])
        systemTags = systemTags.stream().filter({ SystemTagInventory sTag -> sTag.tag == dTag }).collect { it }

        if (systemTags.size() == 0) {
            return
        }

        systemTags.forEach { SystemTagInventory tag ->
            DeleteTagAction delete = new DeleteTagAction(
                    sessionId: sessionId,
                    uuid: tag.uuid
            )
            delete.call()
        }
    }
}
