package org.zstack.pluginpremium.externalapiadapter.api.ecs.disk

import org.zstack.pluginpremium.externalapiadapter.EcsSystemTags
import org.zstack.pluginpremium.externalapiadapter.api.BaseAPI
import org.zstack.sdk.DeleteTagAction
import org.zstack.sdk.DetachDataVolumeFromVmAction
import org.zstack.sdk.QuerySystemTagAction
import org.zstack.sdk.VolumeInventory

import static org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants.*

/**
 * @Author: fubang
 * @Date: 2018/4/23
 */
class DetachDisk extends BaseAPI {
    @Override
    void configAPIConversionSpec() {
        spec = config {
            convertAPIParam {

                simpleConvert {
                    ecsParamName = ECS_INSTANCE_ID
                    zstackParamName = ZSTACK_DISK_INSTANCE_ID
                }

                simpleConvert {
                    ecsParamName = ECS_DISK_ID
                    zstackParamName = ZSTACK_UUID
                }
            }

            convertAPIResponse {}
        }
    }

    private void removeDeleteWithInstance(VolumeInventory volumeInventory) {
        QuerySystemTagAction query = new QuerySystemTagAction(
                sessionId: sessionId,
                conditions: ["resourceUuid=${volumeInventory.uuid}".toString(),
                             "tag=${EcsSystemTags.DELETE_WITH_INSTANCE.getTagFormat()}".toString()]
        )

        QuerySystemTagAction.Result result = query.call()
        if (result.value.inventories.size() == 0) {
            return
        }

        DeleteTagAction action = new DeleteTagAction(
                sessionId: sessionId,
                uuid: result.value.inventories.get(0).uuid
        )
        DeleteTagAction.Result dRes = action.call()
        dRes.throwExceptionIfError()
    }

    @Override
    Class getZStackAction() {
        return DetachDataVolumeFromVmAction.class
    }

    @Override
    void afterCallZStackAction(Object zstackActionResult) {
        super.afterCallZStackAction(zstackActionResult)
        DetachDataVolumeFromVmAction.Result result = zstackActionResult
        VolumeInventory volumeInventory = result.value.inventory

        removeDeleteWithInstance(volumeInventory)
    }
}
