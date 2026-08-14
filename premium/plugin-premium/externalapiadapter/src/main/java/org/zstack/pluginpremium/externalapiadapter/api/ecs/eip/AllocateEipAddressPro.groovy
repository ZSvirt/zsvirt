package org.zstack.pluginpremium.externalapiadapter.api.ecs.eip

import org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants
import org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterGlobalProperty
import org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterUtils
import org.zstack.pluginpremium.externalapiadapter.api.BaseAPI
import org.zstack.pluginpremium.externalapiadapter.exception.APIAdapterSpecifiedErrorException
import org.zstack.sdk.CreateEipAction
import org.zstack.sdk.CreateVipAction
import org.zstack.sdk.SetVipQosAction
import org.zstack.sdk.VipInventory
import org.zstack.utils.data.SizeUnit

import static org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants.*

/**
 * Created by Qi Le on 2020/11/3
 */
class AllocateEipAddressPro extends BaseAPI {
    final static String RESERVE_IP_FAIL_CODE = "ReserveIpFail"
    final static String RESERVE_IP_FAIL_MESSAGE = "Reserve eip failed."
    final static String QOS_FAIL_MESSAGE = 'Specified value of "Bandwidth" is not valid.'

    @Override
    protected void configAPIConversionSpec() {
        spec = config {
            convertAPIParam {
                complexConvert {
                    ecsParamName = ECS_NETWORK_IP_ADDRESS
                    zstackParamName = ZSTACK_VIP_UUID
                    stillConvertParamWhenEcsParamValueIsNull = true
                    String eipId

                    getZstackValue = { Map ecsParamMap, String ecsParamValue ->
                        eipId = ExternalAPIAdapterUtils.randomUUID()
                        CreateVipAction vipAction = new CreateVipAction(
                                sessionId: sessionId,
                                name: "vip-for-eip-$eipId".toString(),
                                l3NetworkUuid: ExternalAPIAdapterGlobalProperty.PUBLICL3NETWORKUUID
                        )
                        if (ecsParamValue != null) {
                            vipAction.requiredIp = ecsParamValue
                        }
                        CreateVipAction.Result vipResult = vipAction.call()

                        handleActionResult(vipResult.error, RESERVE_IP_FAIL_CODE, RESERVE_IP_FAIL_MESSAGE)

                        VipInventory vip = vipResult.value.inventory

                        String bandWidth = ecsParamMap[ECS_NETWORK_BANDWIDTH] ?: "5"
                        long bandWidthValue = SizeUnit.MEGABYTE.toByte(Integer.parseInt(bandWidth))
                        SetVipQosAction qosAction = new SetVipQosAction(
                                sessionId: sessionId,
                                uuid: vip.uuid,
                                inboundBandwidth: bandWidthValue,
                                outboundBandwidth: bandWidthValue
                        )
                        SetVipQosAction.Result qosResult = qosAction.call()
                        handleActionResult(qosResult.error,
                                ExternalAPIAdapterConstants.ECSErrorCode.InvalidParameter, QOS_FAIL_MESSAGE)

                        return vip
                    }

                    putZstackParamValue = { Map zstackParamMap, VipInventory vip ->
                        zstackParamMap[zstackParamName] = vip.uuid
                        zstackParamMap[ZSTACK_NAME] = "eip-$vip.ip".toString()
                        zstackParamMap[ZSTACK_RESOURCEUUID_KEY] = eipId
                    }
                }
            }

            convertAPIResponse {
                convertResponseAttribute {
                    ecsAttributeName = ECS_EIP_ADDRESS

                    getZstackAttributeValue = {
                        return zstackAPIRsp.value.inventory.vipIp
                    }

                    addEcsValueToEcsAPIRsp = { Map ecsAPIRsp ->
                        ecsAPIRsp[ecsAttributeName] = zstackAttributeValue
                    }
                }

                convertResponseAttribute {
                    ecsAttributeName = ECS_EIP_ALLOCATION_ID

                    getZstackAttributeValue = {
                        return zstackAPIRsp.value.inventory.uuid
                    }

                    addEcsValueToEcsAPIRsp = { Map ecsAPIRsp ->
                        ecsAPIRsp[ecsAttributeName] = zstackAttributeValue
                    }
                }
            }
        }
    }

    @Override
    Class getZStackAction() {
        return CreateEipAction.class
    }

    @Override
    Exception handleZStackActionFailed(Exception e) {
        return new APIAdapterSpecifiedErrorException(RESERVE_IP_FAIL_CODE, RESERVE_IP_FAIL_MESSAGE)
    }
}
