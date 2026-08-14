package org.zstack.pluginpremium.externalapiadapter.api.ecs.vpc

import org.zstack.pluginpremium.externalapiadapter.api.BaseAPI

import static org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants.ECS_API_STATUS_KEY
import static org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants.ECS_VPC_VPC_ID

/**
 * @Author: fubang* @Date: 2018/5/18
 */

// mock api
class InnerVpcCloudQuerySimpleVpcInfo extends BaseAPI {
    @Override
    protected void configAPIConversionSpec() {
        spec = config {
            convertAPIParam {}

            convertAPIResponse {
                convertResponseAttribute {
                    ecsAttributeName = ECS_VPC_VPC_ID

                    getZstackAttributeValue = {
                        return ecsAPIParamMap.get(ecsAttributeName)?:"mock VpcId"
                    }

                    addEcsValueToEcsAPIRsp = {ecsAPIRsp ->
                        ecsAPIRsp.put(ecsAttributeName, zstackAttributeValue)
                    }
                }

                convertResponseAttribute {
                    ecsAttributeName = "GwGroupIp"

                    getZstackAttributeValue = {
                        return "mock GwGroupIp"
                    }

                    addEcsValueToEcsAPIRsp = {ecsAPIRsp ->
                        ecsAPIRsp.put(ecsAttributeName, zstackAttributeValue)
                    }
                }

                convertResponseAttribute {
                    ecsAttributeName = "TunnelId"

                    getZstackAttributeValue = {
                        return ecsAPIParamMap.get(ecsAttributeName)?:"mock TunnelId"
                    }

                    addEcsValueToEcsAPIRsp = {ecsAPIRsp ->
                        ecsAPIRsp.put(ecsAttributeName, zstackAttributeValue.toString())
                    }
                }

                convertResponseAttribute {
                    ecsAttributeName = ECS_API_STATUS_KEY

                    getZstackAttributeValue = {
                        return "Available"
                    }

                    addEcsValueToEcsAPIRsp = {ecsAPIRsp ->
                        ecsAPIRsp.put(ecsAttributeName, zstackAttributeValue.toString())
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
