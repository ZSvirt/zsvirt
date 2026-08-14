package org.zstack.pluginpremium.externalapiadapter.api.ecs.slb

import org.zstack.pluginpremium.externalapiadapter.api.BaseAPI

/**
 * Created by Qi Le on 2019/10/9
 */
class StartLoadBalancerListener extends BaseAPI{
    @Override
    Class getZStackAction() {
        return null
    }

    @Override
    protected void configAPIConversionSpec() {
        spec = config {
            convertAPIParam {}
            convertAPIResponse {}
        }
    }

    @Override
    Object callZStackAction() {
        return null
    }
}
