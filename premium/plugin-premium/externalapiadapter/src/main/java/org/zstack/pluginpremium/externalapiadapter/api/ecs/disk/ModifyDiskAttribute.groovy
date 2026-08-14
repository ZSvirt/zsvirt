package org.zstack.pluginpremium.externalapiadapter.api.ecs.disk

import org.zstack.header.volume.VolumeVO
import org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterUtils
import org.zstack.pluginpremium.externalapiadapter.api.BaseAPI
import org.zstack.sdk.*

import static org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants.*

/**
 * @Author: fubang
 * @Date: 2018/4/24
 */
class ModifyDiskAttribute extends BaseAPI {
    @Override
    void configAPIConversionSpec() {
        spec = config {
            convertAPIParam {

                simpleConvert {
                    ecsParamName = ECS_DISK_ID
                    zstackParamName = ZSTACK_UUID
                }

                simpleConvert {
                    ecsParamName = ECS_DISK_NAME
                    zstackParamName = ZSTACK_NAME
                }

                simpleConvert {
                    ecsParamName = ECS_API_DESCRIPTION_KEY
                    zstackParamName = ZSTACK_API_DESCRIPTION_KEY
                }

            }

            convertAPIResponse {}
        }
    }

    private QuerySystemTagAction.Result deleteWithInstanceTagExist(VolumeInventory volumeInventory) {
        QuerySystemTagAction query = new QuerySystemTagAction(
                sessionId: sessionId,
                conditions: ["resourceUuid=${volumeInventory.uuid}".toString(),
                             "resourceType=${VolumeVO.class.getSimpleName()}".toString(),
                             "tag=${ECS_DISK_DELETEWITHINSTANCE_TAG}".toString()]
        )
        QuerySystemTagAction.Result result = query.call()
        return result
    }

    private void addDeleteWithInstanceTag(VolumeInventory volumeInventory) {

        if (deleteWithInstanceTagExist(volumeInventory).value.inventories.size() > 0) {
            return
        }

        CreateSystemTagAction action = new CreateSystemTagAction(
                sessionId: sessionId,
                resourceType: VolumeVO.class.getSimpleName(),
                resourceUuid: volumeInventory.uuid,
                tag: ECS_DISK_DELETEWITHINSTANCE_TAG
        )
        CreateSystemTagAction.Result result = action.call()
        result.throwExceptionIfError()
    }

    private void removeDeleteWithInstanceTag(VolumeInventory volumeInventory) {
        QuerySystemTagAction.Result result = deleteWithInstanceTagExist(volumeInventory)
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
        return UpdateVolumeAction.class
    }

    @Override
    void afterCallZStackAction(Object zstackActionResult) {
        super.afterCallZStackAction(zstackActionResult)
        UpdateVolumeAction.Result result = zstackActionResult
        VolumeInventory volumeInventory = result.value.inventory

        String paramName = ECS_DISK_DELETEWITHINSTANCE
        if (ExternalAPIAdapterUtils.checkParam(paramName, ecsAPIParamMap)) {
            if (ecsAPIParamMap.get(paramName) == "true") {
                addDeleteWithInstanceTag(volumeInventory)
            } else {
                removeDeleteWithInstanceTag(volumeInventory)
            }
        }
    }
}
