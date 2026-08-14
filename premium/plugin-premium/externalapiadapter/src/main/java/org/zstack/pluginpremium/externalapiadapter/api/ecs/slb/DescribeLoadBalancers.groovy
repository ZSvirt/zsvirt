package org.zstack.pluginpremium.externalapiadapter.api.ecs.slb

import org.apache.commons.lang.StringUtils
import org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterGlobalProperty
import org.zstack.pluginpremium.externalapiadapter.api.BaseQueryAPI
import org.zstack.pluginpremium.externalapiadapter.exception.APIParamConvertException
import org.zstack.sdk.*

import static org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants.*

/**
 * Created by Qi Le on 2019-07-30
 */
class DescribeLoadBalancers extends BaseQueryAPI {

    @Override
    Class getZStackAction() {
        return QueryLoadBalancerAction.class
    }

    @Override
    protected void configAPIConversionSpec() {
        spec = config {
            convertQueryAPIParam {
                beforeZstackAPIParam = { zstackParamMap ->
                    zstackParamMap.put(ZSTACK_QUERY_REPLYWITHCOUNT_KEY, true)
                    zstackParamMap.put(ZSTACK_QUERY_CONDITIONS_KEY, [])
                }

                querySimpleConvert {
                    ecsParamName = "LoadBalancerId"
                    zstackParamName = ZSTACK_UUID

                    putZstackParamValue = { Map zstackParamMap, String ecsAPIParamValue ->
                        List conditions = zstackParamMap.get(ZSTACK_QUERY_CONDITIONS_KEY)
                        conditions.add("${zstackParamName}?=${ecsAPIParamValue}".toString())
                    }
                }

                querySimpleConvert {
                    ecsParamName = "LoadBalancerName"
                    zstackParamName = ZSTACK_NAME

                    putZstackParamValue = { zstackParamMap, zstackParamValue ->
                        List conditions = zstackParamMap.get(ZSTACK_QUERY_CONDITIONS_KEY)
                        conditions.add("${zstackParamName}?=${zstackParamValue}".toString())
                    }
                }

                querySimpleConvert {
                    ecsParamName = ECS_SLB_ADDRESS
                    zstackParamName = "vip.ip"
                }

                querySimpleConvert {
                    ecsParamName = "LoadBalancerStatus"
                    zstackParamName = ZSTACK_API_STATE_KEY

                    putZstackParamValue = { Map zstackParamMap, String ecsParamValue ->
                        List conditions = zstackParamMap.get(ZSTACK_QUERY_CONDITIONS_KEY)

                        Map validInputValue = [
                                "active" : "Enabled",
                                "inactive" : "Disabled",
                                "locked" : "Disabled"
                        ]
                        def validValue = ["Enabled", "Disabled"]
                        if (ecsParamValue == null) {
                            conditions.add("${zstackParamName}?=${StringUtils.join(validValue, ",")}".toString())
                            return
                        }

                        def result = validInputValue.get(ecsParamValue)
                        if (result == null) {
                            throw new APIParamConvertException(ecsParamName, "Status[value: $ecsParamValue] is an invalid value".toString())
                        }
                        conditions.add("${zstackParamName}=${validInputValue.get(ecsParamValue)}".toString())
                    }
                }

                querySimpleConvert {
                    ecsParamName = "ServerId"
                    zstackParamName = "listeners.vmNic.uuid"
                    /*
                    we may not allow query vm instance id, due to we don't support to add vm instance to listener
                     */
//                    zstackParamName = "listeners.vmNic.vmInstanceUuid"
                }

                querySimpleConvert {
                    ecsParamName = "ServerIntranetAddress"
                    zstackParamName = "listeners.vmNic.ip"
                }
            }

            convertQueryAPIResponse {
                convertResponseAttribute {
                    ecsAttributeName = ECS_API_REGIONID_KEY
                    addEcsValueToEcsAPIRsp = { Map ecsAPIRsp ->
                        ecsAPIRsp.put(ecsAttributeName, ecsAPIParamMap.get(ecsAttributeName))
                    }
                }

                convertResponseAttribute {
                    ecsAttributeName = "LoadBalancers"
                    ecsAttributeValue = new HashMap<>()

                    addEcsValueToEcsAPIRsp = { Map ecsAPIRsp ->
                        ecsAPIRsp.put(ecsAttributeName, ecsAttributeValue)
                    }

                    convertList {
                        ecsAttributeName = "LoadBalancer"
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

                        addListElement = { LoadBalancerInventory loadBalancerInv ->
                            addConvertResponseAttribute {
                                ecsAttributeValue = new HashMap<>()

                                addEcsValueToFather = { List parentList ->
                                    parentList.add(ecsAttributeValue)
                                }

                                zstackAttributeValue = loadBalancerInv

                                convertResponseAttribute {
                                    ecsAttributeName = "LoadBalancerId"

                                    zstackAttributeValue = loadBalancerInv.uuid

                                    addEcsValueToFather = { Map parentMap ->
                                        parentMap.put(ecsAttributeName, zstackAttributeValue)
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = "LoadBalancerName"

                                    zstackAttributeValue = loadBalancerInv.name

                                    addEcsValueToFather = { Map parentMap ->
                                        parentMap.put(ecsAttributeName, zstackAttributeValue)
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = "LoadBalancerStatus"

                                    zstackAttributeValue = loadBalancerInv.state

                                    addEcsValueToFather = { Map parentMap ->
                                        if (zstackAttributeValue == "Enabled") {
                                            parentMap.put(ecsAttributeName, "active")
                                        } else {
                                            parentMap.put(ecsAttributeName, "inactive")
                                        }
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = ECS_SLB_ADDRESS

                                    zstackAttributeValue = loadBalancerInv.vipUuid

                                    addEcsValueToFather = { Map parentMap ->
                                        QueryVipAction action = new QueryVipAction(
                                                sessionId: sessionId,
                                                conditions: ["uuid=${zstackAttributeValue}".toString()]
                                        )
                                        QueryVipAction.Result result = action.call()
                                        result.throwExceptionIfError()

                                        if (result.value.inventories.size() == 0) {
                                            throw new APIParamConvertException(ECS_SLB_ADDRESS, "Vip the load balancer[id:${loadBalancerInv.uuid}] bound to is not found".toString())
                                        }
                                        VipInventory vip = result.value.inventories.first()
                                        parentMap[ecsAttributeName] = vip.ip
                                        //Address Type and Vpc Info
                                        if (vip.l3NetworkUuid == ExternalAPIAdapterGlobalProperty.PUBLICL3NETWORKUUID) {
                                            parentMap[ECS_SLB_ADDRESS_TYPE] = ECS_ADDRESS_TYPE_INTERNET
                                            parentMap[ECS_SLB_NETWORK_TYPE] = ECS_NETWORK_TYPE_CLASSIC
                                            return
                                        } else {
                                            parentMap[ECS_SLB_ADDRESS_TYPE] = ECS_ADDRESS_TYPE_INTRANET
                                            parentMap[ECS_SLB_NETWORK_TYPE] = ECS_NETWORK_TYPE_VPC
                                        }
                                        QueryAliyunProxyVSwitchAction vSwitchAction = new QueryAliyunProxyVSwitchAction(
                                                sessionId: sessionId,
                                                conditions: ["$ZSTACK_UUID=$vip.l3NetworkUuid".toString()]
                                        )
                                        QueryAliyunProxyVSwitchAction.Result vSwitchResult = vSwitchAction.call()
                                        if (!(vSwitchResult.error != null || vSwitchResult.value.inventories.size() == 0)) {
                                            AliyunProxyVSwitchInventory vSwitch = vSwitchResult.value.inventories.first()
                                            parentMap[ECS_VPC_VSWITCH_ID] = vSwitch.uuid
                                            parentMap[ECS_VPC_VPC_ID] = vSwitch.aliyunProxyVpcUuid
                                        }
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = ECS_API_REGIONID_KEY
                                    addEcsValueToFather = { Map parentMap ->
                                        parentMap.put(ecsAttributeName, ecsAPIParamMap.get(ecsAttributeName))
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = "CreateTime"

                                    zstackAttributeValue = loadBalancerInv.createDate

                                    addEcsValueToFather = { Map parentMap ->
                                        parentMap.put(ecsAttributeName, zstackAttributeValue.toString())
                                        parentMap.put("CreateTimeStamp", zstackAttributeValue.getTime())
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = "AddressIPVersion"

                                    zstackAttributeValue = "ipv4"

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
}
