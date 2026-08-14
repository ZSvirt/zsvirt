package org.zstack.pluginpremium.externalapiadapter.api.ecs.securitygroup

import org.apache.commons.lang.StringUtils
import org.zstack.aliyunproxy.vpc.AliyunProxyVpcVO
import org.zstack.pluginpremium.externalapiadapter.EcsSystemTags
import org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterUtils
import org.zstack.pluginpremium.externalapiadapter.api.BaseQueryAPI
import org.zstack.sdk.*

import java.util.stream.Collectors

import static org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants.*

/**
 * @Author: fubang* @Date: 2018/5/2
 */
class DescribeSecurityGroups extends BaseQueryAPI {
    @Override
    protected void configAPIConversionSpec() {
        spec = config {
            convertQueryAPIParam {
                beforeZstackAPIParam = { zstackParamMap ->
                    zstackParamMap.put(ZSTACK_QUERY_REPLYWITHCOUNT_KEY, true)
                    zstackParamMap.put(ZSTACK_QUERY_CONDITIONS_KEY, [])
                }

                querySimpleConvert {
                    ecsParamName = ECS_SECURITY_GROUP_NAME
                    zstackParamName = ZSTACK_NAME
                }

                querySimpleConvert {
                    ecsParamName = ECS_VPC_VPC_ID
                    zstackParamName = "attachedL3NetworkUuids"

                    putZstackParamValue = { Map zstackParamMap, String ecsParamValue ->
                        List conditions = zstackParamMap.get(ZSTACK_QUERY_CONDITIONS_KEY)
                        QueryAliyunProxyVSwitchAction query = new QueryAliyunProxyVSwitchAction(
                                sessionId: sessionId,
                                conditions: ["aliyunProxyVpcUuid=${ecsParamValue}".toString()]
                        )
                        QueryAliyunProxyVSwitchAction.Result result = query.call()
                        result.throwExceptionIfError()
                        if (result.value.inventories.size() == 0) {
                            //to get a empty result, same with ecs
                            conditions.add("${zstackParamName}=000".toString())
                            return
                        }
                        List l3Uuids = result.value.inventories.stream().map({inv -> inv.vpcL3NetworkUuid}).collect(Collectors.toList())
                        String uuids = StringUtils.join(l3Uuids, ",")
                        conditions.add("${zstackParamName}?=${uuids}".toString())
                    }
                }

                queryComplexConvert {
                    ecsParamName = "SecurityGroupIds"
                    ecsParamType = ArrayList.class
                    alterEcsParamName = ECS_SECURITY_GROUP_ID
                    alterEcsParamType = String.class
                    zstackParamName = ZSTACK_UUID
                    zstackParamType = String.class

                    getZstackValue = { Map ecsParamMap, ecsParamValue ->
                        String sgIds = ecsParamMap.get("SecurityGroupIds")
                        String sgId = ecsParamMap.get(ECS_SECURITY_GROUP_ID)
                        List res = []
                        if (sgIds != null) {
                            res = ExternalAPIAdapterUtils.changeValueType(sgIds, ecsParamType)
                        }
                        if (sgId != null) {
                            if (res.size() == 0) {
                                res.add(sgId)
                            } else {
                                //to get a empty result same with ecs
                                return ["000"]
                            }
                        }
                        return res
                    }

                    putZstackParamValue = { Map zstackParamMap, List zstackParamValue ->
                        String uuids
                        String op
                        if (zstackParamValue.size() == 0) {
                            return
                        }

                        if (zstackParamValue.size() == 1) {
                            op = "="
                            uuids = zstackParamValue.get(0)
                        } else {
                            op = "?="
                            uuids = StringUtils.join(zstackParamValue, ",")
                        }
                        List conditions = zstackParamMap.get(ZSTACK_QUERY_CONDITIONS_KEY)
                        conditions.add(zstackParamName + op + uuids)
                    }
                }
            }

            convertQueryAPIResponse {
                convertResponseAttribute {
                    ecsAttributeName = ECS_API_REGIONID_KEY
                    addEcsValueToEcsAPIRsp = { ecsAPIRsp ->
                        ecsAPIRsp.put(ecsAttributeName, ecsAPIParamMap.get(ecsAttributeName))
                    }
                }

                convertResponseAttribute {
                    ecsAttributeName = "SecurityGroups"
                    ecsAttributeValue = new HashMap<>()

                    addEcsValueToEcsAPIRsp = { ecsAPIRsp ->
                        ecsAPIRsp.put(ecsAttributeName, ecsAttributeValue)
                    }

                    convertList {
                        ecsAttributeName = "SecurityGroup"
                        ecsAttributeValue = new ArrayList<>()

                        getZstackAttributeValue = {
                            return zstackAPIRsp.value.inventories
                        }

                        getElementZstackValues = {
                            return zstackAttributeValue
                        }

                        addEcsValueToFather = { fatherValue ->
                            fatherValue.put(ecsAttributeName, ecsAttributeValue)
                        }

                        addListElement = { SecurityGroupInventory elementZstackValue ->
                            Map vpcSG = [:]
                            QuerySystemTagAction qSysTagAct = new QuerySystemTagAction(
                                    sessionId: sessionId,
                                    conditions: [
                                            "resourceType=${AliyunProxyVpcVO.class.getSimpleName()}".toString(),
                                            "tag~=${EcsSystemTags.SECURITYGROUP_ID_TOKEN}::%".toString()
                                    ]
                            )
                            QuerySystemTagAction.Result sysTagRes = qSysTagAct.call()
                            if (sysTagRes.error == null && !sysTagRes.value.inventories.isEmpty()) {
                                List tags = sysTagRes.value.inventories
                                tags.stream().forEach(
                                        { SystemTagInventory tag ->
                                            String sgId = EcsSystemTags.VPC_SECURITYGROUP_ID.getTokenByTag(tag.tag, EcsSystemTags.SECURITYGROUP_ID_TOKEN)
                                            if (sgId != null) {
                                                vpcSG[sgId] = tag.resourceUuid
                                            }
                                        }
                                )
                            }

                            addConvertResponseAttribute {
                                ecsAttributeValue = new HashMap<>()

                                addEcsValueToFather = { fatherValue ->
                                    fatherValue.add(ecsAttributeValue)
                                }

                                zstackAttributeValue = elementZstackValue

                                convertResponseAttribute {
                                    ecsAttributeName = ECS_SECURITY_GROUP_ID

                                    zstackAttributeValue = elementZstackValue.uuid

                                    addEcsValueToFather = { fatherValue ->
                                        fatherValue.put(ecsAttributeName, zstackAttributeValue)
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = ECS_SECURITY_GROUP_NAME

                                    zstackAttributeValue = elementZstackValue.name

                                    addEcsValueToFather = { fatherValue ->
                                        fatherValue.put(ecsAttributeName, zstackAttributeValue)
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = ECS_API_DESCRIPTION_KEY

                                    zstackAttributeValue = elementZstackValue.description

                                    addEcsValueToFather = { fatherValue ->
                                        fatherValue.put(ecsAttributeName, zstackAttributeValue)
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = ECS_API_CREATION_TIME

                                    zstackAttributeValue = elementZstackValue.createDate

                                    addEcsValueToFather = { fatherValue ->
                                        fatherValue.put(ecsAttributeName, ExternalAPIAdapterUtils.formatIso8601Date(zstackAttributeValue))
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = ECS_VPC_VPC_ID

                                    addEcsValueToFather = { Map parentMap ->
                                        String vpcId = vpcSG[elementZstackValue.uuid]
                                        parentMap[ecsAttributeName] = vpcId
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = "AvailableInstanceAmount"

                                    addEcsValueToFather = { fatherMap ->
                                        QueryVmNicInSecurityGroupAction query = new QueryVmNicInSecurityGroupAction(
                                                sessionId: sessionId,
                                                conditions: ["securityGroupUuid=${elementZstackValue.uuid}".toString()]
                                        )
                                        QueryVmNicInSecurityGroupAction.Result result = query.call()
                                        result.throwExceptionIfError()
                                        fatherMap.put(ecsAttributeName, 2000 - result.value.inventories.size())
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
}
