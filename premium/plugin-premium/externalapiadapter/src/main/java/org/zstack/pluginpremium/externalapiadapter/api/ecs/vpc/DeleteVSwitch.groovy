package org.zstack.pluginpremium.externalapiadapter.api.ecs.vpc

import org.zstack.pluginpremium.externalapiadapter.api.BaseAsyncAPI
import org.zstack.pluginpremium.externalapiadapter.exception.APIAdapterSpecifiedErrorException
import org.zstack.sdk.*

import static org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants.ECS_VPC_VSWITCH_ID
import static org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants.ZSTACK_UUID
/**
 * Created by lining on 2018/5/30.
 */
class DeleteVSwitch extends BaseAsyncAPI<DeleteL2NetworkAction.Result> {
    private static final String VSWITCH_NOT_FOUND_CODE = "InvalidVSwitchId.NotFound"
    private static final String VSWITCH_NOT_FOUND_MESSAGE = "VSwitch not exist."
    private static final String BAD_VSWITCH_STATUS_CODE = "IncorrectStatus"
    private static final String BAD_VSWITCH_STATUS_MESSAGE = "Vswtich status not stable."
    private static final String UNRELEASED_RESOURCE_CODE = "DependencyViolation"
    private static final String UNRELEASED_RESOURCE_MESSAGE = "Specified object has dependent resources."

    @Override
    Class getZStackAction() {
        return DeleteL2NetworkAction.class
    }

    @Override
    void configAPIConversionSpec() {
        spec = config {
            convertAPIParam {
                complexConvert {
                    ecsParamName = ECS_VPC_VSWITCH_ID
                    zstackParamName = ZSTACK_UUID

                    getZstackValue = { String ecsParamValue ->
                        QueryAliyunProxyVSwitchAction action = new QueryAliyunProxyVSwitchAction(
                                sessionId: sessionId,
                                conditions: ["uuid=$ecsParamValue".toString()],
                                limit: 1
                        )
                        QueryAliyunProxyVSwitchAction.Result result = action.call()
                        if (result.error != null || result.value.inventories.isEmpty()) {
                            throw new APIAdapterSpecifiedErrorException(VSWITCH_NOT_FOUND_CODE, VSWITCH_NOT_FOUND_MESSAGE)
                        }

                        AliyunProxyVSwitchInventory vSwitch = result.value.inventories.first()

                        String vRouterUuid = ""
                        QueryAliyunProxyVpcAction queryVpc = new QueryAliyunProxyVpcAction(
                                sessionId: sessionId,
                                conditions: ["uuid=${vSwitch.aliyunProxyVpcUuid}".toString()]
                        )
                        QueryAliyunProxyVpcAction.Result vpcRes = queryVpc.call()
                        if (vpcRes.error == null && !vpcRes.value.inventories.isEmpty()) {
                            vRouterUuid = (vpcRes.value.inventories.first() as AliyunProxyVpcInventory).vRouterUuid
                        }

                        QueryL3NetworkAction queryL3 = new QueryL3NetworkAction(
                                sessionId: sessionId,
                                conditions: ["uuid=${vSwitch.vpcL3NetworkUuid}".toString()],
                                limit: 1
                        )
                        QueryL3NetworkAction.Result l3Result = queryL3.call()
                        if (l3Result.error != null || l3Result.value.inventories.isEmpty()) {
                            throw new APIAdapterSpecifiedErrorException(BAD_VSWITCH_STATUS_CODE, BAD_VSWITCH_STATUS_MESSAGE)
                        }
                        L3NetworkInventory l3Network = l3Result.value.inventories.first()

                        if (existsUnreleasedResources(l3Network.uuid, vRouterUuid)) {
                            throw new APIAdapterSpecifiedErrorException(UNRELEASED_RESOURCE_CODE, UNRELEASED_RESOURCE_MESSAGE)
                        }

                        return l3Network.l2NetworkUuid
                    }
                }
            }

            convertAPIResponse {
            }
        }
    }

    @Override
    void afterCallZStackAction(Object zstackActionResult) {
        super.afterCallZStackAction(zstackActionResult)
        String proxyVSUuid = ecsAPIParamMap[ECS_VPC_VSWITCH_ID]
        DeleteAliyunProxyVSwitchAction deleteProxyVSAct = new DeleteAliyunProxyVSwitchAction(
                sessionId: sessionId,
                uuid: proxyVSUuid
        )
        deleteProxyVSAct.call()
    }

    boolean existsUnreleasedResources(String l3Uuid, String vrUuid) {
        if (l3Uuid == null || vrUuid == null) {
            return false
        }
        QueryVmNicAction queryNic = new QueryVmNicAction(
                sessionId: sessionId,
                conditions: [
                        "l3NetworkUuid=$l3Uuid".toString(),
                        "vmInstanceUuid!=$vrUuid".toString(),
                        "ip!=null"
                ]
        )
        QueryVmNicAction.Result nicRes = queryNic.call()
        boolean existNic = nicRes.error == null && !nicRes.value.inventories.isEmpty()

        QueryVipAction queryVip = new QueryVipAction(
                sessionId: sessionId,
                conditions: ["l3NetworkUuid=$l3Uuid".toString()]
        )
        QueryVipAction.Result vipRes = queryVip.call()
        boolean existVip = vipRes.error == null && !vipRes.value.inventories.isEmpty()

        return existNic || existVip
    }
}
