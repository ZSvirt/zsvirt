package org.zstack.pluginpremium.externalapiadapter.api.ecs.disk

import org.zstack.pluginpremium.externalapiadapter.api.BaseAPI
import org.zstack.pluginpremium.externalapiadapter.exception.APIParamConvertException
import org.zstack.sdk.QueryVolumeAction
import org.zstack.sdk.ReimageVmInstanceAction
import org.zstack.sdk.VolumeInventory

import static org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants.*

/**
 * @Author: fubang
 * @Date: 2018/4/25
 */
class ReInitDisk extends BaseAPI {
    @Override
    void configAPIConversionSpec() {
        spec = config {
            convertAPIParam {
                complexConvert {
                    ecsParamName = ECS_DISK_ID
                    zstackParamName = ZSTACK_VM_INSTANCE_UUID
                    getZstackValue = { ecsParamValue ->
                        QueryVolumeAction action = new QueryVolumeAction(
                                sessionId: sessionId,
                                conditions: [
                                        "$ZSTACK_UUID=$ecsParamValue".toString(),
                                        "$ZSTACK_API_TYPE_KEY=Root".toString(),
                                        "$ZSTACK_API_STATE_KEY=Enabled".toString(),
                                        "$ZSTACK_API_STATUS_KEY=Ready".toString()
                                ]
                        )

                        QueryVolumeAction.Result result = action.call()
                        result.throwExceptionIfError()

                        List list = result.value.inventories
                        if (list.size() == 0) {
                            throw new APIParamConvertException(ecsParamName, "ReInitDisk only support reinitialize system disk in current version.")
                        } else if (list.size() != 1) {
                            throw new APIParamConvertException(ecsParamName, "${action.class.simpleName} result should be only one".toString())
                        }

                        VolumeInventory volume = list.first() as VolumeInventory
                        if (volume.vmInstanceUuid == null) {
                            throw new APIParamConvertException(ecsParamName, "The volume[uuid: ${volume.uuid}] is not attached".toString())
                        }

                        return volume.vmInstanceUuid
                    }
                    putZstackParamValue = { zstackParamMap, zstackParamValue ->
                        zstackParamMap.put(zstackParamName, zstackParamValue)
                    }

                }
            }
            convertAPIResponse {}
        }
    }

    @Override
    Class getZStackAction() {
        return ReimageVmInstanceAction.class
    }
}
