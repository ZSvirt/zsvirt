package org.zstack.pluginpremium.externalapiadapter.api.ecs.eip

import org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterGlobalProperty
import org.zstack.pluginpremium.externalapiadapter.api.BaseAPI
import org.zstack.sdk.CreateEipAction
import org.zstack.sdk.CreateVipAction
import org.zstack.sdk.SetVipQosAction
import org.zstack.sdk.VipInventory
import org.zstack.utils.data.SizeUnit

import static org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants.*

/**
 * @Author: fubang
 * @Date: 2018/5/19
 */
class AllocateEipAddress extends BaseAPI {
    @Override
    protected void configAPIConversionSpec() {
        spec = config {
            convertAPIParam {
                zstackNeedParam {
                    zstackParamName = "vipUuid"

                    getZstackValue = { Map ecsParamMap, Map zstackParamMap ->
                        CreateVipAction action = new CreateVipAction()
                        action.sessionId = sessionId
                        action.name = "untitled-vip"
                        action.l3NetworkUuid = ExternalAPIAdapterGlobalProperty.PUBLICL3NETWORKUUID

                        CreateVipAction.Result result = action.call()
                        result.throwExceptionIfError()

                        VipInventory vipInventory = result.value.inventory

                        String brandWidth = ecsAPIParamMap.get(ECS_NETWORK_BANDWIDTH) ?: "40"
                        long brandWidthValue = SizeUnit.MEGABYTE.toByte(Integer.parseInt(brandWidth)) * 8
                        SetVipQosAction setVipQosAction = new SetVipQosAction()
                        setVipQosAction.sessionId  =sessionId
                        setVipQosAction.uuid = vipInventory.uuid
                        setVipQosAction.inboundBandwidth = brandWidthValue
                        setVipQosAction.outboundBandwidth = brandWidthValue

                        SetVipQosAction.Result setVipQosResult = setVipQosAction.call()
                        setVipQosResult.throwExceptionIfError()

                        return vipInventory
                    }

                    putZstackParamValue = { Map zstackParamMap, VipInventory zstackParamValue ->
                        zstackParamMap.put(zstackParamName, zstackParamValue.uuid)
                        zstackParamMap.put(ZSTACK_NAME, zstackParamValue.ip)
                    }
                }
            }

            convertAPIResponse {
                convertResponseAttribute {
                    ecsAttributeName = ECS_EIP_ADDRESS

                    getZstackAttributeValue = {
                        return zstackAPIRsp.value.inventory.vipIp
                    }

                    addEcsValueToEcsAPIRsp = { ecsAPIRsp ->
                        ecsAPIRsp.put(ecsAttributeName, zstackAttributeValue)
                    }
                }

                convertResponseAttribute {
                    ecsAttributeName = ECS_EIP_ALLOCATION_ID

                    getZstackAttributeValue = {
                        return zstackAPIRsp.value.inventory.uuid
                    }

                    addEcsValueToEcsAPIRsp = { ecsAPIRsp ->
                        ecsAPIRsp.put(ecsAttributeName, zstackAttributeValue)
                    }
                }
            }
        }
    }

    @Override
    Class getZStackAction() {
        return CreateEipAction.class
    }
}
