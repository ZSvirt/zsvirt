package org.zstack.pluginpremium.externalapiadapter.api.ecs.disk


import org.zstack.pluginpremium.externalapiadapter.api.BaseAPI
import org.zstack.sdk.DeleteDataVolumeAction
import org.zstack.sdk.DeleteTagAction
import org.zstack.sdk.QuerySystemTagAction
import org.zstack.sdk.SystemTagInventory

import static org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants.*

/**
 * @Author: fubang
 * @Date: 2018/4/23
 */
class DeleteDisk extends BaseAPI {
    @Override
    void configAPIConversionSpec() {
        spec = config {
            convertAPIParam {

                simpleConvert {
                    ecsParamName = ECS_DISK_ID
                    zstackParamName = ZSTACK_UUID
                }
            }

            convertAPIResponse {}
        }
    }

    @Override
    Class getZStackAction() {
        return DeleteDataVolumeAction.class
    }

    //todo if systemTag removed automatically, remove this
    @Override
    void afterCallZStackAction(Object zstackActionResult) {
        super.afterCallZStackAction(zstackActionResult)

        String paramName = ECS_DISK_ID
        deleteVolumeSystemTag(ecsAPIParamMap.get(paramName))
    }

    private void deleteVolumeSystemTag(String diskUuid) {
        if (diskUuid == null) {
            return
        }

        QuerySystemTagAction query = new QuerySystemTagAction(
                sessionId: sessionId,
                conditions: [
                        "resourceType=VolumeVO",
                        "resourceUuid=${diskUuid}".toString()
                ]
        )

        QuerySystemTagAction.Result result = query.call()
        result.throwExceptionIfError()

        if (result.value.inventories.size() == 0) {
            return
        }

        for (SystemTagInventory sTag: result.value.inventories) {
            DeleteTagAction delAct = new DeleteTagAction(
                    sessionId: sessionId,
                    uuid: sTag.uuid
            )
            delAct.call()
        }
    }
}
