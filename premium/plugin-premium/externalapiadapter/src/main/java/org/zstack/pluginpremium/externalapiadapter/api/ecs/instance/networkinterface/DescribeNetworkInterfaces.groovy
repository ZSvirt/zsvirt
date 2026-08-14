package org.zstack.pluginpremium.externalapiadapter.api.ecs.instance.networkinterface

import org.apache.commons.lang.StringUtils
import org.zstack.pluginpremium.externalapiadapter.EcsSystemTags
import org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterUtils
import org.zstack.pluginpremium.externalapiadapter.api.BaseQueryAPI
import org.zstack.pluginpremium.externalapiadapter.exception.APIParamConvertException
import org.zstack.sdk.*

import java.util.stream.Collectors

import static org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants.*

/**
 * Created by lining on 2018/5/20.
 */
class DescribeNetworkInterfaces extends BaseQueryAPI {
    @Override
    Class getZStackAction() {
        return QueryVmNicAction.class
    }

    @Override
    protected void configAPIConversionSpec() {
        spec = config {
            convertQueryAPIParam {
                beforeZstackAPIParam = { zstackParamMap ->
                    zstackParamMap.put(ZSTACK_QUERY_CONDITIONS_KEY, [])
                    zstackParamMap.put(ZSTACK_QUERY_REPLYWITHCOUNT_KEY, true)
                }

	            zstackNeedParam {
		            zstackParamName = "l3NetworkType"

		            getZstackValue = { Map ecsParamMap, Map zstackParamMap ->
			            return null
		            }

		            putZstackParamValue = { Map zstackParamMap, def zstackParamValue ->
			            List conditions = zstackParamMap.get(ZSTACK_QUERY_CONDITIONS_KEY) as List
			            conditions.add("l3Network.type=L3VpcNetwork")
		            }
	            }

                queryComplexConvert {
                    ecsParamName = ECS_VPC_VPC_ID
                    alterEcsParamName = ECS_VPC_VSWITCH_ID
                    zstackParamName = "l3NetworkUuid"

                    getZstackValue = { Map ecsParamMap, String ecsParamValue ->

                        String vSwitchId = ecsAPIParamMap.containsKey(ECS_VPC_VSWITCH_ID) ? ecsAPIParamMap.get(ECS_VPC_VSWITCH_ID) : null
                        String vpcId = ecsAPIParamMap.containsKey(ECS_VPC_VPC_ID) ? ecsAPIParamMap.get(ECS_VPC_VPC_ID) : null

                        def res = []
                        if (vpcId != null) {
                            QueryAliyunProxyVSwitchAction query = new QueryAliyunProxyVSwitchAction(
                                    sessionId: sessionId,
                                    conditions: ["aliyunProxyVpcUuid=${vpcId}".toString()]
                            )
                            QueryAliyunProxyVSwitchAction.Result result = query.call()
                            result.throwExceptionIfError()
                            if (result.value.inventories.size() == 0) {
                                throw new APIParamConvertException(ECS_VPC_VPC_ID, "VPC[id:${vpcId}] do not have any VSwitch attached to it.".toString())
                            }
                            result.value.inventories.stream().forEach { inv -> res.add(inv.vpcL3NetworkUuid) }
                        }

                        if (vSwitchId != null) {
                            QueryAliyunProxyVSwitchAction query = new QueryAliyunProxyVSwitchAction(
                                    sessionId: sessionId,
                                    conditions: ["uuid=${vSwitchId}".toString()]
                            )
                            QueryAliyunProxyVSwitchAction.Result result = query.call()
                            result.throwExceptionIfError()
                            if (result.value.inventories.size() == 0) {
                                throw new APIParamConvertException(ECS_VPC_VSWITCH_ID, "VSwitch[id:${vSwitchId}] not found".toString())
                            }
                            String l3Uuid = result.value.inventories.get(0).vpcL3NetworkUuid
                            if (res.size() == 0) {
                                res.add(l3Uuid)
                            } else if (!res.contains(l3Uuid)) {
                                throw new APIParamConvertException("VSwitchId/VpcId", "VSwitch[id:${vSwitchId}] not under VPC[id:${vpcId}]".toString())
                            }
                        }

                        return res
                    }

                    putZstackParamValue = { Map zstackParamMap, List zstackParamValue ->
                        String op
                        String l3Uuids
                        if (zstackParamValue.size() == 0) {
                            return
                        }

                        if (zstackParamValue.size() == 1) {
                            op = "="
                            l3Uuids = zstackParamValue.get(0)
                        } else {
                            op = "?="
                            l3Uuids = StringUtils.join(zstackParamValue, ",")
                        }

                        List conditions = zstackParamMap.get(ZSTACK_QUERY_CONDITIONS_KEY) as List
                        conditions.add(zstackParamName + op + l3Uuids)
                    }
                }

	            querySimpleConvert {
		            ecsParamName = ECS_INSTANCE_PRIMARY_IP
		            zstackParamName = ZSTACK_NIC_IP
	            }

                querySimpleConvert {
                    ecsParamName = ECS_SECURITY_GROUP_ID

	                putZstackParamValue = { Map zstackParamMap, String zstackParamValue ->
		                List conditions = zstackParamMap.get(ZSTACK_QUERY_CONDITIONS_KEY) as List

		                conditions.add("__systemTag__=${EcsSystemTags.SECURITYGROUP_ID_TOKEN}::${zstackParamValue}%".toString())
	                }
                }

                querySimpleConvert {
                    ecsParamName = ECS_INSTANCE_ID
                    zstackParamName = ZSTACK_VM_INSTANCE_UUID
                }

                querySimpleConvert {
                    ecsParamName = ECS_API_TYPE_KEY

                    putZstackParamValue = {Map zstackParamMap, String zstackParamValue ->
                        List conditions = zstackParamMap.get(ZSTACK_QUERY_CONDITIONS_KEY) as List

                        if (zstackParamValue == "Primary") {
                            conditions.add("deviceId=0")
                        } else {
                            conditions.add("deviceId!=0")
                        }
                    }
                }

	            querySimpleConvert {
                    ecsParamName = "NetworkInterfaceId.N"

                    zstackParamName = ZSTACK_UUID

                    stillConvertParamWhenEcsParamValueIsNull = true

                    putZstackParamValue = { Map zstackParamMap, String zstackParamValue ->
                        List conditions = zstackParamMap.get(ZSTACK_QUERY_CONDITIONS_KEY) as List
                        String paramname = "NetworkInterfaceId."
                        List nicIds = []

                        for (i in 1..100) {
                            String paramValue = ecsAPIParamMap.get("${paramname}${i}".toString())
				            if (paramValue == null) {
					            break
				            }
				            nicIds.add(paramValue)
			            }

			            if (nicIds.size() != 0) {
                            String nicIdConditions = StringUtils.join(nicIds, ",")
                            conditions.add("${zstackParamName}?=${nicIdConditions}".toString())
				            return
			            }

			            QueryApplianceVmAction queryVpcRouter = new QueryApplianceVmAction(
                                sessionId: sessionId,
                        )
                        QueryApplianceVmAction.Result vpcRouterResult = queryVpcRouter.call()
                        if (vpcRouterResult.error != null || vpcRouterResult.value.inventories.size() == 0) {
                            return
                        }
                        List<ApplianceVmInventory> vpcRouters = vpcRouterResult.value.inventories
                        List<String> vpcRouterNics = vpcRouters.stream().flatMap({ ApplianceVmInventory router ->
                            router.vmNics.stream()
                        }).map({ VmNicInventory nic ->
                            nic.uuid
                        }).collect(Collectors.toList())
                        String vpcRouterNicsStr = StringUtils.join(vpcRouterNics, ",")
                        conditions.add("$zstackParamName!?=$vpcRouterNicsStr".toString())
		            }
	            }

            }

            convertQueryAPIResponse {
                convertResponseAttribute {
                    ecsAttributeName = "NetworkInterfaceSets"
                    ecsAttributeValue = new HashMap<>()

                    addEcsValueToEcsAPIRsp = { ecsAPIRsp ->
                        ecsAPIRsp.put(ecsAttributeName, ecsAttributeValue)
                    }

                    convertList {
                        ecsAttributeName = "NetworkInterfaceSet"
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

                        addListElement = { VmNicInventory nicInventory ->

                            addConvertResponseAttribute {
                                ecsAttributeValue = new HashMap<>()

                                Map pubRspValue = new HashMap<>()

                                addEcsValueToFather = { fatherValue ->
                                    fatherValue.add(ecsAttributeValue)
                                }

                                zstackAttributeValue = nicInventory

                                convertResponseAttribute {
                                    ecsAttributeName = "NetworkInterfaceId"

                                    zstackAttributeValue = nicInventory.uuid

                                    addEcsValueToFather = { fatherValue ->
                                        fatherValue.put(ecsAttributeName, zstackAttributeValue)
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = ECS_API_STATUS_KEY

                                    addEcsValueToFather = { fatherValue ->
                                        String status = nicInventory.vmInstanceUuid == null ? "Available" : "InUse"
                                        fatherValue.put(ecsAttributeName, status)
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = ECS_API_TYPE_KEY

                                    def getValue = {
                                        if (ecsAPIParamMap.containsKey(ECS_API_TYPE_KEY)) {
                                            return ecsAPIParamMap.get(ECS_API_TYPE_KEY)
                                        }

                                        if (nicInventory.vmInstanceUuid == null) {
                                            return "Secondary"
                                        } else {
                                            return nicInventory.deviceId == 0 ? "Primary" : "Secondary"
                                        }
                                    }

                                    addEcsValueToFather = { fatherValue ->
                                        def ret = getValue()
                                        fatherValue.put(ecsAttributeName, ret)
                                        pubRspValue.put(ecsAttributeName, ret)
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = ECS_INSTANCE_PRIVATE_IP

                                    addEcsValueToFather = { fatherValue ->
                                        fatherValue.put(ecsAttributeName, nicInventory.ip)
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = "AssociatedPublicIp"
                                    ecsAttributeValue = new HashMap<>()

                                    addEcsValueToFather = { fatherMap ->
                                        fatherMap.put(ecsAttributeName, ecsAttributeValue)
                                        pubRspValue.put(ecsAttributeName, ecsAttributeValue)
                                        QueryEipAction queryEip = new QueryEipAction(
                                                sessionId: sessionId,
                                                conditions: ["vmNicUuid=${nicInventory.uuid}".toString(),
                                                             "state=Enable"]
                                        )
                                        QueryEipAction.Result EipRes = queryEip.call()
                                        EipRes.throwExceptionIfError()
                                        if (EipRes.value.inventories.size() == 0) {
                                            return
                                        }
                                        EipInventory eip = EipRes.value.inventories.first()
                                        ecsAttributeValue.put(ECS_EIP_ALLOCATION_ID, eip.uuid)
                                        ecsAttributeValue.put("PublicIpAddress", eip.vipIp)
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = "PrivateIpSets"
                                    ecsAttributeValue = new HashMap<>()

                                    addEcsValueToFather = { Map parentMap ->
                                        parentMap.put(ecsAttributeName, ecsAttributeValue)
                                        List privateIpSet = new ArrayList<>()
                                        ecsAttributeValue.put("PrivateIpSet", privateIpSet)
                                        Map pipElement = new HashMap<>()
                                        privateIpSet.add(pipElement)
                                        pipElement.put(ECS_INSTANCE_PRIVATE_IP, nicInventory.ip)
                                        pipElement.put("Primary", pubRspValue.get(ECS_API_TYPE_KEY) == "Primary")
                                        pipElement.put("AssociatedPublicIp", pipElement.getOrDefault("AssociatedPublicIp", new HashMap<>()))
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = "MacAddress"

                                    addEcsValueToFather = { fatherValue ->
                                        fatherValue.put(ecsAttributeName, nicInventory.mac)
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = ECS_INSTANCE_ID

                                    addEcsValueToFather = { fatherValue ->
                                        fatherValue.put(ecsAttributeName, nicInventory.vmInstanceUuid)
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = ECS_API_CREATION_TIME

                                    addEcsValueToFather = { fatherValue ->
                                        fatherValue.put(ecsAttributeName, ExternalAPIAdapterUtils.formatIso8601Date(nicInventory.createDate))
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = ECS_VPC_VSWITCH_ID

                                    addEcsValueToFather = { fatherValue ->
                                        QueryAliyunProxyVSwitchAction query = new QueryAliyunProxyVSwitchAction(
                                                sessionId: sessionId,
                                                conditions: ["vpcL3NetworkUuid=${nicInventory.l3NetworkUuid}".toString()]
                                        )
                                        QueryAliyunProxyVSwitchAction.Result result = query.call()
                                        result.throwExceptionIfError()
                                        if (result.value.inventories.size() == 0) {
                                            throw new APIParamConvertException(ecsAttributeName, "VSwitch with l3Network[id:${nicInventory.l3NetworkUuid}] not found".toString())
                                        }
                                        AliyunProxyVSwitchInventory vSwitchInv = result.value.inventories.get(0)
                                        fatherValue.put(ecsAttributeName, vSwitchInv.uuid)
                                        if (vSwitchInv.aliyunProxyVpcUuid != null) {
                                            fatherValue.put(ECS_VPC_VPC_ID, vSwitchInv.aliyunProxyVpcUuid)
                                        }
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = ECS_API_REGIONID_KEY

                                    addEcsValueToFather = { fatherValue ->
                                        fatherValue.put(ecsAttributeName, ecsAPIParamMap.get(ecsAttributeName))
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = "SecurityGroupIds"

                                    addEcsValueToFather = { fatherValue ->
                                        QuerySystemTagAction action = new QuerySystemTagAction(
                                                sessionId: sessionId,
                                                conditions: [
                                                        "resourceType=VmNicVO",
                                                        "resourceUuid=${nicInventory.uuid}".toString()
                                                ]
                                        )
                                        QuerySystemTagAction.Result result = action.call()
                                        result.throwExceptionIfError()

                                        List securityGroupIds = result.value.inventories.stream().filter { SystemTagInventory tag ->
                                            EcsSystemTags.SECURITYGROUP_ID.isMatch(tag.getTag())
                                        }.map { SystemTagInventory tag ->
                                            EcsSystemTags.SECURITYGROUP_ID.getTokenByTag(tag.getTag(), EcsSystemTags.SECURITYGROUP_ID_TOKEN)
                                        }.collect(Collectors.toList())
                                        def value = [(ECS_SECURITY_GROUP_ID): securityGroupIds]
                                        fatherValue.put(ecsAttributeName, value)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
