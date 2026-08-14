package org.zstack.pluginpremium.externalapiadapter.api.ecs.vpc

import org.zstack.pluginpremium.externalapiadapter.api.BaseAPI

/**
 * @Author: fubang
 * @Date: 2018/5/18
 */

// mock api
class InnerVpcCloudQueryVgwVipByEcsId extends BaseAPI {
    @Override
    protected void configAPIConversionSpec() {
        spec = config {
            convertAPIParam {}

            convertAPIResponse {
                convertResponseAttribute {
                    ecsAttributeName = "VgwIp"

                    getZstackAttributeValue = {
                        return "mock VgwIp"
                    }

                    addEcsValueToEcsAPIRsp = {ecsAPIRsp ->
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
