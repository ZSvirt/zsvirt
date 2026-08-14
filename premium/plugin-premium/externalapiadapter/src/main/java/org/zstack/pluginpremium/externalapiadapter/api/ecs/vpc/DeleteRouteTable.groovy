package org.zstack.pluginpremium.externalapiadapter.api.ecs.vpc

import org.zstack.pluginpremium.externalapiadapter.api.BaseAPI
import org.zstack.sdk.DeleteVRouterRouteTableAction

import static org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants.ECS_ROUTE_TABLE_ID
import static org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants.ZSTACK_UUID

/**
 * Created by Qi Le on 2019-05-29
 */
class DeleteRouteTable extends BaseAPI {

    @Override
    Class getZStackAction() {
        return DeleteVRouterRouteTableAction.class
    }

    @Override
    protected void configAPIConversionSpec() {
        spec = config {
            convertAPIParam {
                simpleConvert {
                    ecsParamName = ECS_ROUTE_TABLE_ID
                    zstackParamName = ZSTACK_UUID
                }
            }

            convertAPIResponse {}
        }
    }
}
