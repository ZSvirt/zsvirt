package org.zstack.pluginpremium.externalapiadapter.api.ecs.slb

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.zstack.network.service.lb.LoadBalancerVO
import org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterGlobalProperty
import org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterUtils
import org.zstack.pluginpremium.externalapiadapter.api.BaseAPI
import org.zstack.pluginpremium.externalapiadapter.convert.param.ParameterConversionUtils
import org.zstack.pluginpremium.externalapiadapter.datatypes.SLBBackendServer
import org.zstack.sdk.*
import org.zstack.utils.gson.JSONObjectUtil

import java.util.stream.Collectors

import static org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants.*

/**
 * Created by Qi Le on 2019/10/9
 */
class DescribeLoadBalancerAttribute extends BaseAPI {
    @Override
    Class getZStackAction() {
        return QueryLoadBalancerAction.class
    }

    @Override
    protected void configAPIConversionSpec() {
        spec = config {
            convertQueryAPIParam {
                beforeZstackAPIParam = { zstackParamMap ->
                    zstackParamMap.put(ZSTACK_QUERY_CONDITIONS_KEY, [])
                    zstackParamMap.put(ZSTACK_QUERY_REPLYWITHCOUNT_KEY, true)
                }

                querySimpleConvert {
                    ecsParamName = "LoadBalancerId"
                    zstackParamName = ZSTACK_UUID
                }
            }

            convertAPIResponse {
                convertResponseAttribute {
                    ecsAttributeName = "LoadBalancerId"

                    getZstackAttributeValue = {
                        return zstackAPIRsp.uuid
                    }

                    addEcsValueToEcsAPIRsp = { Map ecsAPIRsp ->
                        ecsAPIRsp.put(ecsAttributeName, zstackAttributeValue)
                    }
                }

                convertResponseAttribute {
                    ecsAttributeName = ECS_API_REGIONID_KEY

                    getZstackAttributeValue = {
                        String vipId = zstackAPIRsp.vipUuid
                        QueryVipAction vipAction = new QueryVipAction(
                                sessionId: sessionId,
                                conditions: ["$ZSTACK_UUID=$vipId".toString()]
                        )
                        QueryVipAction.Result vipResult = vipAction.call()
                        if (vipResult.error != null || vipResult.value.inventories.size() == 0) {
                            return null
                        }
                        String l3NetworkId = vipResult.value.inventories.first().l3NetworkUuid
                        QueryL3NetworkAction l3Action = new QueryL3NetworkAction(
                                sessionId: sessionId,
                                conditions: ["$ZSTACK_UUID=$l3NetworkId".toString()]
                        )
                        QueryL3NetworkAction.Result l3Result = l3Action.call()
                        if (l3Result.error != null || l3Result.value.inventories.size() == 0) {
                            return null
                        }
                        String zoneId = l3Result.value.inventories.first().zoneUuid
                        QueryZoneAction zoneAction = new QueryZoneAction(
                                sessionId: sessionId,
                                conditions: ["$ZSTACK_UUID=$zoneId".toString()]
                        )
                        QueryZoneAction.Result zoneResult = zoneAction.call()
                        if (zoneResult.error != null || zoneResult.value.inventories.size() == 0) {
                            return null
                        }
                        return zoneResult.value.inventories.first().name
                    }

                    addEcsValueToEcsAPIRsp = { Map ecsAPIRsp ->
                        if (zstackAttributeValue != null) {
                            ecsAPIRsp[ecsAttributeName] = zstackAttributeValue
                        } else {
                            ecsAPIRsp[ecsAttributeName] = ecsAPIParamMap[ecsAttributeName]
                        }
                    }
                }

                convertResponseAttribute {
                    ecsAttributeName = "RegionIdAlias"

                    addEcsValueToEcsAPIRsp = { Map ecsAPIRsp ->
                        ecsAPIRsp.put(ecsAttributeName, ecsAPIParamMap.get(ecsAttributeName))
                    }
                }

                convertResponseAttribute {
                    ecsAttributeName = "LoadBalancerName"

                    getZstackAttributeValue = {
                        return zstackAPIRsp.name
                    }

                    addEcsValueToEcsAPIRsp = { Map ecsAPIRsp ->
                        ecsAPIRsp.put(ecsAttributeName, zstackAttributeValue)
                    }
                }

                convertResponseAttribute {
                    ecsAttributeName = "LoadBalancerStatus"
                    ecsAttributeValue = "active"

                    addEcsValueToEcsAPIRsp = { Map ecsAPIRsp ->
                        ecsAPIRsp.put(ecsAttributeName, ecsAttributeValue)
                    }
                }

                convertResponseAttribute {
                    ecsAttributeName = ECS_SLB_ADDRESS

                    getZstackAttributeValue = {
                        return zstackAPIRsp
                    }

                    addEcsValueToEcsAPIRsp = { Map ecsAPIRsp ->
                        QueryVipAction actionVip = new QueryVipAction(
                                sessionId: sessionId,
                                conditions: ["uuid=$zstackAttributeValue.vipUuid".toString()]
                        )
                        QueryVipAction.Result vipResult = actionVip.call()
                        if (vipResult.error != null || vipResult.value.inventories.size() == 0) {
                            return
                        }
                        VipInventory vip = vipResult.value.inventories.first()
                        ecsAPIRsp.put(ecsAttributeName, vip.ip)

                        if (vip.l3NetworkUuid == ExternalAPIAdapterGlobalProperty.PUBLICL3NETWORKUUID) {
                            ecsAPIRsp[ECS_SLB_ADDRESS_TYPE] = ECS_ADDRESS_TYPE_INTERNET
                            ecsAPIRsp[ECS_SLB_NETWORK_TYPE] = ECS_NETWORK_TYPE_CLASSIC
                            return
                        }

                        ecsAPIRsp[ECS_SLB_ADDRESS_TYPE] = ECS_ADDRESS_TYPE_INTRANET
                        ecsAPIRsp[ECS_SLB_NETWORK_TYPE] = ECS_NETWORK_TYPE_VPC

                        QueryAliyunProxyVSwitchAction actionVS = new QueryAliyunProxyVSwitchAction(
                                sessionId: sessionId,
                                conditions: ["vpcL3NetworkUuid=${vip.l3NetworkUuid}".toString()]
                        )
                        QueryAliyunProxyVSwitchAction.Result vsResult = actionVS.call()
                        if (vsResult.error != null || vsResult.value.inventories.size() == 0) {
                            return
                        }
                        AliyunProxyVSwitchInventory vs = vsResult.value.inventories.first()
                        ecsAPIRsp.put(ECS_VPC_VPC_ID, vs.aliyunProxyVpcUuid)
                        ecsAPIRsp.put(ECS_VPC_VSWITCH_ID, vs.uuid)
                    }
                }

                convertResponseAttribute {
                    ecsAttributeName = ECS_NETWORK_BANDWIDTH
                    ecsAttributeValue = 20

                    addEcsValueToEcsAPIRsp = { Map ecsAPIRsp ->
                        ecsAPIRsp.put(ecsAttributeName, ecsAttributeValue)
                    }
                }

                convertResponseAttribute {
                    ecsAttributeName = "CreateTime"

                    getZstackAttributeValue = {
                        return zstackAPIRsp
                    }

                    addEcsValueToEcsAPIRsp = { Map ecsAPIRsp ->
                        ecsAPIRsp.put(ecsAttributeName, ExternalAPIAdapterUtils.formatIso8601Date(zstackAttributeValue.createDate))
                    }
                }

                convertResponseAttribute {
                    ecsAttributeName = "ListenerPorts"
                    ecsAttributeValue = new HashMap<>()

                    getZstackAttributeValue = {
                        return zstackAPIRsp
                    }

                    addEcsValueToEcsAPIRsp = { Map ecsAPIRsp ->
                        ecsAPIRsp.put(ecsAttributeName, ecsAttributeValue)
                        List ports = zstackAttributeValue.listeners.stream()
                                .map { listener -> listener.loadBalancerPort }.collect(Collectors.toList())
                        ecsAttributeValue.put("ListenerPort", ports)
                    }
                }

                convertResponseAttribute {
                    ecsAttributeName = "ListenerPortsAndProtocol"
                    ecsAttributeValue = new HashMap<>()

                    addEcsValueToEcsAPIRsp = { Map ecsAPIRsp ->
                        ecsAPIRsp.put(ecsAttributeName, ecsAttributeValue)
                    }

                    convertList {
                        ecsAttributeName = "ListenerPortAndProtocol"
                        ecsAttributeValue = new ArrayList<>()

                        getZstackAttributeValue = {
                            return zstackAPIRsp.listeners
                        }

                        getElementZstackValues = {
                            return zstackAttributeValue
                        }

                        addEcsValueToFather = { Map parentMap ->
                            parentMap.put(ecsAttributeName, ecsAttributeValue)
                        }

                        addListElement = { LoadBalancerListenerInventory listenerInventory ->
                            addConvertResponseAttribute {
                                ecsAttributeValue = new HashMap<>()

                                addEcsValueToFather = { List parentList ->
                                    parentList.add(ecsAttributeValue)
                                }

                                zstackAttributeValue = listenerInventory

                                convertResponseAttribute {
                                    ecsAttributeName = "ListenerPort"
                                    zstackAttributeValue = listenerInventory.loadBalancerPort

                                    addEcsValueToFather = { Map parentMap ->
                                        parentMap.put(ecsAttributeName, zstackAttributeValue)
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = "ListenerProtocol"
                                    zstackAttributeValue = listenerInventory.protocol

                                    addEcsValueToFather = { Map parentMap ->
                                        parentMap.put(ecsAttributeName, zstackAttributeValue)
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = ECS_API_DESCRIPTION_KEY
                                    zstackAttributeValue = listenerInventory.description

                                    addEcsValueToFather = { Map parentMap ->
                                        parentMap.put(ecsAttributeName, zstackAttributeValue)
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = "ForwardPort"
                                    zstackAttributeValue = listenerInventory.instancePort

                                    addEcsValueToFather = { Map parentMap ->
                                        parentMap.put(ecsAttributeName, zstackAttributeValue)
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = "ListenerForward"
                                    zstackAttributeValue = "yes"

                                    addEcsValueToFather = { Map parentMap ->
                                        parentMap.put(ecsAttributeName, zstackAttributeValue)
                                    }
                                }
                            }
                        }
                    }
                }

                convertResponseAttribute {
                    ecsAttributeName = ECS_SLB_BACKEND_SERVERS
                    ecsAttributeValue = new HashMap<>()

                    addEcsValueToEcsAPIRsp = { Map ecsAPIRsp ->
                        ecsAPIRsp.put(ecsAttributeName, ecsAttributeValue)
                    }

                    convertList {
                        ecsAttributeName = ECS_SLB_BACKEND_SERVER
                        ecsAttributeValue = new ArrayList<>()

                        getZstackAttributeValue = {
                            String slbUuid = zstackAPIRsp.uuid
                            QuerySystemTagAction querySystemTagAction = new QuerySystemTagAction(
                                    sessionId: sessionId,
                                    conditions: [
                                            "resourceType=${LoadBalancerVO.class.getSimpleName()}".toString(),
                                            "resourceUuid=${slbUuid}".toString()
                                    ]
                            )
                            QuerySystemTagAction.Result result = querySystemTagAction.call()
                            result.throwExceptionIfError()
                            List res = ParameterConversionUtils.getSLBBackendServersInfoFromTags(sessionId, result.value.inventories)
                            return res
                        }

                        getElementZstackValues = {
                            return zstackAttributeValue
                        }

                        addEcsValueToFather = { Map parentMap ->
                            parentMap.put(ecsAttributeName, ecsAttributeValue)
                        }

                        addListElement = { SLBBackendServer serverInfo ->
                            addConvertResponseAttribute {
                                ecsAttributeValue = new HashMap<>()

                                addEcsValueToFather = { List parentList ->
                                    parentList.add(ecsAttributeValue)
                                }

                                zstackAttributeValue = serverInfo

                                convertResponseAttribute {
                                    ecsAttributeName = "ServerId"
                                    zstackAttributeValue = serverInfo.serverId

                                    addEcsValueToFather = { Map parentMap ->
                                        parentMap.put(ecsAttributeName, zstackAttributeValue)
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = ECS_API_TYPE_KEY
                                    zstackAttributeValue = serverInfo.type

                                    addEcsValueToFather = { Map parentMap ->
                                        parentMap.put(ecsAttributeName, zstackAttributeValue)
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = "Weight"
                                    zstackAttributeValue = serverInfo.weight

                                    addEcsValueToFather = { Map parentMap ->
                                        parentMap.put(ecsAttributeName, zstackAttributeValue)
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = ECS_API_DESCRIPTION_KEY
                                    zstackAttributeValue = ""

                                    addEcsValueToFather = { Map parentMap ->
                                        parentMap.put(ecsAttributeName, zstackAttributeValue)
                                    }
                                }
                            }
                        }
                    }
                }

                convertResponseAttribute {
                    ecsAttributeName = "AddressIPVersion"
                    ecsAttributeValue = "ipv4"

                    addEcsValueToEcsAPIRsp = { Map ecsAPIRsp ->
                        ecsAPIRsp.put(ecsAttributeName, ecsAttributeValue)
                    }
                }

                convertResponseAttribute {
                    ecsAttributeName = "CreateTimeStamp"

                    getZstackAttributeValue = {
                        return zstackAPIRsp.createDate.getTime()
                    }

                    addEcsValueToEcsAPIRsp = { Map ecsAPIRsp ->
                        ecsAPIRsp.put(ecsAttributeName, zstackAttributeValue)
                    }
                }

                convertResponseAttribute {
                    ecsAttributeName = "DeleteProtection"
                    ecsAttributeValue = "off"

                    addEcsValueToEcsAPIRsp = { Map ecsAPIRsp ->
                        ecsAPIRsp.put(ecsAttributeName, ecsAttributeValue)
                    }
                }
            }
        }
    }

    @Override
    Object callZStackAction() {
        Gson gson = new GsonBuilder().create()
        QueryLoadBalancerAction action = gson.fromJson(JSONObjectUtil.toJsonString(zstackAPIParamMap), QueryLoadBalancerAction.class)
        def result = action.call()
        result.throwExceptionIfError()
        if (result.value.inventories == null || result.value.inventories.size() == 0) {
            return null
        }

        LoadBalancerInventory loadBalancer = result.value.inventories.first()

        this.afterCallZStackAction(loadBalancer)

        return loadBalancer
    }
}
