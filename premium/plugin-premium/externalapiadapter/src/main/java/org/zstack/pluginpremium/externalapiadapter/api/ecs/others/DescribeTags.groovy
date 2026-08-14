package org.zstack.pluginpremium.externalapiadapter.api.ecs.others

import org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants
import org.zstack.pluginpremium.externalapiadapter.api.BaseAPI
import org.zstack.utils.gson.JSONObjectUtil

import static org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants.*

/**
 * Created by Qi Le on 2019/10/9
 * mock API
 */
class DescribeTags extends BaseAPI {
    @Override
    Class getZStackAction() {
        return null
    }

    @Override
    protected void configAPIConversionSpec() {
        spec = config {
            convertAPIParam {
                beforeZstackAPIParam = { Map zstackParamMap ->
                    zstackParamMap.put(ZSTACK_QUERY_CONDITIONS_KEY, [])
                    zstackParamMap.put(ZSTACK_QUERY_REPLYWITHCOUNT_KEY, true)
                }

                simpleConvert {
                    ecsParamName = ECS_QUERY_API_PAGESIZE_KEY
                    ecsParamType = Integer.class
                    zstackParamName = ECS_QUERY_API_PAGESIZE_KEY
                    zstackParamType = Integer.class
                }

                simpleConvert {
                    ecsParamName = ECS_QUERY_API_PAGENUMBER_KEY
                    ecsParamType = Integer.class
                    zstackParamName = ECS_QUERY_API_PAGENUMBER_KEY
                    zstackParamType = Integer.class
                }
            }

            convertAPIResponse {
                convertResponseAttribute {
                    ecsAttributeName = "TagSets"
                    getZstackAttributeValue = {
                        String pageNumber = ECS_QUERY_API_PAGENUMBER_KEY
                        String pageSize = ECS_QUERY_API_PAGESIZE_KEY
                        List tagList = []
                        Map ret = [
                                (ECS_QUERY_API_PAGENUMBER_KEY): zstackAPIParamMap.getOrDefault(pageNumber, 1),
                                (ECS_QUERY_API_PAGESIZE_KEY)  : zstackAPIParamMap.getOrDefault(pageSize, 50),
                                (ECS_TOTAL_COUNT)             : 0,
                                "TagSet"                      : tagList
                        ]
                        return ret
                    }

                    addEcsValueToEcsAPIRsp = { Map ecsAPIRsp ->
                        ecsAPIRsp.put(ecsAttributeName, zstackAttributeValue)
                    }
                }
            }
        }
    }

    @Override
    String call(Map ecsAPIParamMap) {
        this.ecsAPIParamMap = ecsAPIParamMap

        this.zstackAPIParamMap = getZstackAPIParam(ecsAPIParamMap)
        logger.debug("[requestId:${this.zstackAPIParamMap.get(ExternalAPIAdapterConstants.ZSTACK_API_APIID_KEY)}, API:${this.getClass().simpleName}] Convert into zstack api params: ${this.zstackAPIParamMap}")

        Map ecsAPIRsp = this.getEcsAPIRsp(zstackAPIParamMap)
        String ecsResultRspJson = JSONObjectUtil.toJsonString(ecsAPIRsp)

        return ecsResultRspJson
    }
}
