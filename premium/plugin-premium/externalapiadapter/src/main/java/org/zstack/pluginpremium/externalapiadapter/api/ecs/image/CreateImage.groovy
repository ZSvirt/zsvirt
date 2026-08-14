package org.zstack.pluginpremium.externalapiadapter.api.ecs.image

import org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterGlobalProperty
import org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterUtils
import org.zstack.pluginpremium.externalapiadapter.api.BaseAsyncAPI
import org.zstack.pluginpremium.externalapiadapter.exception.APIParamConvertException
import org.zstack.sdk.CreateRootVolumeTemplateFromRootVolumeAction
import org.zstack.sdk.CreateRootVolumeTemplateFromVolumeSnapshotAction
import org.zstack.sdk.QueryVolumeAction

import static org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants.*

/**
 * @Author: fubang
 * @Date: 2018/4/25
 */
class CreateImage extends BaseAsyncAPI {

    @Override
    Class getZStackAction() {
        if (ecsAPIParamMap.get(ECS_INSTANCE_ID) != null) {
            return CreateRootVolumeTemplateFromRootVolumeAction.class
        } else if (ecsAPIParamMap.get(ECS_SNAPSHOT_ID) != null) {
            return CreateRootVolumeTemplateFromVolumeSnapshotAction.class
        }
    }

    @Override
    void configAPIConversionSpec() {
        spec = config {
            convertAPIParam {
                complexConvert {
                    ecsParamName = ECS_INSTANCE_ID
                    zstackParamName = "rootVolumeUuid"
                    getZstackValue = { ecsParamValue ->
                        QueryVolumeAction action = new QueryVolumeAction()
                        action.conditions = ["vmInstanceUuid=$ecsParamValue".toString(), "type=Root"]
                        action.sessionId = sessionId
                        action.apiId = requestId

                        QueryVolumeAction.Result result = action.call()
                        result.throwExceptionIfError()

                        List inventories = result.value.inventories
                        if (inventories.isEmpty()) {
                            throw new APIParamConvertException(ecsParamName, "The vm do not exist")
                        }

                        return inventories.get(0).uuid
                    }

                    putZstackParamValue = { zstackParamMap, zstackParamValue ->
                        zstackParamMap.put(zstackParamName, zstackParamValue)
                    }
                }

                complexConvert {
                    ecsParamName = ECS_SNAPSHOT_ID
                    zstackParamName = "snapshotUuid"
                    getZstackValue = {
                        return ecsAPIParamMap.get(ecsParamName)
                    }
                    putZstackParamValue = { zstackParamMap, zstackParamValue ->
                        zstackParamMap.put(zstackParamName, zstackParamValue)
                        zstackParamMap.put("backupStorageUuids", [ExternalAPIAdapterGlobalProperty.BACKUPSTORAGE_UUID])
                    }
                }

                simpleConvert {
                    ecsParamName = "ImageName"
                    zstackParamName = ZSTACK_NAME
                }

                simpleConvert {
                    ecsParamName = ECS_API_DESCRIPTION_KEY
                    zstackParamName = ZSTACK_API_DESCRIPTION_KEY
                }

                zstackNeedParam {
                    zstackParamName = ZSTACK_RESOURCEUUID_KEY
                    getZstackValue = { Map ecsParamMap, Map zstackParamMap ->
                        String clientToken = ecsParamMap.get(ECS_API_CLIENTTOKEN_KEY)
                        return ExternalAPIAdapterUtils.randomUUID(clientToken)
                    }
                }

            }

            convertAPIResponse {

                convertResponseAttribute {
                    ecsAttributeName = ECS_IMAGE_ID
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
}
