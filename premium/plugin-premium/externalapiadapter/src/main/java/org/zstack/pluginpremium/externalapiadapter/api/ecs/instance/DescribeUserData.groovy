package org.zstack.pluginpremium.externalapiadapter.api.ecs.instance

import org.zstack.pluginpremium.externalapiadapter.api.BaseAPI
import org.zstack.sdk.QuerySystemTagAction

import static org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants.*
/**
 * @Author: fubang
 * @Date: 2018/5/18
 */
class DescribeUserData extends BaseAPI {
    @Override
    void configAPIConversionSpec() {
        spec = config {
            convertAPIParam {
                beforeZstackAPIParam = { zstackParamMap ->
                    zstackParamMap.put(ZSTACK_QUERY_CONDITIONS_KEY, [])
                    zstackParamMap.put(ZSTACK_QUERY_REPLYWITHCOUNT_KEY, true)
                }

                querySimpleConvert {
                    ecsParamName = ECS_INSTANCE_ID
                    zstackParamName = ZSTACK_RESOURCEUUID_KEY
                }

            }
            convertAPIResponse {
                convertResponseAttribute {
                    ecsAttributeName = ECS_INSTANCE_ID
                    addEcsValueToEcsAPIRsp = { ecsAPIRsp ->
                        ecsAPIRsp.put(ecsAttributeName, ecsAPIParamMap.get(ecsAttributeName))
                    }
                }

                convertResponseAttribute {
                    ecsAttributeName = ECS_API_REGIONID_KEY
                    getZstackAttributeValue = {
                        return ecsAPIParamMap.get(ecsAttributeName)
                    }

                    addEcsValueToEcsAPIRsp = { ecsAPIRsp ->
                        ecsAPIRsp.put(ecsAttributeName, zstackAttributeValue)
                    }
                }

                convertResponseAttribute {
                    ecsAttributeName = ECS_USERDATA
                    getZstackAttributeValue = {
                        def list = zstackAPIRsp.value.inventories as List
                        for (a in list) {
                            def tmp = a.tag as String
                            if (tmp.indexOf("userdata::") >= 0) {
                                return tmp.substring(tmp.lastIndexOf(":") + 1)
                            }
                        }

                        return null
                    }

                    addEcsValueToEcsAPIRsp = { ecsAPIRsp ->
                        ecsAPIRsp.put(ecsAttributeName, zstackAttributeValue)
                    }
                }
            }
        }
    }

    @Override
    Class getZStackAction() {
        return QuerySystemTagAction.class
    }
}
