package org.zstack.pluginpremium.externalapiadapter.api.ecs.vpc


import org.zstack.pluginpremium.externalapiadapter.EcsSystemTags
import org.zstack.pluginpremium.externalapiadapter.api.BaseQueryAPI
import org.zstack.pluginpremium.externalapiadapter.convert.param.ParameterConversionUtils
import org.zstack.sdk.QueryAliyunProxyVpcAction
import org.zstack.sdk.QuerySystemTagAction
import org.zstack.sdk.QueryVRouterRouteTableAction
import org.zstack.sdk.SystemTagInventory

import static org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants.*
import static org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterUtils.formatIso8601Date

class DescribeVpcAttribute extends BaseQueryAPI {
    @Override
    Class getZStackAction() {
        return QueryAliyunProxyVpcAction.class
    }

    @Override
    protected void configAPIConversionSpec() {
        spec = config {
            convertQueryAPIParam {
                beforeZstackAPIParam = { Map zstackParamMap ->
                    zstackParamMap.put(ZSTACK_QUERY_CONDITIONS_KEY, [])
                    zstackParamMap.put(ZSTACK_QUERY_REPLYWITHCOUNT_KEY, false)
                }

                querySimpleConvert {
                    ecsParamName = ECS_VPC_VPC_ID
                    zstackParamName = ZSTACK_UUID
                }

                querySimpleConvert {
                    ecsParamName = "IsDefault"
                    zstackParamName = "isDefault"
                }
            }

            convertQueryAPIResponse {
                convertResponseAttribute {
                    ecsAttributeName = ECS_VPC_VPC_NAME
                    addEcsValueToEcsAPIRsp = { Map ecsAPIRsp ->
                        ecsAPIRsp.put(ecsAttributeName, zstackAPIRsp.value.inventories.first().vpcName)
                    }
                }

                convertResponseAttribute {
                    ecsAttributeName = ECS_VPC_VPC_ID
                    addEcsValueToEcsAPIRsp = { Map ecsAPIRsp ->
                        ecsAPIRsp.put(ecsAttributeName, ecsAPIParamMap.get(ecsAttributeName))
                    }
                }

                convertResponseAttribute {
                    ecsAttributeName = ECS_API_DESCRIPTION_KEY
                    addEcsValueToEcsAPIRsp = { Map ecsAPIRsp ->
                        ecsAPIRsp.put(ecsAttributeName, zstackAPIRsp.value.inventories.get(0).description)
                    }
                }

                convertResponseAttribute {
                    ecsAttributeName = "IsDefault"
                    addEcsValueToEcsAPIRsp = { Map ecsAPIRsp ->
                        ecsAPIRsp.put(ecsAttributeName, zstackAPIRsp.value.inventories.get(0).isDefault)
                    }
                }

                convertResponseAttribute {
                    ecsAttributeName = "ClassicLinkEnabled"
                    addEcsValueToEcsAPIRsp = { Map ecsAPIRsp ->
                        ecsAPIRsp.put(ecsAttributeName, false)
                    }
                }

                convertResponseAttribute {
                    ecsAttributeName = "UserCidrs"
                    ecsAttributeValue = new HashMap<>()
                    addEcsValueToEcsAPIRsp = { Map ecsAPIRsp ->
                        ecsAPIRsp.put(ecsAttributeName, ecsAttributeValue)
                    }

                    convertList {
                        ecsAttributeName = "UserCidr"
                        ecsAttributeValue = new ArrayList<>()

                        addEcsValueToFather = { Map parentMap ->
                            parentMap.put(ecsAttributeName, ecsAttributeValue)
                        }
                    }
                }

                convertResponseAttribute {
                    ecsAttributeName = "AssociatedCens"
                    ecsAttributeValue = new HashMap<>()
                    addEcsValueToEcsAPIRsp = { Map ecsAPIRsp ->
                        ecsAPIRsp.put(ecsAttributeName, ecsAttributeValue)
                    }

                    convertList {
                        ecsAttributeName = "AssociatedCen"
                        ecsAttributeValue = new ArrayList<>()

                        addEcsValueToFather = { Map parentMap ->
                            parentMap.put(ecsAttributeName, ecsAttributeValue)
                        }
                    }
                }

                convertResponseAttribute {
                    ecsAttributeName = ECS_API_CREATION_TIME
                    addEcsValueToEcsAPIRsp = { Map ecsAPIRsp ->
                        ecsAPIRsp.put(ecsAttributeName, formatIso8601Date(zstackAPIRsp.value.inventories.get(0).createDate))
                    }
                }

                convertResponseAttribute {
                    ecsAttributeName = "CidrBlock"
                    addEcsValueToEcsAPIRsp = { Map ecsAPIRsp ->
                        ecsAPIRsp.put(ecsAttributeName, zstackAPIRsp.value.inventories.get(0).cidrBlock)
                    }
                }

                convertResponseAttribute {
                    ecsAttributeName = ECS_API_STATUS_KEY

                    addEcsValueToEcsAPIRsp = { Map ecsAPIRsp ->
                        String vRouterUuid = zstackAPIRsp.value.inventories.first().vRouterUuid
                        ParameterConversionUtils.convertVPCRsp(ecsAPIRsp, vRouterUuid, sessionId)
                    }
                }

                convertResponseAttribute {
                    ecsAttributeName = "VSwitchIds"
                    ecsAttributeValue = new HashMap<>()

                    addEcsValueToEcsAPIRsp = { Map ecsAPIRsp ->
                        ecsAPIRsp.put(ecsAttributeName, ecsAttributeValue)
                    }

                    convertList {
                        ecsAttributeName = ECS_VPC_VSWITCH_ID
                        ecsAttributeValue = new ArrayList<>()

                        getZstackAttributeValue = {
                            return zstackAPIRsp.value.inventories.first().aliyunProxyVSwitches
                        }

                        getElementZstackValues = {
                            return zstackAttributeValue
                        }

                        addEcsValueToFather = { Map parentMap ->
                            parentMap.put(ecsAttributeName, ecsAttributeValue)
                        }

                        addListElement = { elementZStackValue ->
                            addConvertResponseAttribute {
                                ecsAttributeValue = elementZStackValue.uuid
                                addEcsValueToFather = { List parentList ->
                                    parentList.add(ecsAttributeValue)
                                }
                            }
                        }
                    }
                }

                convertResponseAttribute {
                    ecsAttributeName = VPC_CLOUD_RESOURCE_CONST.CLOUD_RES
                    ecsAttributeValue = new HashMap<>()
                    addEcsValueToEcsAPIRsp = { Map ecsAPIRsp ->
                        ecsAPIRsp.put(ecsAttributeName, ecsAttributeValue)
                    }

                    convertList {
                        ecsAttributeName = VPC_CLOUD_RESOURCE_CONST.CLOUD_RES_SET_TYPE
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

                        addListElement = { elementZStackValue ->
                            addConvertResponseAttribute {
                                ecsAttributeValue = new HashMap<>()

                                addEcsValueToFather = { List parentList ->
                                    parentList.add(ecsAttributeValue)
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = VPC_CLOUD_RESOURCE_CONST.RES_TYPE
                                    addEcsValueToFather = { Map parentMap ->
                                        parentMap.put(ecsAttributeName, "VSwitch")
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = VPC_CLOUD_RESOURCE_CONST.RES_COUNT

                                    addEcsValueToFather = { Map parentMap ->
                                        List vSwitches = elementZStackValue.aliyunProxyVSwitches
                                        parentMap.put(ecsAttributeName, vSwitches == null ? 0 : vSwitches.size())
                                    }
                                }
                            }

                            addConvertResponseAttribute {
                                ecsAttributeValue = new HashMap<>()

                                addEcsValueToFather = { List parentList ->
                                    parentList.add(ecsAttributeValue)
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = VPC_CLOUD_RESOURCE_CONST.RES_TYPE
                                    addEcsValueToFather = { Map parentMap ->
                                        parentMap.put(ecsAttributeName, "VRouter")
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = VPC_CLOUD_RESOURCE_CONST.RES_COUNT
                                    addEcsValueToFather = { Map parentMap ->
                                        parentMap.put(ecsAttributeName, 1)
                                    }
                                }
                            }

                            addConvertResponseAttribute {
                                ecsAttributeValue = new HashMap<>()

                                addEcsValueToFather = { List parentList ->
                                    parentList.add(ecsAttributeValue)
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = VPC_CLOUD_RESOURCE_CONST.RES_TYPE
                                    addEcsValueToFather = { Map parentMap ->
                                        parentMap.put(ecsAttributeName, "RouteTable")
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = VPC_CLOUD_RESOURCE_CONST.RES_COUNT
                                    addEcsValueToFather = { Map parentMap ->
                                        int count = 0
                                        QueryVRouterRouteTableAction queryRouteTable = new QueryVRouterRouteTableAction(
                                                sessionId: sessionId,
                                                conditions: ["attachedRouterRef.virtualRouterVmUuid=${elementZStackValue.vRouterUuid}".toString()]
                                        )
                                        QueryVRouterRouteTableAction.Result routeTableRes = queryRouteTable.call()
                                        routeTableRes.throwExceptionIfError()
                                        count += routeTableRes.value.inventories.size()

                                        QuerySystemTagAction queryTag = new QuerySystemTagAction(
                                                sessionId: sessionId,
                                                conditions: ["resourceUuid=${elementZStackValue.uuid}".toString()]
                                        )
                                        QuerySystemTagAction.Result tagRes = queryTag.call()
                                        tagRes.throwExceptionIfError()
                                        if (tagRes.value.inventories.size() != 0) {
                                            List<SystemTagInventory> sysTags = tagRes.value.inventories
                                            sysTags.forEach { SystemTagInventory sysTag ->
                                                String routeTableUuid = EcsSystemTags.DEFAULT_ROUTE_TABLE.getTokenByTag(sysTag.tag, EcsSystemTags.DEFAULT_ROUTE_TABLE_TOKEN)
                                                if (routeTableUuid != null) {
                                                    count += 1
                                                }
                                            }
                                        }

                                        parentMap.put(ecsAttributeName, count)
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
    Map getEcsAPIRsp(def zstackAPIRsp) {
        Map res = super.getEcsAPIRsp(zstackAPIRsp)
        res.remove(ECS_QUERY_API_PAGESIZE_KEY)
        res.remove(ECS_QUERY_API_PAGENUMBER_KEY)
        return res
    }
}
