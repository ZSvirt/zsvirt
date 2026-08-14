package org.zstack.pluginpremium.externalapiadapter.api.ecs.slb

import org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterGlobalProperty
import org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterUtils
import org.zstack.pluginpremium.externalapiadapter.api.BaseAPI
import org.zstack.pluginpremium.externalapiadapter.exception.APIParamConvertException
import org.zstack.sdk.*

import static org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants.*

/**
 * Created by Qi Le on 2019-07-26
 */
class CreateLoadBalancer extends BaseAPI {

    private String vipUuid
    private String slbUuid

    @Override
    Class getZStackAction() {
        return CreateLoadBalancerAction.class
    }

    @Override
    protected void configAPIConversionSpec() {
        spec = config {
            convertAPIParam {
                simpleConvert {
                    ecsParamName = "LoadBalancerName"
                    zstackParamName = ZSTACK_NAME
                }

                zstackNeedParam {
                    zstackParamName = "vipUuid"
                    getZstackValue = { ecsParamMap, zstackParamMap ->
                        return vipUuid
                    }
                }

                zstackNeedParam {
                    zstackParamName = ZSTACK_RESOURCEUUID_KEY
                    getZstackValue = { ecsParamMap, zstackParamMap ->
                        return slbUuid
                    }
                }
            }

            convertAPIResponse {

                convertResponseAttribute {
                    ecsAttributeName = "LoadBalancerId"

                    getZstackAttributeValue = {
                        zstackAPIRsp.value.inventory.uuid
                    }

                    addEcsValueToEcsAPIRsp = { ecsApiRsp ->
                        ecsApiRsp.put(ecsAttributeName, zstackAttributeValue)
                    }
                }

                convertResponseAttribute {
                    ecsAttributeName = ECS_SLB_ADDRESS

                    getZstackAttributeValue = {
                        LoadBalancerInventory loadBalancer = zstackAPIRsp.value.inventory
                        QueryVipAction queryVipAction = new QueryVipAction(
                                sessionId: sessionId,
                                conditions: ["uuid=${loadBalancer.vipUuid}".toString()]
                        )
                        QueryVipAction.Result result = queryVipAction.call()
                        result.throwExceptionIfError()
                        if (result.value.inventories.size() == 0) {
                            return null
                        }
                        return ((VipInventory) result.value.inventories.get(0)).ip
                    }

                    addEcsValueToEcsAPIRsp = { ecsApiRsp ->
                        ecsApiRsp.put(ecsAttributeName, zstackAttributeValue)
                    }
                }

                convertResponseAttribute {
                    ecsAttributeName = "LoadBalancerName"

                    getZstackAttributeValue = {
                        return zstackAPIRsp.value.inventory.name
                    }

                    addEcsValueToEcsAPIRsp = { ecsApiRsp ->
                        ecsApiRsp.put(ecsAttributeName, zstackAttributeValue)
                    }
                }

                convertResponseAttribute {
                    ecsAttributeName = "AddressIPVersion"

                    getZstackAttributeValue = {
                        return "ipv4"
                    }

                    addEcsValueToEcsAPIRsp = { ecsApiRsp ->
                        ecsApiRsp.put(ecsAttributeName, zstackAttributeValue)
                    }
                }

                convertResponseAttribute {
                    ecsAttributeName = ECS_SLB_NETWORK_TYPE

                    getZstackAttributeValue = {
                        return ecsAPIParamMap[ECS_VPC_VSWITCH_ID] ? ECS_NETWORK_TYPE_VPC : ECS_NETWORK_TYPE_CLASSIC
                    }

                    addEcsValueToEcsAPIRsp = { ecsApiRsp ->
                        ecsApiRsp.put(ecsAttributeName, zstackAttributeValue)
                    }
                }

                convertResponseAttribute {
                    ecsAttributeName = ECS_VPC_VSWITCH_ID

                    getZstackAttributeValue = {
                        return ecsAPIParamMap[ecsAttributeName]
                    }

                    addEcsValueToEcsAPIRsp = { Map ecsApiRsp ->
                        if (zstackAttributeValue != null) {
                            ecsApiRsp[ecsAttributeName] = zstackAttributeValue
                            String vpcId = ecsAPIParamMap[ECS_VPC_VPC_ID]
                            if (vpcId == null) {
                                QueryAliyunProxyVSwitchAction action = new QueryAliyunProxyVSwitchAction(
                                        sessionId: sessionId,
                                        conditions: ["$ZSTACK_UUID=$zstackAttributeValue".toString()]
                                )
                                QueryAliyunProxyVSwitchAction.Result result = action.call()
                                if (!(result.error != null || result.value.inventories.size() == 0)) {
                                    vpcId = result.value.inventories.first().aliyunProxyVpcUuid
                                }
                            }
                            ecsApiRsp[ECS_VPC_VPC_ID] = vpcId
                        }
                    }
                }
            }
        }
    }

    @Override
    String call(Map ecsAPIParamMap) {
        slbUuid = ExternalAPIAdapterUtils.randomUUID()

        //create vip
        String vSwitchId = ecsAPIParamMap[ECS_VPC_VSWITCH_ID]
        String address = ecsAPIParamMap[ECS_SLB_ADDRESS]

        CreateVipAction cVipAction = new CreateVipAction(
                sessionId: sessionId,
                name: "vip-for-SLB-${slbUuid}".toString(),
        )

        if (!vSwitchId) {
            cVipAction.l3NetworkUuid = ExternalAPIAdapterGlobalProperty.PUBLICL3NETWORKUUID
        } else {
            QueryAliyunProxyVSwitchAction action = new QueryAliyunProxyVSwitchAction(
                    sessionId: sessionId,
                    conditions: ["$ZSTACK_UUID=$vSwitchId".toString()]
            )
            QueryAliyunProxyVSwitchAction.Result result = action.call()
            if (result.error != null || result.value.inventories.size() == 0) {
                throw new APIParamConvertException(ECS_VPC_VSWITCH_ID, "Cannot find vswitch [id:$vSwitchId]".toString())
            }
            cVipAction.l3NetworkUuid = result.value.inventories.first().vpcL3NetworkUuid
        }

        if (address != null) {
            cVipAction.requiredIp = address
        }

        CreateVipAction.Result cVipResult = cVipAction.call()
        cVipResult.throwExceptionIfError()
        vipUuid = cVipResult.value.inventory.uuid
        return super.call(ecsAPIParamMap)
    }
}
