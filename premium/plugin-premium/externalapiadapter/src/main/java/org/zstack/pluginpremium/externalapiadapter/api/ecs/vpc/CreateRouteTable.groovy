package org.zstack.pluginpremium.externalapiadapter.api.ecs.vpc

import org.zstack.pluginpremium.externalapiadapter.api.BaseAPI
import org.zstack.pluginpremium.externalapiadapter.exception.APIParamConvertException
import org.zstack.sdk.*

import static org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants.*

/**
 * Created by Qi Le on 2019-05-29
 */
class CreateRouteTable extends BaseAPI {
    @Override
    Class getZStackAction() {
        return CreateVRouterRouteTableAction.class
    }

    @Override
    protected void configAPIConversionSpec() {
        spec = config {
            convertAPIParam {
                zstackNeedParam {
                    zstackParamName = ZSTACK_NAME
                    getZstackValue = {Map ecsParamMap, Map zstackParamMap ->
                        String vpcId = ecsParamMap.get(ECS_VPC_VPC_ID)
                        return ecsParamMap.containsKey("RouteTableName") ? ecsParamMap.get("RouteTableName") : "RouteTableFor-" + vpcId
                    }
                }
            }

            convertAPIResponse {
                convertResponseAttribute {
                    ecsAttributeName = ECS_ROUTE_TABLE_ID
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

    void attachRouteTableToVPCRouter(VRouterRouteTableInventory routeTableInventory,
                                     AliyunProxyVpcInventory vpcInventory) {
        AttachVRouterRouteTableToVRouterAction action = new AttachVRouterRouteTableToVRouterAction(
                sessionId: sessionId,
                virtualRouterVmUuid: vpcInventory.VRouterUuid,
                routeTableUuid: routeTableInventory.uuid
        )
        AttachVRouterRouteTableToVRouterAction.Result result = action.call()
        result.throwExceptionIfError()
    }

    @Override
    void afterCallZStackAction(Object zstackActionResult) {
        super.afterCallZStackAction(zstackActionResult)

        CreateVRouterRouteTableAction.Result result = zstackActionResult
        VRouterRouteTableInventory routeTableInventory = result.value.inventory
        QueryAliyunProxyVpcAction query = new QueryAliyunProxyVpcAction(
                sessionId: sessionId,
                conditions: ["uuid=${ecsAPIParamMap.get(ECS_VPC_VPC_ID)}".toString()]
        )
        QueryAliyunProxyVpcAction.Result qResult = query.call()
        qResult.throwExceptionIfError()
        if (qResult.value.inventories.size() < 1) {
            DeleteVRouterRouteTableAction rollback = new DeleteVRouterRouteTableAction(
                    sessionId: sessionId,
                    uuid: routeTableInventory.uuid
            )
            rollback.call()
            throw new APIParamConvertException(ECS_VPC_VPC_ID, "VRouter of this VPC not found.")
        }
        attachRouteTableToVPCRouter(routeTableInventory, qResult.value.inventories.first())
    }
}
