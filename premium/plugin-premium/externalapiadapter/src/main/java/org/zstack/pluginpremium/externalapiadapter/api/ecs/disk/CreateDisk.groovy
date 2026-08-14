package org.zstack.pluginpremium.externalapiadapter.api.ecs.disk

import org.zstack.kvm.KVMSystemTags
import org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterUtils
import org.zstack.pluginpremium.externalapiadapter.api.BaseAsyncAPI
import org.zstack.pluginpremium.externalapiadapter.convert.param.ParameterConversionUtils
import org.zstack.sdk.CreateDataVolumeAction
import org.zstack.sdk.CreateDataVolumeFromVolumeSnapshotAction

import static org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants.*

/**
 * @Author: fubang
 * @Date: 2018/4/23
 */
class CreateDisk extends BaseAsyncAPI {

    @Override
    void configAPIConversionSpec() {
        spec = config {
            convertAPIParam {
                simpleConvert {
                    ecsParamName = ECS_DISK_NAME
                    zstackParamName = ZSTACK_NAME
                    stillConvertParamWhenEcsParamValueIsNull = true
                    putZstackParamValue = { zstackParamMap, zstackParamValue ->
                        if (zstackParamValue == null) {
                            zstackParamValue = "untitledDisk"
                        }
                        zstackParamMap.put(zstackParamName, zstackParamValue)
                    }
                }

                simpleConvert {
                    ecsParamName = ECS_API_DESCRIPTION_KEY
                    zstackParamName = ZSTACK_API_DESCRIPTION_KEY
                }

                complexConvert {
                    ecsParamName = ECS_API_SIZE
                    ecsParamType = Integer.class
                    zstackParamName = ZSTACK_DISK_OFFERING_UUID
                    getZstackValue = { ecsParamValue ->
                        return Integer.parseInt(ecsParamValue as String)
                    }
                    putZstackParamValue = { zstackParamMap, zstackParamValue ->
                        def value = null
                        if (ecsAPIParamMap.get(ECS_SNAPSHOT_ID) == null) {
                            value = ParameterConversionUtils.getDiskOffering(sessionId, requestId, ecsParamName, zstackParamValue as long)
                        }
                        zstackParamMap.put(zstackParamName, value)
                    }

                }

                simpleConvert {
                    ecsParamName = ECS_SNAPSHOT_ID
                    zstackParamName = ZSTACK_DISK_SNAPSHOT_UUID
                }

                zstackNeedParam {
                    zstackParamName = ZSTACK_RESOURCEUUID_KEY
                    getZstackValue = { Map ecsParamMap, Map zstackParamMap ->
                        String clientToken = ecsParamMap.get(ECS_API_CLIENTTOKEN_KEY)
                        return ExternalAPIAdapterUtils.randomUUID(clientToken)
                    }
                }

                systemTagConvert {
                    ecsParamName = ECS_DISK_VIRTIO_SCSI
                    stillConvertParamWhenEcsParamValueIsNull = true

                    getTag = { String ecsParamValue ->
                        return KVMSystemTags.VOLUME_VIRTIO_SCSI.getTagFormat()
                    }
                }

            }
            convertAPIResponse {
                convertResponseAttribute {
                    ecsAttributeName = ECS_DISK_ID

                    getZstackAttributeValue = {
                        return zstackAPIReq.get(ZSTACK_RESOURCEUUID_KEY)
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
        if (ecsAPIParamMap.get(ECS_SNAPSHOT_ID) == null) {
            return CreateDataVolumeAction.class
        }

        return CreateDataVolumeFromVolumeSnapshotAction.class
    }
}
