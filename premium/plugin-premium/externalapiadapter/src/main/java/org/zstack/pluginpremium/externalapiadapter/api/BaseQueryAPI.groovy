package org.zstack.pluginpremium.externalapiadapter.api

import org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants

/**
 * Created by lining on 2018/4/21.
 */
abstract class BaseQueryAPI extends BaseAPI{

    @Override
    void setEcsAPIParamDefaultValue(Map ecsAPIParamMap) {
        if(!ecsAPIParamMap.containsKey(ExternalAPIAdapterConstants.ECS_QUERY_API_PAGESIZE_KEY)) {
            ecsAPIParamMap.put(ExternalAPIAdapterConstants.ECS_QUERY_API_PAGESIZE_KEY, Integer.toString(ExternalAPIAdapterConstants.ECS_QUERY_API_PAGESIZE_DEFAULT_VALUE))
        }

        if(!ecsAPIParamMap.containsKey(ExternalAPIAdapterConstants.ECS_QUERY_API_PAGENUMBER_KEY)) {
            ecsAPIParamMap.put(ExternalAPIAdapterConstants.ECS_QUERY_API_PAGENUMBER_KEY, Integer.toString(ExternalAPIAdapterConstants.ECS_QUERY_API_PAGENUMBER_DEFAULT_VALUE))
        }
    }

}
