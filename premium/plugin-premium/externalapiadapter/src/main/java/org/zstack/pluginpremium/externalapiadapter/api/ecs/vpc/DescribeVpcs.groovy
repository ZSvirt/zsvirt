package org.zstack.pluginpremium.externalapiadapter.api.ecs.vpc


import org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterUtils
import org.zstack.pluginpremium.externalapiadapter.api.BaseQueryAPI
import org.zstack.pluginpremium.externalapiadapter.convert.param.ParameterConversionUtils
import org.zstack.sdk.AliyunProxyVSwitchInventory
import org.zstack.sdk.AliyunProxyVpcInventory
import org.zstack.sdk.QueryAliyunProxyVpcAction
import org.zstack.sdk.QueryVRouterRouteTableAction

import java.util.stream.Collectors

import static org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants.*
/**
 * Created by lining on 2018/5/30.
 */
class DescribeVpcs extends BaseQueryAPI {

    Map routeTableMap = [:]

    @Override
    Class getZStackAction() {
        return QueryAliyunProxyVpcAction.class
    }

    @Override
    void setEcsAPIParamDefaultValue(Map ecsAPIParamMap) {
        super.setEcsAPIParamDefaultValue(ecsAPIParamMap)

        if(!ecsAPIParamMap.containsKey("IsDefault")) {
            ecsAPIParamMap.put("IsDefault", "true")
        }
    }

    @Override
    void afterCallZStackAction(Object zstackActionResult) {
        QueryAliyunProxyVpcAction.Result result = zstackActionResult

        for (AliyunProxyVpcInventory vpcInventory in result.value.inventories) {
            if (vpcInventory.vRouterUuid == null) {
                continue
            }
            QueryVRouterRouteTableAction queryRouteTable = new QueryVRouterRouteTableAction(
                    sessionId: sessionId,
                    conditions: ["attachedRouterRef.virtualRouterVmUuid=${vpcInventory.vRouterUuid}".toString()]
            )
            QueryVRouterRouteTableAction.Result qResult = queryRouteTable.call()
            qResult.throwExceptionIfError()
            routeTableMap.put("${vpcInventory.vRouterUuid}".toString(), qResult.value.inventories.stream().map({inv -> inv.uuid}).collect(Collectors.toList()))
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
                    //this param name is quite confusing, it is actually a list of vpcIds splits by comma
                    ecsParamName = ECS_VPC_VPC_ID
                    ecsParamType = String.class
                    zstackParamName = ZSTACK_UUID
                    zstackParamType = String.class

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

                querySimpleConvert {
                    ecsParamName = ECS_VPC_VPC_NAME
                    zstackParamName = "vpcName"
                }
            }

            convertQueryAPIResponse {

                convertResponseAttribute {
                    ecsAttributeName = "Vpcs"
                    ecsAttributeValue = new HashMap<>()

                    addEcsValueToEcsAPIRsp = { ecsAPIRsp ->
                        ecsAPIRsp.put(ecsAttributeName, ecsAttributeValue)
                    }

                    convertList {
                        ecsAttributeName = "Vpc"
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

                        addListElement = { AliyunProxyVpcInventory vpcInventory ->

                            addConvertResponseAttribute {
                                ecsAttributeValue = new HashMap<>()

                                addEcsValueToFather = { fatherValue ->
                                    fatherValue.add(ecsAttributeValue)
                                }

                                zstackAttributeValue = vpcInventory

                                convertResponseAttribute {
                                    ecsAttributeName = ECS_VPC_VPC_ID

                                    zstackAttributeValue = vpcInventory.uuid

                                    addEcsValueToFather = { fatherValue ->
                                        fatherValue.put(ecsAttributeName, zstackAttributeValue)
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = ECS_API_STATUS_KEY

                                    zstackAttributeValue = vpcInventory.status

                                    addEcsValueToFather = { Map parentMap ->
                                        ParameterConversionUtils.convertVPCRsp(parentMap, vpcInventory.vRouterUuid, sessionId)
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = ECS_VPC_VPC_NAME

                                    zstackAttributeValue = vpcInventory.vpcName

                                    addEcsValueToFather = { fatherValue ->
                                        fatherValue.put(ecsAttributeName, zstackAttributeValue)
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = "VSwitchIds"

                                    zstackAttributeValue = vpcInventory.aliyunProxyVSwitches.stream().map { AliyunProxyVSwitchInventory vSwitch ->
                                        vSwitch.uuid
                                    }.collect(Collectors.toList())
                                    addEcsValueToFather = { fatherValue ->
                                        fatherValue.put(ecsAttributeName, [(ECS_VPC_VSWITCH_ID): zstackAttributeValue])
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = "RouterTableIds"

                                    addEcsValueToFather = {fatherValue ->
                                        fatherValue.put(ecsAttributeName, ["RouterTableIds": routeTableMap.get(vpcInventory.VRouterUuid)])
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = "CidrBlock"

                                    zstackAttributeValue = vpcInventory.cidrBlock

                                    addEcsValueToFather = { fatherValue ->
                                        fatherValue.put(ecsAttributeName, zstackAttributeValue)
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = ECS_API_DESCRIPTION_KEY

                                    zstackAttributeValue = vpcInventory.description

                                    addEcsValueToFather = { fatherValue ->
                                        fatherValue.put(ecsAttributeName, zstackAttributeValue)
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = ECS_API_CREATION_TIME

                                    zstackAttributeValue = ExternalAPIAdapterUtils.formatIso8601Date(vpcInventory.createDate)

                                    addEcsValueToFather = { fatherValue ->
                                        fatherValue.put(ecsAttributeName, zstackAttributeValue)
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = "IsDefault"

                                    zstackAttributeValue = vpcInventory.isDefault

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
