package org.zstack.pluginpremium.externalapiadapter.api.ecs.vpc

import org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterUtils
import org.zstack.pluginpremium.externalapiadapter.api.BaseQueryAPI
import org.zstack.sdk.*

import static org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants.*

/**
 * Created by lining on 2018/5/30.
 */
class DescribeVSwitches extends BaseQueryAPI {

    @Override
    Class getZStackAction() {
        return QueryAliyunProxyVSwitchAction.class
    }

    @Override
    void setEcsAPIParamDefaultValue(Map ecsAPIParamMap) {
        super.setEcsAPIParamDefaultValue(ecsAPIParamMap)

        if(!ecsAPIParamMap.containsKey("IsDefault")) {
            ecsAPIParamMap.put("IsDefault", "true")
        }
    }

    void configAPIConversionSpec() {
        spec = config {
            convertQueryAPIParam {
                beforeZstackAPIParam = { zstackParamMap ->
                    zstackParamMap.put(ZSTACK_QUERY_CONDITIONS_KEY, [])
                    zstackParamMap.put(ZSTACK_QUERY_REPLYWITHCOUNT_KEY, true)
                }

                querySimpleConvert {
                    ecsParamName = ECS_VPC_VPC_ID
                    zstackParamName = "aliyunProxyVpcUuid"
                }

                querySimpleConvert {
                    ecsParamName = ECS_VPC_VSWITCH_ID
                    zstackParamName = ZSTACK_UUID

                    putZstackParamValue = { zstackParamMap, zstackParamValue ->
                        List conditions = zstackParamMap.get(ZSTACK_QUERY_CONDITIONS_KEY)
                        conditions.add("$zstackParamName?=$zstackParamValue".toString())
                    }
                }

                querySimpleConvert {
                    ecsParamName = "IsDefault"
                    zstackParamName = "isDefault"

                    putZstackParamValue = { zstackParamMap, zstackParamValue ->
                        if (zstackParamValue == "false") {
                            List conditions = zstackParamMap.get(ZSTACK_QUERY_CONDITIONS_KEY)
                            conditions.add("$zstackParamName?=$zstackParamValue".toString())
                        }
                    }
                }
            }

            convertQueryAPIResponse {

                convertResponseAttribute {
                    ecsAttributeName = "VSwitches"
                    ecsAttributeValue = new HashMap<>()

                    addEcsValueToEcsAPIRsp = { ecsAPIRsp ->
                        ecsAPIRsp.put(ecsAttributeName, ecsAttributeValue)
                    }

                    convertList {
                        ecsAttributeName = "VSwitch"
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

                        addListElement = { AliyunProxyVSwitchInventory vSwitchInventory ->

                            QueryL3NetworkAction queryVpcL3Network = new QueryL3NetworkAction(
                                    sessionId: sessionId,
                                    conditions: ["uuid=${vSwitchInventory.vpcL3NetworkUuid}".toString()]
                            )
                            QueryL3NetworkAction.Result vpcL3NetworkResult = queryVpcL3Network.call()
                            vpcL3NetworkResult.throwExceptionIfError()
                            L3NetworkInventory vpcL3Network = vpcL3NetworkResult.value.inventories.get(0)

                            addConvertResponseAttribute {
                                ecsAttributeValue = new HashMap<>()

                                addEcsValueToFather = { fatherValue ->
                                    fatherValue.add(ecsAttributeValue)
                                }

                                zstackAttributeValue = vSwitchInventory

                                convertResponseAttribute {
                                    ecsAttributeName = ECS_VPC_VPC_ID

                                    zstackAttributeValue = vSwitchInventory.aliyunProxyVpcUuid

                                    addEcsValueToFather = { fatherValue ->
                                        fatherValue.put(ecsAttributeName, zstackAttributeValue)
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = ECS_VPC_VSWITCH_NAME

                                    zstackAttributeValue = vpcL3Network.name

                                    addEcsValueToFather = { fatherValue ->
                                        fatherValue.put(ecsAttributeName, zstackAttributeValue)
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = ECS_API_DESCRIPTION_KEY

                                    zstackAttributeValue = vpcL3Network.description

                                    addEcsValueToFather = { fatherValue ->
                                        fatherValue.put(ecsAttributeName, zstackAttributeValue)
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = ECS_API_CREATION_TIME

                                    zstackAttributeValue = ExternalAPIAdapterUtils.formatIso8601Date(vpcL3Network.createDate)

                                    addEcsValueToFather = { fatherValue ->
                                        fatherValue.put(ecsAttributeName, zstackAttributeValue)
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = "CidrBlock"

                                    zstackAttributeValue = vpcL3Network.ipRanges.get(0).networkCidr

                                    addEcsValueToFather = { fatherValue ->
                                        fatherValue.put(ecsAttributeName, zstackAttributeValue)
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = "AvailableIpAddressCount"

                                    addEcsValueToFather = { fatherValue ->

                                        GetIpAddressCapacityAction action = new GetIpAddressCapacityAction(
                                                sessionId: sessionId,
                                                l3NetworkUuids: [vpcL3Network.uuid]
                                        )
                                        GetIpAddressCapacityAction.Result ipAddressCapacityResult = action.call()
                                        ipAddressCapacityResult.throwExceptionIfError()

                                        fatherValue.put(ecsAttributeName, ipAddressCapacityResult.value.availableCapacity)
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = ECS_VPC_VSWITCH_ID

                                    zstackAttributeValue = vSwitchInventory.uuid

                                    addEcsValueToFather = { fatherValue ->
                                        fatherValue.put(ecsAttributeName, zstackAttributeValue)
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = ECS_API_STATUS_KEY

                                    zstackAttributeValue = vSwitchInventory.status

                                    addEcsValueToFather = { fatherValue ->
                                        fatherValue.put(ecsAttributeName, zstackAttributeValue)
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = ECS_API_ZONEID_KEY

                                    addEcsValueToFather = { Map parentMap ->
                                        QueryAliyunProxyVpcAction qVpcAct = new QueryAliyunProxyVpcAction(
                                                sessionId: sessionId,
                                                conditions: ["uuid=${vSwitchInventory.aliyunProxyVpcUuid}".toString()]
                                        )
                                        QueryAliyunProxyVpcAction.Result vpcRes = qVpcAct.call()
                                        if (vpcRes.error != null || vpcRes.value.inventories.isEmpty()) {
                                            parentMap[ecsAttributeName] = ""
                                            return
                                        }
                                        QueryVpcRouterAction qVrAct = new QueryVpcRouterAction(
                                                sessionId: sessionId,
                                                conditions: ["uuid=${vpcRes.value.inventories.first().vRouterUuid}".toString()]
                                        )
                                        QueryVpcRouterAction.Result vrRes = qVrAct.call()
                                        if (vrRes.error != null || vrRes.value.inventories.isEmpty()) {
                                            parentMap[ecsAttributeName] = ""
                                            return
                                        }
                                        VpcRouterVmInventory vr = vrRes.value.inventories.first()
                                        QueryClusterAction qZoneAct = new QueryClusterAction(
                                                sessionId: sessionId,
                                                conditions: ["uuid=${vr.clusterUuid}".toString()]
                                        )
                                        QueryClusterAction.Result zoneRes = qZoneAct.call()
                                        if (zoneRes.error != null || zoneRes.value.inventories.isEmpty()) {
                                            parentMap[ecsAttributeName] = ""
                                            return
                                        }
                                        ClusterInventory zone = zoneRes.value.inventories.first()
                                        parentMap[ecsAttributeName] = zone.name
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = "IsDefault"

                                    zstackAttributeValue = vSwitchInventory.isDefault

                                    addEcsValueToFather = { fatherValue ->
                                        fatherValue.put(ecsAttributeName, zstackAttributeValue)
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
