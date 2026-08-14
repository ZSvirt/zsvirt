package org.zstack.pluginpremium.externalapiadapter.api.ecs.disk

import org.zstack.header.volume.VolumeVO
import org.zstack.pluginpremium.externalapiadapter.EcsSystemTags
import org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterUtils
import org.zstack.pluginpremium.externalapiadapter.api.BaseAPI
import org.zstack.pluginpremium.externalapiadapter.exception.APIAdapterSpecifiedErrorException
import org.zstack.sdk.AttachDataVolumeToVmAction
import org.zstack.sdk.CreateSystemTagAction
import org.zstack.sdk.ErrorCode
import org.zstack.sdk.VolumeInventory

import static org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants.*

/**
 * @Author: fubang
 * @Date: 2018/4/23
 */
class AttachDisk extends BaseAPI {
    final static String STORAGE_NOT_ENOUGH_CODE = "ResourceNotEnough.Storage"
    final static String SOTRAGE_NOT_ENOUGH_MESSAGE = "Storage space not enough for instantiating this disk."

    @Override
    void configAPIConversionSpec() {
        spec = config {
            convertAPIParam {
                simpleConvert {
                    ecsParamName = ECS_INSTANCE_ID
                    zstackParamName = ZSTACK_VM_INSTANCE_UUID
                }

                simpleConvert {
                    ecsParamName = ECS_DISK_ID
                    zstackParamName = ZSTACK_DISK_UUID
                }

                /*
                systemTagConvert {
                    ecsParamName  ="DeleteWithInstance"

                    getTag = { String ecsParamValue ->
                        if (ecsParamValue != null && ecsParamValue == "true") {
                            return EcsSystemTags.DELETE_WITH_INSTANCE.instantiateTag([(EcsSystemTags.DELETE_WITH_INSTANCE_TOKEN): ""])
                        }
                    }
                }

                 */
            }

            convertAPIResponse {}
        }
    }

    private void addDeleteWithInstanceTag(VolumeInventory volumeInventory) {
        CreateSystemTagAction action = new CreateSystemTagAction(
                sessionId: sessionId,
                resourceType: VolumeVO.class.getSimpleName(),
                resourceUuid: volumeInventory.uuid,
                tag: EcsSystemTags.DELETE_WITH_INSTANCE.getTagFormat()
        )
        CreateSystemTagAction.Result result = action.call()
        result.throwExceptionIfError()
    }

    @Override
    Class getZStackAction() {
        return AttachDataVolumeToVmAction.class
    }

    @Override
    void afterCallZStackAction(Object zstackActionResult) {
        super.afterCallZStackAction(zstackActionResult)
        AttachDataVolumeToVmAction.Result result = zstackActionResult
        VolumeInventory volumeInventory = result.value.inventory

        String paramName = ECS_DISK_DELETEWITHINSTANCE
        if (ExternalAPIAdapterUtils.checkParam(paramName, ecsAPIParamMap)) {
            addDeleteWithInstanceTag(volumeInventory)
        }
    }

    @Override
    void handleActionResult(ErrorCode e, String code, String message) {
        if (e != null) {
            throw new APIAdapterSpecifiedErrorException(STORAGE_NOT_ENOUGH_CODE, SOTRAGE_NOT_ENOUGH_MESSAGE)
        }
    }
}
