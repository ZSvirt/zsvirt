package org.zstack.pluginpremium.externalapiadapter.api.ecs.snapshot

import org.zstack.pluginpremium.externalapiadapter.api.BaseAPI
import org.zstack.pluginpremium.externalapiadapter.api.BaseAsyncAPI
import org.zstack.sdk.DeleteVolumeSnapshotAction

import static org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants.*


/**
 * @Author: fubang
 * @Date: 2018/4/25
 */
class DeleteSnapshot extends BaseAsyncAPI<DeleteVolumeSnapshotAction.Result> {
    @Override
    void configAPIConversionSpec() {
        spec = config {
            convertAPIParam {
                simpleConvert {
                    ecsParamName = ECS_SNAPSHOT_ID
                    zstackParamName = ZSTACK_UUID
                }
            }
            convertAPIResponse {

            }
        }
    }

    @Override
    Class getZStackAction() {
        return DeleteVolumeSnapshotAction.class
    }
}
