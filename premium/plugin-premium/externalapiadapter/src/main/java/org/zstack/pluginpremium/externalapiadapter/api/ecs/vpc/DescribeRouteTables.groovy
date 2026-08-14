package org.zstack.pluginpremium.externalapiadapter.api.ecs.vpc

import org.apache.commons.lang.StringUtils
import org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterUtils
import org.zstack.pluginpremium.externalapiadapter.api.BaseQueryAPI
import org.zstack.sdk.*

import java.util.stream.Collectors

import static org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants.*

/**
 * Created by Qi Le on 2019/11/8
 */
class DescribeRouteTables extends BaseQueryAPI {
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

                querySimpleConvert {
                    ecsParamName = "RouterId"
                    zstackParamName = "attachedRouterRefs.virtualRouterVmUuid"
                }

                querySimpleConvert {
                    ecsParamName = ECS_VPC_VROUTER_ID
                    zstackParamName = "attachedRouterRefs.virtualRouterVmUuid"
                    putZstackParamValue = { Map zstackParamMap, String ecsParamValue ->
                        if (ecsAPIParamMap.containsKey("RouterId")) {
                            return
                        }
                        List conditions = zstackParamMap.get(ZSTACK_QUERY_CONDITIONS_KEY)
                        conditions.add("$zstackParamName=$ecsParamValue".toString())
                    }
                }
            }

            convertQueryAPIResponse {
                convertResponseAttribute {
                    ecsAttributeName = "RouteTables"
                    ecsAttributeValue = new HashMap<>()
                    addEcsValueToEcsAPIRsp = { Map ecsAPIRsp ->
                        ecsAPIRsp.put(ecsAttributeName, ecsAttributeValue)
                    }

                    convertList {
                        ecsAttributeName = "RouteTable"
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
                                    ecsAttributeName = ECS_ROUTE_TABLE_ID

                                    zstackAttributeValue = routeTableInventory.uuid

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
                                    ecsAttributeName = "RouteTableType"

                                    zstackAttributeValue = "System"

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

                                convertResponseAttribute {
                                    //VRouterId and VSwitchIds
                                    ecsAttributeName = ECS_VPC_VROUTER_ID

                                    zstackAttributeValue = routeTableInventory.attachedRouterRefs

                                    addEcsValueToFather = { Map parentMap ->
                                        if (zstackAttributeValue.size() == 0) {
                                            return
                                        }
                                        String vRouterUuid = zstackAttributeValue.first().virtualRouterVmUuid
                                        parentMap.put(ecsAttributeName, vRouterUuid)
                                        QueryAliyunProxyVpcAction vpcAction = new QueryAliyunProxyVpcAction(
                                                sessionId: sessionId,
                                                conditions: ["vRouterUuid=$vRouterUuid".toString()]
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
                                    ecsAttributeName = "RouteEntrys"
                                    ecsAttributeValue = new HashMap<>()
                                    addEcsValueToFather = { Map parentMap ->
                                        parentMap.put(ecsAttributeName, ecsAttributeValue)
                                        List routeEntries = new ArrayList<>()
                                        ecsAttributeValue.put("RouteEntry", routeEntries)
                                        for (VRouterRouteEntryInventory routeEntry : routeTableInventory.routeEntries) {
                                            Map entry = new HashMap<>()
                                            routeEntries.add(entry)
                                            entry[ECS_ROUTE_TABLE_ID] = routeEntry.routeTableUuid
                                            entry["DestinationCidrBlock"] = routeEntry.destination
                                            entry[ECS_API_TYPE_KEY] = "Custom"
                                            entry["RouteEntryId"] = routeEntry.uuid
                                            entry[ECS_API_STATUS_KEY] = "Available"

                                            //InstanceId and nextHopType
                                            String vRouterId = null
                                            if (routeTableInventory.attachedRouterRefs.size() != 0) {
                                                vRouterId = routeTableInventory.attachedRouterRefs.first().virtualRouterVmUuid
                                            }
                                            if (vRouterId != null) {
                                                QueryAliyunProxyVpcAction vpcAction = new QueryAliyunProxyVpcAction(
                                                        sessionId: sessionId,
                                                        conditions: ["vRouterUuid=$vRouterId".toString()]
                                                )
                                                QueryAliyunProxyVpcAction.Result vpcResult = vpcAction.call()
                                                vpcResult.throwExceptionIfError()
                                                String vpcId = null
                                                if (vpcResult.value.inventories.size() != 0) {
                                                    vpcId = vpcResult.value.inventories.first().uuid
                                                }
                                                if (vpcId != null) {
                                                    QueryAliyunProxyVSwitchAction vSwitchAction = new QueryAliyunProxyVSwitchAction(
                                                            sessionId: sessionId,
                                                            conditions: ["aliyunProxyVpcUuid=$vpcId".toString()]
                                                    )
                                                    QueryAliyunProxyVSwitchAction.Result vSwitchResult = vSwitchAction.call()
                                                    vSwitchResult.throwExceptionIfError()
                                                    List l3Networks = vSwitchResult.value.inventories.stream()
                                                            .map { vSwitch -> vSwitch.vpcL3NetworkUuid }.collect(Collectors.toList())
                                                    QueryVmNicAction vmNicAction = new QueryVmNicAction(
                                                            sessionId: sessionId,
                                                            conditions: [
                                                                    "l3NetworkUuid?=${StringUtils.join(l3Networks, ',')}".toString(),
                                                                    "ip=$routeEntry.target".toString()
                                                            ]
                                                    )
                                                    QueryVmNicAction.Result vmNicResult = vmNicAction.call()
                                                    vmNicResult.throwExceptionIfError()
                                                    String instanceId = null
                                                    if (vmNicResult.value.inventories.size() != 0) {
                                                        instanceId = vmNicResult.value.inventories.first().vmInstanceUuid
                                                    }
                                                    if (instanceId != null) {
                                                        entry.put(ECS_INSTANCE_ID, instanceId)
                                                        QueryVmInstanceAction instanceAction = new QueryVmInstanceAction(
                                                                sessionId: sessionId,
                                                                conditions: ["uuid=$instanceId".toString()]
                                                        )
                                                        QueryVmInstanceAction.Result instanceResult = instanceAction.call()
                                                        instanceResult.throwExceptionIfError()
                                                        String nextHopType = null
                                                        if (instanceResult.value.inventories.size() != 0) {
                                                            nextHopType = instanceResult.value.inventories.first().type == "ApplianceVm" ? "RouterInterface" : ECS_INSTANCE
                                                        }
                                                        if (nextHopType != null) {
                                                            entry.put("NextHopType", nextHopType)
                                                        } else {
                                                            entry.put("NextHopType", ECS_INSTANCE)
                                                        }
                                                    } else {
                                                        entry.put(ECS_INSTANCE_ID, routeEntry.target)
                                                        entry.put("NextHopType", ECS_INSTANCE)
                                                    }
                                                } else {
                                                    entry.put(ECS_INSTANCE_ID, routeEntry.target)
                                                    entry.put("NextHopType", ECS_INSTANCE)
                                                }
                                            } else {
                                                entry.put(ECS_INSTANCE_ID, routeEntry.target)
                                                entry.put("NextHopType", ECS_INSTANCE)
                                            }

                                            //NextHops according to the aliyun doc it is an empty list
                                            Map nextHops = new HashMap<>()
                                            entry.put("NextHops", nextHops)
                                            List hops = new ArrayList<>()
                                            nextHops.put("NextHop", hops)
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

    @Override
    Class getZStackAction() {
        return QueryVRouterRouteTableAction.class
    }
}
