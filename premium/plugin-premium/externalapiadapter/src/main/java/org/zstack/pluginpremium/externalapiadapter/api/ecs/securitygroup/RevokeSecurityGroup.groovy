package org.zstack.pluginpremium.externalapiadapter.api.ecs.securitygroup

import org.zstack.pluginpremium.externalapiadapter.api.BaseAPI
import org.zstack.pluginpremium.externalapiadapter.exception.APIParamConvertException
import org.zstack.sdk.DeleteSecurityGroupRuleAction
import org.zstack.sdk.QuerySecurityGroupRuleAction

import static org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants.*

/**
 * @Author: fubang* @Date: 2018/5/2
 */
class RevokeSecurityGroup extends BaseAPI {
    @Override
    protected void configAPIConversionSpec() {
        spec = config {
            convertAPIParam {}
            convertAPIResponse {}
        }
    }

    @Override
    Object callZStackAction() {
        revokeRule(true)
        return null
    }

    protected void revokeRule(boolean isIngress) {
        String direction
        String cidrKey
        String securityGroupKey
        if (isIngress) {
            direction = ECS_SECURITY_GROUP_RULE_INGRESS
            cidrKey = ECS_SECURITY_GROUP_RULE_SOURCE_CIDR
            securityGroupKey = ECS_SECURITY_GROUP_SOURCE_GROUP_ID
        } else {
            direction = ECS_SECURITY_GROUP_RULE_EGRESS
            cidrKey = ECS_SECURITY_GROUP_RULE_DEST_CIDR
            securityGroupKey = ECS_SECURITY_GROUP_DEST_GROUP_ID
        }

        String securityGroupId = ecsAPIParamMap.get(ECS_SECURITY_GROUP_ID)
        String ipProtocol = ecsAPIParamMap.get(ECS_SECURITY_GROUP_RULE_PROTOCOL)
        String portRange = ecsAPIParamMap.get(ECS_SECURITY_GROUP_RULE_PORT_RANGE)
        String[] arr = portRange.split("/")

        if (arr.size() != 2) {
            throw new APIParamConvertException(ECS_SECURITY_GROUP_RULE_PORT_RANGE, "PortRange[value: ${portRange}] is not valid".toString())
        }

        QuerySecurityGroupRuleAction querySecurityGroupRuleAction = new QuerySecurityGroupRuleAction()
        querySecurityGroupRuleAction.sessionId = sessionId
        querySecurityGroupRuleAction.conditions = ["protocol=${ipProtocol.toUpperCase()}".toString(), "securityGroupUuid=$securityGroupId".toString(), "startPort=${arr[0]}".toString(), "endPort=${arr[1]}".toString(), "type=$direction".toString()]

        QuerySecurityGroupRuleAction.Result querySecurityGroupRuleActionResult = querySecurityGroupRuleAction.call()
        querySecurityGroupRuleActionResult.throwExceptionIfError()

        def inventories = querySecurityGroupRuleActionResult.value.inventories
        String cidrIp = ecsAPIParamMap.get(cidrKey)
        if (cidrIp != null) {
            inventories = inventories.grep { cidrIp == it.allowedCidr }
        }

        String otherGroupId = ecsAPIParamMap.get(securityGroupKey)
        if (otherGroupId != null) {
            inventories = inventories.grep { otherGroupId == it.remoteSecurityGroupUuid }
        }

        if (inventories.size() == 0) {
            logger.debug("[RequestId: ${requestId}] ${querySecurityGroupRuleActionResult.class.simpleName} result is empty".toString())
            return
        }

        DeleteSecurityGroupRuleAction deleteSecurityGroupRuleAction = new DeleteSecurityGroupRuleAction()
        deleteSecurityGroupRuleAction.sessionId = sessionId
        deleteSecurityGroupRuleAction.apiId = requestId
        deleteSecurityGroupRuleAction.ruleUuids = inventories.collect { it.uuid }

        DeleteSecurityGroupRuleAction.Result deleteSecurityGroupRuleActionResult = deleteSecurityGroupRuleAction.call()
        deleteSecurityGroupRuleActionResult.throwExceptionIfError()
    }

    @Override
    Class getZStackAction() {
        return null
    }
}
