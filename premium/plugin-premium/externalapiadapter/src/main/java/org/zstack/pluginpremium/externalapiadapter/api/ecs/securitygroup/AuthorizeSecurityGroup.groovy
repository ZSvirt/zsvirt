package org.zstack.pluginpremium.externalapiadapter.api.ecs.securitygroup

import org.zstack.network.securitygroup.APIAddSecurityGroupRuleMsg
import org.zstack.network.securitygroup.SecurityGroupRuleType
import org.zstack.pluginpremium.externalapiadapter.api.BaseAPI
import org.zstack.pluginpremium.externalapiadapter.typeconvertor.SecurityGroupProtocol
import org.zstack.sdk.AddSecurityGroupRuleAction

import static org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants.*

/**
 * Created by lining on 2018/4/30.
 */
class AuthorizeSecurityGroup extends BaseAPI{

    private static final boolean isIngress = true

    @Override
    Class getZStackAction() {
        return AddSecurityGroupRuleAction.class
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
                    ecsParamName = ECS_SECURITY_GROUP_RULE_PROTOCOL
                    ecsParamType = String.class
                    zstackParamName = ZSTACK_SECURITY_GROUP_RULES // SecurityGroupRuleAO.Protocol
                    zstackParamType = List.class

                    getZstackValue = { String ecsParamValue ->
                        return SecurityGroupProtocol.toZstack(ecsParamValue)
                    }

                    putZstackParamValue = { zstackParamMap, String zstackParamValue ->
                        APIAddSecurityGroupRuleMsg.SecurityGroupRuleAO rule = new APIAddSecurityGroupRuleMsg.SecurityGroupRuleAO()
                        rule.setProtocol(zstackParamValue)

                        zstackParamMap.put(zstackParamName, Arrays.asList(rule))
                    }
                }

                complexConvert {
                    ecsParamName = ECS_SECURITY_GROUP_RULE_PORT_RANGE
                    ecsParamType = String.class
                    zstackParamName = ZSTACK_SECURITY_GROUP_RULES // SecurityGroupRuleAO.startPort, SecurityGroupRuleAO.endPort
                    zstackParamType = List.class

                    getZstackValue = { String ecsParamValue ->
                        String[] range = ecsParamValue.split("/")
                        assert range.size() == 2

                        int start = Integer.parseInt(range[0])
                        int end = Integer.parseInt(range[1])

                        return [start, end]
                    }

                    putZstackParamValue = { Map zstackParamMap, List zstackParamValue ->
                        if ("all" == ecsAPIParamMap[ECS_SECURITY_GROUP_RULE_PROTOCOL]) {
                            return
                        }
                        APIAddSecurityGroupRuleMsg.SecurityGroupRuleAO rule = zstackParamMap.get(zstackParamName).get(0)
                        rule.setStartPort(zstackParamValue.get(0))
                        rule.setEndPort(zstackParamValue.get(1))
                    }
                }

                zstackNeedParam {
                    zstackParamName = ZSTACK_SECURITY_GROUP_RULES // SecurityGroupRuleAO.type

                    getZstackValue = { Map ecsParamMap, Map zstackParamMap ->
                        if (isIngress) {
                            return SecurityGroupRuleType.Ingress.toString()
                        } else {
                            return SecurityGroupRuleType.Egress.toString()
                        }
                    }

                    putZstackParamValue = { Map zstackParamMap, String zstackParamValue ->
                        APIAddSecurityGroupRuleMsg.SecurityGroupRuleAO rule = zstackParamMap.get(zstackParamName).get(0)
                        rule.setType(zstackParamValue)
                    }
                }

                complexConvert {
                    ecsParamName = isIngress ? ECS_SECURITY_GROUP_RULE_SOURCE_CIDR : ECS_SECURITY_GROUP_RULE_DEST_CIDR
                    ecsParamType = String.class
                    zstackParamName = ZSTACK_SECURITY_GROUP_RULES // SecurityGroupRuleAO.allowedCidr
                    zstackParamType = List.class

                    getZstackValue = { String ecsParamValue ->
                        return ecsParamValue
                    }

                    putZstackParamValue = { Map zstackParamMap, String zstackParamValue ->
                        APIAddSecurityGroupRuleMsg.SecurityGroupRuleAO rule = zstackParamMap.get(zstackParamName).get(0)
                        rule.setAllowedCidr(zstackParamValue)
                    }
                }

                complexConvert {
                    ecsParamName = isIngress ? ECS_SECURITY_GROUP_SOURCE_GROUP_ID : ECS_SECURITY_GROUP_DEST_GROUP_ID
                    ecsParamType = String.class
                    zstackParamName = ZSTACK_SECURITY_GROUP_SOURCE_GROUP_ID
                    zstackParamType = List.class

                    getZstackValue = { String ecsParamValue ->
                        return ecsParamValue
                    }

                    putZstackParamValue = { Map zstackParamMap, String zstackParamValue ->
                        zstackParamMap.put(zstackParamName, [zstackParamValue])
                    }
                }
            }

            convertAPIResponse {}
        }
    }
}
