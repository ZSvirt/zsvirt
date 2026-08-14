package org.zstack.pluginpremium.externalapiadapter.api.ecs.vpc

import org.zstack.pluginpremium.externalapiadapter.api.BaseAPI
import org.zstack.pluginpremium.externalapiadapter.exception.APIParamConvertException
import org.zstack.sdk.DeleteVRouterRouteEntryAction
import org.zstack.sdk.QueryVRouterRouteEntryAction

import static org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants.*

/**
 * Created by Qi Le on 2019-05-29
 */
class DeleteRouteEntry extends BaseAPI {

    @Override
    Class getZStackAction() {
        return DeleteVRouterRouteEntryAction.class
    }

    @Override
    protected void configAPIConversionSpec() {
        spec = config {
            convertAPIParam {
                simpleConvert {
                    ecsParamName = ECS_ROUTE_TABLE_ID
                    zstackParamName = ZSTACK_ROUTE_TABLE_ID
                }

                complexConvert {
                    ecsParamName = "DestinationCidrBlock"
                    zstackParamName = ZSTACK_UUID

                    getZstackValue = { ecsParamMap, String ecsParamValue ->
                        QueryVRouterRouteEntryAction query = new QueryVRouterRouteEntryAction(
                                sessionId: sessionId,
                                conditions: ["routeTableUuid=${ecsParamMap.get(ECS_ROUTE_TABLE_ID)}".toString(),
                                             "destination=${ecsParamValue}".toString()]
                        )
                        QueryVRouterRouteEntryAction.Result result = query.call()
                        result.throwExceptionIfError()
                        if (result.value.inventories.size() > 0) {
                            return result.value.inventories.get(0).uuid
                        }
                        throw new APIParamConvertException(ecsParamName, "Cannot find destination cidr block ${ecsParamValue}".toString())
                    }
                }
            }

            convertAPIResponse {}
        }
    }

}
