package org.zstack.pluginpremium.externalapiadapter.api.ecs.securitygroup

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.zstack.aliyunproxy.vpc.AliyunProxyVpcVO
import org.zstack.pluginpremium.externalapiadapter.EcsSystemTags
import org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterGlobalProperty
import org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterUtils
import org.zstack.pluginpremium.externalapiadapter.api.BaseAPI
import org.zstack.pluginpremium.externalapiadapter.typeconvertor.SecurityGroupDirection
import org.zstack.pluginpremium.externalapiadapter.typeconvertor.SecurityGroupProtocol
import org.zstack.sdk.QuerySecurityGroupAction
import org.zstack.sdk.QuerySystemTagAction
import org.zstack.sdk.SecurityGroupRuleInventory
import org.zstack.sdk.SystemTagInventory
import org.zstack.utils.gson.JSONObjectUtil

import java.util.stream.Collectors

import static org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants.*

/**
 * @Author: fubang
 * @Date: 2018/5/1
 */
class DescribeSecurityGroupAttribute extends BaseAPI {
    @Override
    protected void configAPIConversionSpec() {
        spec = config {
            convertAPIParam {
                beforeZstackAPIParam = { zstackParamMap ->
                    zstackParamMap.put(ZSTACK_QUERY_CONDITIONS_KEY, [])
                    zstackParamMap.put(ZSTACK_QUERY_REPLYWITHCOUNT_KEY, true)
                }

                querySimpleConvert {
                    ecsParamName = ECS_SECURITY_GROUP_ID
                    zstackParamName = ZSTACK_UUID
                }
            }

            convertAPIResponse {
                Map pubResValue = new HashMap<>()

                convertResponseAttribute {
                    ecsAttributeName = ECS_SECURITY_GROUP_ID

                    addEcsValueToEcsAPIRsp = { ecsAPIRsp ->
                        ecsAPIRsp.put(ecsAttributeName, zstackAPIRsp?.uuid)
                    }
                }

                convertResponseAttribute {
                    ecsAttributeName = ECS_SECURITY_GROUP_NAME

                    addEcsValueToEcsAPIRsp = { ecsAPIRsp ->
                        ecsAPIRsp.put(ecsAttributeName, zstackAPIRsp?.name)
                    }
                }

                convertResponseAttribute {
                    ecsAttributeName = ECS_VPC_VPC_ID

                    addEcsValueToEcsAPIRsp = { Map ecsAPIRsp ->
                        String sgId = zstackAPIRsp?.uuid
                        if (sgId == null) {
                            ecsAPIRsp[ecsAttributeName] = ""
                        }
                        String tagStr = EcsSystemTags.VPC_SECURITYGROUP_ID.instantiateTag([(EcsSystemTags.SECURITYGROUP_ID_TOKEN): sgId])
                        QuerySystemTagAction qSysTagAct = new QuerySystemTagAction(
                                sessionId: sessionId,
                                conditions: [
                                        "resourceType=${AliyunProxyVpcVO.class.getSimpleName()}".toString(),
                                        "tag=${tagStr}".toString()
                                ]
                        )
                        QuerySystemTagAction.Result sysTagRes = qSysTagAct.call()
                        if (sysTagRes.error != null || sysTagRes.value.inventories.isEmpty()) {
                            ecsAPIRsp[ecsAttributeName] = ""
                        }
                        SystemTagInventory tag = sysTagRes.value.inventories.first()
                        ecsAPIRsp[ecsAttributeName] = tag.resourceUuid
                    }
                }

                convertResponseAttribute {
                    ecsAttributeName = ECS_API_REGIONID_KEY
                    addEcsValueToEcsAPIRsp = { ecsAPIRsp ->
                        ecsAPIRsp.put(ecsAttributeName, ecsAPIParamMap.get(ecsAttributeName))
                    }
                }

                convertResponseAttribute {
                    ecsAttributeName = ECS_API_DESCRIPTION_KEY
                    addEcsValueToEcsAPIRsp = { ecsAPIRsp ->
                        String description = zstackAPIRsp?.description
                        pubResValue.put(ecsAttributeName, description)
                        ecsAPIRsp.put(ecsAttributeName, description)
                    }
                }

                convertResponseAttribute {
                    ecsAttributeName = "InnerAccessPolicy"
                    addEcsValueToEcsAPIRsp = { ecsAPIRsp ->
                        ecsAPIRsp.put(ecsAttributeName, "Accept")
                    }
                }

                convertResponseAttribute {
                    ecsAttributeName = "Permissions"
                    ecsAttributeValue = new HashMap<>()
                    addEcsValueToEcsAPIRsp = { ecsAPIRsp ->
                        ecsAPIRsp.put(ecsAttributeName, ecsAttributeValue)
                    }

                    convertList {
                        ecsAttributeName = "Permission"
                        ecsAttributeValue = new ArrayList<>()

                        getZstackAttributeValue = {
                            pubResValue[ECS_SECURITY_GROUP_NIC_TYPE] = ECS_ADDRESS_TYPE_INTRANET
                            Set<String> l3Networks = zstackAPIRsp.attachedL3NetworkUuids
                            for (String l3Network : l3Networks) {
                                if (ExternalAPIAdapterGlobalProperty.PUBLICL3NETWORKUUID == l3Network) {
                                    pubResValue[ECS_SECURITY_GROUP_NIC_TYPE] = ECS_ADDRESS_TYPE_INTERNET
                                }
                            }

                            List elements = zstackAPIRsp == null ? [] : zstackAPIRsp.rules
                            boolean bidirectional = !ecsAPIParamMap.containsKey("Direction")
                            String direction
                            if (!bidirectional) {
                                direction = SecurityGroupDirection.getSecurityGroupDirectionFromEcs(ecsAPIParamMap.get("Direction") as String).zstackValue
                            }
                            List list = elements.stream().filter { rule ->
                                rule.remoteSecurityGroupUuid == null
                            }.filter { rule ->
                                (bidirectional ?: rule.type == direction)
                            }.collect(Collectors.toList())
                            return list
                        }

                        getElementZstackValues = {
                            return zstackAttributeValue
                        }

                        addEcsValueToFather = { fatherValue ->
                            fatherValue.put(ecsAttributeName, ecsAttributeValue)
                        }

                        addListElement = { SecurityGroupRuleInventory ruleInventory ->
                            addConvertResponseAttribute {
                                ecsAttributeValue = new HashMap<>()

                                addEcsValueToFather = { fatherValue ->
                                    fatherValue.add(ecsAttributeValue)
                                }

                                zstackAttributeValue = ruleInventory

                                convertResponseAttribute {
                                    ecsAttributeName = ECS_SECURITY_GROUP_RULE_PROTOCOL

                                    zstackAttributeValue = ruleInventory.protocol

                                    addEcsValueToFather = { fatherValue ->
                                        fatherValue.put(ecsAttributeName, SecurityGroupProtocol.toEcs(zstackAttributeValue as String))
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = ECS_SECURITY_GROUP_RULE_PORT_RANGE

                                    addEcsValueToFather = { fatherValue ->
                                        int startport = ruleInventory.startPort
                                        int endport = ruleInventory.endPort
                                        if (startport == 0 && endport == 0) {
                                            startport = ECS_SECURITY_GROUP_PORT_RANGE_OMIT_FLAG
                                            endport = ECS_SECURITY_GROUP_PORT_RANGE_OMIT_FLAG
                                        }
                                        fatherValue.put(ecsAttributeName, "$startport/$endport".toString())
                                    }
                                }

                                convertResponseAttribute {
                                    addEcsValueToFather = { fatherValue ->
                                        if (ruleInventory.type == ECS_SECURITY_GROUP_RULE_INGRESS) {
                                            fatherValue.put(ECS_SECURITY_GROUP_RULE_SOURCE_CIDR, ruleInventory.allowedCidr)
                                            fatherValue.put(ECS_SECURITY_GROUP_SOURCE_GROUP_ID, ruleInventory.remoteSecurityGroupUuid)
                                        } else if (ruleInventory.type == ECS_SECURITY_GROUP_RULE_EGRESS) {
                                            fatherValue.put(ECS_SECURITY_GROUP_RULE_DEST_CIDR, ruleInventory.allowedCidr)
                                            fatherValue.put(ECS_SECURITY_GROUP_DEST_GROUP_ID, ruleInventory.remoteSecurityGroupUuid)
                                        }
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = "Direction"

                                    zstackAttributeValue = ruleInventory.type

                                    addEcsValueToFather = { fatherValue ->
                                        def value = SecurityGroupDirection.getSecurityGroupDirectionFromZstack(zstackAttributeValue as String).ecsValue
                                        fatherValue.put(ecsAttributeName, value)
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = "CreateTime"

                                    zstackAttributeValue = ruleInventory.createDate

                                    addEcsValueToFather = { fatherValue ->
                                        fatherValue.put(ecsAttributeName, ExternalAPIAdapterUtils.formatIso8601Date(zstackAttributeValue))
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = ECS_API_DESCRIPTION_KEY

                                    addEcsValueToFather = { Map parentMap ->
                                        String description = pubResValue.get(ecsAttributeName)
                                        parentMap.put(ecsAttributeName, description? description : "")
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = ECS_SECURITY_GROUP_NIC_TYPE

                                    zstackAttributeValue = pubResValue[ecsAttributeName]

                                    addEcsValueToFather = { Map parentMap ->
                                        (parentMap[ecsAttributeName] = zstackAttributeValue)
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = "Policy"

                                    zstackAttributeValue = "Accept"

                                    addEcsValueToFather = { Map parentMap ->
                                        parentMap.put(ecsAttributeName, zstackAttributeValue)
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = ECS_API_PRIORITY

                                    addEcsValueToFather = { Map parentMap ->
                                        parentMap[ecsAttributeName] = "1"
                                    }
                                }
                            }
                        }
                    }

                }
            }
        }
    }

    @Override
    Class getZStackAction() {
        return QuerySecurityGroupAction.class
    }

    @Override
    Object callZStackAction() {
        Gson gson = new GsonBuilder().create()
        QuerySecurityGroupAction action = gson.fromJson(JSONObjectUtil.toJsonString(zstackAPIParamMap), QuerySecurityGroupAction.class)
        def result = action.call()
        result.throwExceptionIfError()

        this.afterCallZStackAction(result)

        if (result.value.inventories.size() == 0) {
            return null
        }

        return result.value.inventories.first()
    }
}

