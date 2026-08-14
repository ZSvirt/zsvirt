package org.zstack.pluginpremium.externalapiadapter.api.ecs.others

import static org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants.*
import org.zstack.pluginpremium.externalapiadapter.api.BaseAPI

/**
 * @Author: fubang
 * @Date: 2018/4/30
 */
// mock api
class DescribeInstanceTypeFamilies extends BaseAPI {

    @Override
    protected void configAPIConversionSpec() {
        spec = config {
            convertAPIParam {

            }
            convertAPIResponse {
                convertResponseAttribute {
                    ecsAttributeName = "InstanceTypeFamilies"
                    ecsAttributeValue = new HashMap<>()
                    addEcsValueToEcsAPIRsp = { ecsAPIRsp ->
                        ecsAttributeValue.put("InstanceTypeFamily", zstackAPIRsp)
                        ecsAPIRsp.put(ecsAttributeName, ecsAttributeValue)
                    }
                }
            }
        }
    }

    @Override
    Object callZStackAction() {
        return [
                [
                        "InstanceTypeFamilyId": SUPPORT_RESOURCE_INFO.INSTANCE_TYPE_FAMILY,
                        "Generation": SUPPORT_RESOURCE_INFO.INSTANCE_TYPE_GENERATION
                ]
        ]
    }

    @Override
    Class getZStackAction() {
        return null
    }
}
