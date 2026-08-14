package org.zstack.pluginpremium.externalapiadapter.api.ecs.securitygroup
/**
 * @Author: fubang* @Date: 2018/5/2
 * Rewrite by Qi Le on 2019/12/18
 */
class RevokeSecurityGroupEgress extends RevokeSecurityGroup {
    @Override
    Object callZStackAction() {
        revokeRule(false)
        return null
    }
}
