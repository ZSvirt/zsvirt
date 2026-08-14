package org.zstack.pluginpremium.externalapiadapter.api.ecs.vpc


import org.zstack.pluginpremium.externalapiadapter.api.BaseAPI

import static org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants.*

/**
 * Created by Qi Le on 2019/11/12
 */
class DescribeIpv6Gateways extends BaseAPI{
    //mock api
    @Override
    protected void configAPIConversionSpec() {
        spec = config {
            convertAPIParam {}

            convertAPIResponse {
                convertResponseAttribute {
                    ecsAttributeName = "Ipv6Gateways"
                    ecsAttributeValue = new HashMap<>()

                    addEcsValueToEcsAPIRsp = { Map ecsAPIRsp ->
                        ecsAPIRsp.put(ecsAttributeName, ecsAttributeValue)
                        ecsAttributeValue.put("Ipv6Gateway", new ArrayList<>())
                    }
                }

                convertResponseAttribute {
                    ecsAttributeName = ECS_QUERY_API_PAGENUMBER_KEY

                    addEcsValueToEcsAPIRsp = { Map ecsAPIRsp ->
                        def pageNumber = ecsAPIParamMap.get(ecsAttributeName)
                        if (pageNumber == null) {
                            pageNumber = 1
                        }
                        ecsAPIRsp.put(ecsAttributeName, pageNumber)
                    }
                }

                convertResponseAttribute {
                    ecsAttributeName = ECS_QUERY_API_PAGESIZE_KEY

                    addEcsValueToEcsAPIRsp = { Map ecsAPIRsp ->
                        def pageSize = ecsAPIParamMap.get(ecsAttributeName)
                        if (pageSize == null) {
                            pageSize = 10
                        }
                        ecsAPIRsp.put(ecsAttributeName, pageSize)
                    }
                }

                convertResponseAttribute {
                    ecsAttributeName = ECS_TOTAL_COUNT
                    zstackAttributeValue = 0

                    addEcsValueToEcsAPIRsp = { Map ecsAPIRsp ->
                        ecsAPIRsp.put(ecsAttributeName, zstackAttributeValue)
                    }
                }
            }
        }
    }

    @Override
    Class getZStackAction() {
        return null
    }

    @Override
    Object callZStackAction() {
        return null
    }
}
