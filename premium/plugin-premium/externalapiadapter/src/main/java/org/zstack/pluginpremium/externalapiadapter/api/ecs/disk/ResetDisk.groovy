package org.zstack.pluginpremium.externalapiadapter.api.ecs.disk

import org.zstack.header.vm.VmInstanceState
import org.zstack.header.volume.VolumeStatus
import org.zstack.pluginpremium.externalapiadapter.api.BaseAPI
import org.zstack.pluginpremium.externalapiadapter.exception.APIAdapterSpecifiedErrorException
import org.zstack.sdk.*

import static org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants.*

/**
 * @Author: fubang* @Date: 2018/4/25
 */
class ResetDisk extends BaseAPI {
    private static final String DISK_NOT_FOUND_CODE = "Disk.NotFound"
    private static final String DISK_NOT_FOUND_MESSAGE = "The specified disk does not exist."
    private static final String BAD_INSTANCE_STATUS_CODE = "IncorrectInstanceStatus"
    private static final String BAD_INSTANCE_STATUS_MESSAGE = "The current status of the resource does not support this operation."
    private static final String BAD_DISK_STATUS_CODE = "IncorrectDiskStatus"
    private static final String BAD_DISK_STATUS_MESSAGE = "The current disk status does not support this operation."

    @Override
    void configAPIConversionSpec() {
        spec = config {
            convertAPIParam {
                simpleConvert {
                    ecsParamName = ECS_SNAPSHOT_ID
                    zstackParamName = ZSTACK_UUID
                }

                simpleConvert {
                    ecsParamName = ECS_DISK_ID

                    putZstackParamValue = { Map zstackParamMap, String ecsParamValue ->
                        QueryVolumeAction qVolume = new QueryVolumeAction(
                                sessionId: sessionId,
                                conditions: ["uuid=$ecsParamValue".toString()]
                        )
                        QueryVolumeAction.Result volumeRes = qVolume.call()
                        if (volumeRes.error != null || volumeRes.value.inventories.isEmpty()) {
                            throw new APIAdapterSpecifiedErrorException(DISK_NOT_FOUND_CODE, DISK_NOT_FOUND_MESSAGE)
                        }
                        VolumeInventory volume = volumeRes.value.inventories.first()
                        if (volume.status == VolumeStatus.NotInstantiated.toString()) {
                            throw new APIAdapterSpecifiedErrorException(BAD_DISK_STATUS_CODE, BAD_DISK_STATUS_MESSAGE)
                        }
                        String vmUuid = volume.vmInstanceUuid
                        if (vmUuid == null) {
                            return
                        }
                        QueryVmInstanceAction queryVm = new QueryVmInstanceAction(
                                sessionId: sessionId,
                                conditions: ["uuid=$vmUuid".toString()]
                        )
                        QueryVmInstanceAction.Result vmRes = queryVm.call()
                        if (!(vmRes.error != null || vmRes.value.inventories.isEmpty())) {
                            VmInstanceInventory vm = vmRes.value.inventories.first()
                            if (vm.state == VmInstanceState.Stopped.toString()) {
                                return
                            }
                        }

                        throw new APIAdapterSpecifiedErrorException(BAD_INSTANCE_STATUS_CODE, BAD_INSTANCE_STATUS_MESSAGE)
                    }
                }
            }

            convertAPIResponse {}
        }
    }

    @Override
    Class getZStackAction() {
        return RevertVolumeFromSnapshotAction.class
    }
}
