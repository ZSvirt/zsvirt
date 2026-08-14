package org.zstack.pluginpremium.externalapiadapter.api.ecs.snapshot

import org.zstack.pluginpremium.externalapiadapter.api.BaseAPI
import org.zstack.sdk.CreateVolumeSnapshotAction

import static org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants.*

/**
 * @Author: fubang
 * @Date: 2018/4/25
 */
class CreateSnapshot extends BaseAPI {
    @Override
    void configAPIConversionSpec() {
        spec = config {
            convertAPIParam {
                simpleConvert {
                    ecsParamName = ECS_DISK_ID
                    zstackParamName = ZSTACK_DISK_UUID
                }
                simpleConvert {
                    ecsParamName = "SnapshotName"
                    zstackParamName = ZSTACK_NAME
                }
                simpleConvert {
                    ecsParamName = ECS_API_DESCRIPTION_KEY
                    zstackParamName = ZSTACK_API_DESCRIPTION_KEY
                }

            }

            convertAPIResponse {
                convertResponseAttribute {
                    ecsAttributeName = ECS_SNAPSHOT_ID
                    getZstackAttributeValue = {
                        return zstackAPIRsp.value.inventory.uuid
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
        return CreateVolumeSnapshotAction.class
    }
}
