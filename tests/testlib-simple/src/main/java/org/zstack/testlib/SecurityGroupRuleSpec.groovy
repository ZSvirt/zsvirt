package org.zstack.testlib
/**
 * Created by xing5 on 2017/2/20.
 */
class SecurityGroupRuleSpec extends Spec {
    @SpecParam(required = true)
    String type
    @SpecParam(required = true)
    String protocol
    @SpecParam(required = true)
    Integer startPort
    @SpecParam(required = true)
    Integer endPort
    @SpecParam
    String allowedCidr

    SecurityGroupRuleSpec(EnvSpec envSpec) {
        super(envSpec)
    }

    SpecID create(String uuid, String sessionId) {
        addSecurityGroupRule {
            delegate.sessionId = sessionId
            delegate.rules = [[
                    "type": type,
                    "protocol": protocol,
                    "startPort": startPort,
                    "endPort": endPort,
                    "allowedCidr": allowedCidr
            ]]
            delegate.securityGroupUuid = (parent as SecurityGroupSpec).inventory.uuid
        }

        return null
    }

    @Override
    void delete(String sessionId) {
        // do nothing
    }
}
