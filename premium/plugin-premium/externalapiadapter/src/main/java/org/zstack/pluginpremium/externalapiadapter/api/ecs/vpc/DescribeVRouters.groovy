package org.zstack.pluginpremium.externalapiadapter.api.ecs.vpc


import org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterUtils
import org.zstack.pluginpremium.externalapiadapter.api.BaseQueryAPI
import org.zstack.pluginpremium.externalapiadapter.exception.APIParamConvertException
import org.zstack.sdk.*

import static org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants.*
/**
 * Created by Qi Le on 2019/11/12
 */
class DescribeVRouters extends BaseQueryAPI {
    @Override
    protected void configAPIConversionSpec() {
        spec = config {
            convertAPIParam {
                beforeZstackAPIParam = { zstackParamMap ->
                    zstackParamMap.put(ZSTACK_QUERY_REPLYWITHCOUNT_KEY, true)
                    zstackParamMap.put(ZSTACK_QUERY_CONDITIONS_KEY, [])
                }

                querySimpleConvert {
                    ecsParamName = ECS_API_REGIONID_KEY
                    zstackParamName = "zoneUuid"

                    putZstackParamValue = {Map zstackParamMap, String zstackParamValue ->
                        QueryZoneAction zoneAction = new QueryZoneAction(
                                sessionId: sessionId,
                                conditions: ["name=$zstackParamValue".toString()]
                        )
                        QueryZoneAction.Result zoneResult = zoneAction.call()
                        zoneResult.throwExceptionIfError()
                        if (zoneResult.value.inventories.size() == 0) {
                            throw new APIParamConvertException(ECS_API_REGIONID_KEY, "Region not found.")
                        }
                        List conditions = zstackParamMap.get(ZSTACK_QUERY_CONDITIONS_KEY)
                        conditions.add("$zstackParamName=${zoneResult.value.inventories.first().uuid}".toString())
                    }
                }

                querySimpleConvert {
                    ecsParamName = ECS_VPC_VROUTER_ID
                    zstackParamName = ZSTACK_UUID
                }
            }

            convertAPIResponse {
                convertResponseAttribute {
                    ecsAttributeName = "VRouters"
                    ecsAttributeValue = new HashMap<>()

                    addEcsValueToEcsAPIRsp = { Map ecsAPIRsp ->
                        ecsAPIRsp.put(ecsAttributeName, ecsAttributeValue)
                    }

                    convertList {
                        ecsAttributeName = "VRouter"
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

                        addListElement = { VpcRouterVmInventory vpcInv ->
                            addConvertResponseAttribute {
                                ecsAttributeValue = new HashMap<>()

                                addEcsValueToFather = { List parentList ->
                                    parentList.add(ecsAttributeValue)
                                }

                                zstackAttributeValue = vpcInv

                                convertResponseAttribute {
                                    ecsAttributeName = ECS_API_CREATION_TIME
                                    zstackAttributeValue = ExternalAPIAdapterUtils.formatIso8601Date(vpcInv.createDate)

                                    addEcsValueToFather = { Map parentMap ->
                                        parentMap.put(ecsAttributeName, zstackAttributeValue)
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = ECS_API_DESCRIPTION_KEY
                                    zstackAttributeValue = vpcInv.description

                                    addEcsValueToFather = { Map parentMap ->
                                        parentMap.put(ecsAttributeName, zstackAttributeValue)
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = ECS_API_REGIONID_KEY
                                    zstackAttributeValue = vpcInv.zoneUuid

                                    addEcsValueToFather = { Map parentMap ->
                                        QueryZoneAction zoneAction = new QueryZoneAction(
                                                sessionId: sessionId,
                                                conditions: ["uuid=$zstackAttributeValue".toString()]
                                        )
                                        QueryZoneAction.Result zoneResult = zoneAction.call()
                                        if (zoneResult.error == null && zoneResult.value.inventories.size() != 0) {
                                            zstackAttributeValue = zoneResult.value.inventories.first().name
                                        }
                                        parentMap.put(ecsAttributeName, zstackAttributeValue)
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = ECS_VPC_VROUTER_ID
                                    zstackAttributeValue = vpcInv.uuid

                                    addEcsValueToFather = { Map parentMap ->
                                        parentMap.put(ecsAttributeName, zstackAttributeValue)
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = ECS_VPC_VROUTER_NAME
                                    zstackAttributeValue = vpcInv.name

                                    addEcsValueToFather = { Map parentMap ->
                                        parentMap.put(ecsAttributeName, zstackAttributeValue)
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = ECS_VPC_VPC_ID
                                    zstackAttributeValue = vpcInv.uuid

                                    addEcsValueToFather = { Map parentMap ->
                                        QueryAliyunProxyVpcAction vpcAction = new QueryAliyunProxyVpcAction(
                                                sessionId: sessionId,
                                                conditions: ["vRouterUuid=$zstackAttributeValue".toString()]
                                        )
                                        QueryAliyunProxyVpcAction.Result vpcResult = vpcAction.call()

                                        if (vpcResult.error == null && vpcResult.value.inventories.size() != 0) {
                                            zstackAttributeValue = vpcResult.value.inventories.first().uuid
                                        }

                                        parentMap.put(ecsAttributeName, zstackAttributeValue)
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = "RouteTableIds"
                                    ecsAttributeValue = new HashMap<>()
                                    zstackAttributeValue = new ArrayList<>()

                                    addEcsValueToFather = { Map parentMap ->
                                        (parentMap[ecsAttributeName] = ecsAttributeValue)
                                        (ecsAttributeValue[ECS_ROUTE_TABLE_ID] = zstackAttributeValue)
                                        QueryVRouterRouteTableAction routeTableAction = new QueryVRouterRouteTableAction(
                                                sessionId: sessionId,
                                                conditions: ["attachedRouterRefs.virtualRouterVm.uuid=$vpcInv.uuid".toString()]
                                        )
                                        QueryVRouterRouteTableAction.Result routeTableResult = routeTableAction.call()
                                        if (routeTableResult.error == null && routeTableResult.value.inventories.size() != 0) {
                                            zstackAttributeValue.add(routeTableResult.value.inventories.first().uuid)
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
        return QueryVpcRouterAction.class
    }
}
