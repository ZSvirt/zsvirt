package org.zstack.pluginpremium.externalapiadapter.api.ecs.vpc

import org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterUtils
import org.zstack.pluginpremium.externalapiadapter.api.BaseQueryAPI
import org.zstack.sdk.QueryAliyunProxyVSwitchAction
import org.zstack.sdk.QueryAliyunProxyVpcAction
import org.zstack.sdk.QueryVRouterRouteTableAction
import org.zstack.sdk.VRouterRouteTableInventory

import java.util.stream.Collectors

import static org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants.*

/**
 * Created by Qi Le on 2019/11/8
 */
class DescribeRouteTableList extends BaseQueryAPI {
    @Override
    protected void configAPIConversionSpec() {
        spec = config {
            convertQueryAPIParam {
                beforeZstackAPIParam = { zstackParamMap ->
                    zstackParamMap.put(ZSTACK_QUERY_CONDITIONS_KEY, [])
                    zstackParamMap.put(ZSTACK_QUERY_REPLYWITHCOUNT_KEY, true)
                }

//                simpleConvert {
//                    ecsParamName = ECS_API_REGIONID_KEY
//                    zstackParamName = "attachedRouterRefs.virtualRouterVm.zone.uuid"
//                }

                querySimpleConvert {
                    ecsParamName = ECS_ROUTE_TABLE_ID
                    zstackParamName = ZSTACK_UUID
                }

                querySimpleConvert {
                    ecsParamName = "RouteTableName"
                    zstackParamName = ZSTACK_NAME
                }

//                simpleConvert {
//                    ecsParamName = "RouterId"
//                    zstackParamName = "attachedRouterRefs.virtualRouterVmUuid"
//                }

                querySimpleConvert {
                    ecsParamName = ECS_VPC_VPC_ID
                    zstackParamName = "attachedRouterRefs.virtualRouterVmUuid"
                    stillConvertParamWhenEcsParamValueIsNull = true
                    putZstackParamValue = { Map zstackParamMap, String ecsParamValue ->
                        List conditions = zstackParamMap.get(ZSTACK_QUERY_CONDITIONS_KEY)
                        String vpcId = ecsAPIParamMap.get(ecsParamName)
                        String routerId = ecsAPIParamMap.get("RouterId")
                        String vRouterUuid = null
                        if (vpcId != null) {
                            QueryAliyunProxyVpcAction vpcAction = new QueryAliyunProxyVpcAction(
                                    sessionId: sessionId,
                                    conditions: ["uuid=$ecsParamValue".toString()]
                            )
                            QueryAliyunProxyVpcAction.Result vpcResult = vpcAction.call()
                            vpcResult.throwExceptionIfError()
                            if (vpcResult.value.inventories.size() != 0) {
                                vRouterUuid = vpcResult.value.inventories.first().vRouterUuid
                            }
                        }
                        if (routerId != null) {
                            if (vRouterUuid != routerId) {
                                vRouterUuid = "000"
                            }
                        }
                        if (vRouterUuid != null) {
                            conditions.add("$zstackParamName=$vRouterUuid".toString())
                        }
                    }
                }
            }

            convertQueryAPIResponse {
                convertResponseAttribute {
                    ecsAttributeName = "RouterTableList"
                    ecsAttributeValue = new HashMap<>()
                    addEcsValueToEcsAPIRsp = { Map ecsAPIRsp ->
                        ecsAPIRsp.put(ecsAttributeName, ecsAttributeValue)
                    }

                    convertList {
                        ecsAttributeName = "RouterTableListType"
                        ecsAttributeValue = new ArrayList<>()

                        getZstackAttributeValue = {
                            return zstackAPIRsp.value.inventories
                        }

                        getElementZstackValues = {
                            return zstackAttributeValue
                        }

                        addEcsValueToFather = { Map parentMap ->
                            parentMap.put(ecsAttributeName, ecsAttributeValue)
                        }

                        addListElement = { VRouterRouteTableInventory routeTableInventory ->
                            addConvertResponseAttribute {
                                ecsAttributeValue = new HashMap<>()

                                addEcsValueToFather = { List parentList ->
                                    parentList.add(ecsAttributeValue)
                                }

                                zstackAttributeValue = routeTableInventory

                                convertResponseAttribute {
                                    ecsAttributeName = ECS_API_CREATION_TIME

                                    zstackAttributeValue = ExternalAPIAdapterUtils.formatIso8601Date(routeTableInventory.createDate)

                                    addEcsValueToFather = { Map parentMap ->
                                        parentMap.put(ecsAttributeName, zstackAttributeValue)
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = ECS_API_DESCRIPTION_KEY

                                    zstackAttributeValue = routeTableInventory.description

                                    addEcsValueToFather = { Map parentMap ->
                                        parentMap.put(ecsAttributeName, zstackAttributeValue)
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = "ResourceGroupId"

                                    addEcsValueToFather = { Map parentMap ->
                                        String resourceGroupId = ecsAPIParamMap.get(ecsAttributeName)
                                        if (resourceGroupId != null) {
                                            parentMap.put(ecsAttributeName, resourceGroupId)
                                        }
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = ECS_ROUTE_TABLE_ID

                                    zstackAttributeValue = routeTableInventory.uuid

                                    addEcsValueToFather = { Map parentMap ->
                                        parentMap.put(ecsAttributeName, zstackAttributeValue)
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = "RouteTableName"

                                    zstackAttributeValue = routeTableInventory.name

                                    addEcsValueToFather = { Map parentMap ->
                                        parentMap.put(ecsAttributeName, zstackAttributeValue)
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = "RouteTableType"

                                    zstackAttributeValue = "System"

                                    addEcsValueToFather = { Map parentMap ->
                                        parentMap.put(ecsAttributeName, zstackAttributeValue)
                                    }
                                }

                                convertResponseAttribute {
                                    //RouterId, VpcId and VSwitchIds
                                    ecsAttributeName = "RouterId"

                                    zstackAttributeValue = routeTableInventory.attachedRouterRefs

                                    addEcsValueToFather = { Map parentMap ->
                                        if (zstackAttributeValue.size() == 0) {
                                            return
                                        }
                                        zstackAttributeValue = zstackAttributeValue.first().virtualRouterVmUuid
                                        parentMap.put(ecsAttributeName, zstackAttributeValue)
                                        QueryAliyunProxyVpcAction vpcAction = new QueryAliyunProxyVpcAction(
                                                sessionId: sessionId,
                                                conditions: ["vRouterUuid=$zstackAttributeValue".toString()]
                                        )
                                        QueryAliyunProxyVpcAction.Result vpcResult = vpcAction.call()
                                        vpcResult.throwExceptionIfError()
                                        String vpcId = null
                                        if (vpcResult.value.inventories.size() != 0) {
                                            vpcId = vpcResult.value.inventories.first().uuid
                                            parentMap.put(ECS_VPC_VPC_ID, vpcId)
                                        }
                                        Map vswitchIds = new HashMap<>()
                                        parentMap.put("VSwithchIds", vswitchIds)
                                        List vswitchId
                                        if (vpcId == null) {
                                            vswitchId = new ArrayList<>()
                                        } else {
                                            QueryAliyunProxyVSwitchAction vSwitchAction = new QueryAliyunProxyVSwitchAction(
                                                    sessionId: sessionId,
                                                    conditions: ["aliyunProxyVpcUuid=$vpcId".toString()]
                                            )
                                            QueryAliyunProxyVSwitchAction.Result vSwitchResult = vSwitchAction.call()
                                            vSwitchResult.throwExceptionIfError()
                                            vswitchId = vSwitchResult.value.inventories.stream().map { inv -> inv.uuid }.collect(Collectors.toList())
                                        }
                                        vswitchIds.put(ECS_VPC_VSWITCH_ID, vswitchId)
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = "RouterType"

                                    zstackAttributeValue = "VRouter"

                                    addEcsValueToFather = { Map parentMap ->
                                        parentMap.put(ecsAttributeName, zstackAttributeValue)
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = ECS_API_STATUS_KEY

                                    zstackAttributeValue = "Available"

                                    addEcsValueToFather = { Map parentMap ->
                                        parentMap.put(ecsAttributeName, zstackAttributeValue)
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
        return QueryVRouterRouteTableAction.class
    }
}
