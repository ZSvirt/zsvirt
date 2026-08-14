package org.zstack.pluginpremium.externalapiadapter.api.ecs.vpc


import org.zstack.pluginpremium.externalapiadapter.api.BaseAPI
import org.zstack.pluginpremium.externalapiadapter.exception.APIAdapterSpecifiedErrorException
import org.zstack.sdk.*

import java.util.stream.Collectors

import static org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants.*
/**
 * Created by Qi Le on 2019-05-29
 */
class CreateRouteEntry extends BaseAPI {
    private static final String ROUTE_TABLE_NOT_FOUND_CODE = "InvalidRouteTableId.NotFound"
    private static final String ROUTE_TABLE_NOT_FOUND_MESSAGE = "Specified route table does not exist."
    private static final String VROUTER_NOT_FOUND_CODE = "InvalidVRouter.NotFound"
    private static final String VROUTER_NOT_FOUND_MESSAGE = "vRouter not exists."
    private static final String VPC_NOT_FOUND_CODE = "InvalidVPC.NotFound"
    private static final String VPC_NOT_FOUND_MESSAGE = "vpc not exists."
    private static final String NEXT_HOP_NOT_FOUND_CODE = "InvalidNextHopId.NotFound"
    private static final String NEXT_HOP_NOT_FOUND_MESSAGE = "Specified next hop does not exist."

    @Override
    Class getZStackAction() {
        return AddVRouterRouteEntryAction.class
    }

    @Override
    protected void configAPIConversionSpec() {
        spec = config {
            convertAPIParam {
                simpleConvert {
                    ecsParamName = ECS_ROUTE_TABLE_ID
                    zstackParamName = ZSTACK_ROUTE_TABLE_ID
                }

                simpleConvert {
                    ecsParamName = "DestinationCidrBlock"
                    zstackParamName = "destination"

                    putZstackParamValue = { Map zstackParamMap, String cidr ->
                        if (cidr.indexOf('/') < 0) {
                            cidr += "/32"
                        }
                        (zstackParamMap[zstackParamName] = cidr)
                    }
                }

                complexConvert {
                    ecsParamName = "NextHopId"
                    zstackParamName = "target"

                    getZstackValue = { Map ecsParamMap, String ecsParamValue ->
                        String routeTableId = ecsParamMap[ECS_ROUTE_TABLE_ID]
                        QueryVRouterRouteTableAction queryRouteTableAction = new QueryVRouterRouteTableAction(
                                sessionId: sessionId,
                                conditions: ["uuid=${routeTableId}".toString()]
                        )
                        QueryVRouterRouteTableAction.Result queryRouteTableResult = queryRouteTableAction.call()
                        if (queryRouteTableResult.error != null || queryRouteTableResult.value.inventories.isEmpty()) {
                            throw new APIAdapterSpecifiedErrorException(ROUTE_TABLE_NOT_FOUND_CODE, ROUTE_TABLE_NOT_FOUND_MESSAGE)
                        }

                        VRouterRouteTableInventory routeTable = queryRouteTableResult.value.inventories[0]
                        if (routeTable.attachedRouterRefs.isEmpty()) {
                            throw new APIAdapterSpecifiedErrorException(VROUTER_NOT_FOUND_CODE, VROUTER_NOT_FOUND_MESSAGE)
                        }

                        VirtualRouterVRouterRouteTableRefInventory routerRef = routeTable.attachedRouterRefs[0]
                        String vRouterId = routerRef.virtualRouterVmUuid

                        QueryAliyunProxyVpcAction queryVpcAction = new QueryAliyunProxyVpcAction(
                                sessionId: sessionId,
                                conditions: ["vRouterUuid=${vRouterId}".toString()]
                        )
                        QueryAliyunProxyVpcAction.Result queryVpcResult = queryVpcAction.call()
                        if (queryVpcResult.error != null || queryVpcResult.value.inventories.isEmpty()) {
                            throw new APIAdapterSpecifiedErrorException(VPC_NOT_FOUND_CODE, VPC_NOT_FOUND_MESSAGE)
                        }
                        String vpcId = queryVpcResult.value.inventories[0].uuid

                        QueryAliyunProxyVSwitchAction queryVSwitchAction = new QueryAliyunProxyVSwitchAction(
                                sessionId: sessionId,
                                conditions: ["aliyunProxyVpcUuid=${vpcId}".toString()]
                        )
                        QueryAliyunProxyVSwitchAction.Result queryVSwitchResult = queryVSwitchAction.call()
                        if (queryVSwitchResult.error != null || queryVSwitchResult.value.inventories.isEmpty()) {
                            throw new APIAdapterSpecifiedErrorException()
                        }

                        Set l3NetList = queryVSwitchResult.value.inventories.stream()
                                .map({ vs -> vs.vpcL3NetworkUuid }).collect(Collectors.toSet()) as Set

                        String nextHopType = ecsAPIParamMap["NextHopType"]
                        if ("Instance" == nextHopType) {
                            return instanceNextHoop(l3NetList, ecsParamValue)
                        } else {
                            throw new APIAdapterSpecifiedErrorException(ECSErrorCode.ApiUnsupported, "Not a supported next hop type $nextHopType".toString())
                        }
                    }
                }
            }

            convertAPIResponse {
                convertResponseAttribute {}
            }
        }
    }

    String instanceNextHoop(Set networks, String instanceId) {
        if (networks == null || networks.isEmpty()) {
            throw new APIAdapterSpecifiedErrorException(NEXT_HOP_NOT_FOUND_CODE, NEXT_HOP_NOT_FOUND_MESSAGE)
        }
        QueryVmNicAction queryVmNicAction = new QueryVmNicAction(
                sessionId: sessionId,
                conditions: ["vmInstanceUuid=${instanceId}".toString()]
        )
        QueryVmNicAction.Result queryVmNicResult = queryVmNicAction.call()

        if (queryVmNicResult.error != null || queryVmNicResult.value.inventories.isEmpty()) {
            throw new APIAdapterSpecifiedErrorException(NEXT_HOP_NOT_FOUND_CODE, NEXT_HOP_NOT_FOUND_MESSAGE)
        }

        if (queryVmNicResult.value.inventories.size() > 0) {
            for (VmNicInventory nic in queryVmNicResult.value.inventories) {
                if (networks.contains(nic.l3NetworkUuid)) {
                    return nic.ip
                }
            }
        }
        throw new APIAdapterSpecifiedErrorException(NEXT_HOP_NOT_FOUND_CODE, NEXT_HOP_NOT_FOUND_MESSAGE)
    }
}
