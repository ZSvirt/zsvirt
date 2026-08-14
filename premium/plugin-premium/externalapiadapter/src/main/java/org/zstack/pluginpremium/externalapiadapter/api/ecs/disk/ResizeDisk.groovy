package org.zstack.pluginpremium.externalapiadapter.api.ecs.disk

import org.zstack.pluginpremium.externalapiadapter.api.BaseAPI
import org.zstack.pluginpremium.externalapiadapter.exception.APIAdapterSpecifiedErrorException
import org.zstack.pluginpremium.externalapiadapter.exception.APIParamConvertException
import org.zstack.pluginpremium.externalapiadapter.typeconvertor.DiskType
import org.zstack.sdk.ErrorCode
import org.zstack.sdk.QueryVolumeAction
import org.zstack.sdk.ResizeDataVolumeAction
import org.zstack.sdk.ResizeRootVolumeAction
import org.zstack.sdk.VolumeInventory
import org.zstack.utils.data.SizeUnit

import static org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants.*

/**
 * @Author: fubang
 * @Date: 2018/4/25
 */
class ResizeDisk extends BaseAPI {
    final static String STORAGE_NOT_ENOUGH_CODE = "ResourceNotEnough.Storage"
    final static String SOTRAGE_NOT_ENOUGH_MESSAGE = "Storage space not enough for instantiating this disk."

    @Override
    void configAPIConversionSpec() {
        spec = config {
            convertAPIParam {
                simpleConvert {
                    ecsParamName = ECS_DISK_ID
                    zstackParamName = ZSTACK_UUID
                }

                simpleConvert {
                    ecsParamName = ECS_DISK_NEW_SIZE
                    ecsParamType = Integer.class
                    zstackParamName = ZSTACK_API_SIZE

                    putZstackParamValue = { zstackParamMap, zstackParamValue ->
                        zstackParamMap.put(zstackParamName, SizeUnit.GIGABYTE.toByte(zstackParamValue))
                    }
                }

            }

            convertAPIResponse {
                convertResponseAttribute {}
            }
        }
    }

    @Override
    Class getZStackAction() {
        def diskId = ecsAPIParamMap.get(ECS_DISK_ID)
        QueryVolumeAction action = new QueryVolumeAction()
        action.sessionId = sessionId
        action.conditions = ["uuid=${diskId}".toString()]
        QueryVolumeAction.Result result = action.call()
        result.throwExceptionIfError()

        if (result.value.inventories.isEmpty()) {
            throw new APIParamConvertException(ECS_DISK_ID, "Not found the volume[uuid: $diskId]".toString())
        }

        def inventory = result.value.inventories.get(0) as VolumeInventory

        if (inventory.type == DiskType.SYSTEM.zstackValue) {
            return ResizeRootVolumeAction.class
        } else if (inventory.type == DiskType.DATA.zstackValue) {
            return ResizeDataVolumeAction.class
        }

        throw new APIParamConvertException(ECS_DISK_ID, "${ECS_DISK_ID}[value: $diskId] is not valid".toString())
    }

    @Override
    void handleActionResult(ErrorCode e, String code, String message) {
        if (e != null) {
            throw new APIAdapterSpecifiedErrorException(STORAGE_NOT_ENOUGH_CODE, SOTRAGE_NOT_ENOUGH_MESSAGE)
        }
    }
}
