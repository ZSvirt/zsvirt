package org.zstack.pluginpremium.externalapiadapter.api.ecs.instance

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.zstack.pluginpremium.externalapiadapter.EcsSystemTags
import org.zstack.pluginpremium.externalapiadapter.api.BaseAPI
import org.zstack.sdk.*
import org.zstack.utils.gson.JSONObjectUtil

import java.util.stream.Collectors

import static org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants.ECS_INSTANCE_ID
import static org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants.ZSTACK_UUID


/**
 * @Author: fubang
 * @Date: 2018/4/23
 */
class DeleteInstance extends BaseAPI{

    @Override
    Class getZStackAction() {
        return DestroyVmInstanceAction.class
    }

    @Override
    void configAPIConversionSpec() {
        spec = config {
            convertAPIParam {
                simpleConvert {
                    ecsParamName = ECS_INSTANCE_ID
                    zstackParamName = ZSTACK_UUID
                }
            }

            convertAPIResponse {
            }
        }
    }

    @Override
    Object callZStackAction() {
        Gson gson = new GsonBuilder().create()
        DestroyVmInstanceAction action = gson.fromJson(JSONObjectUtil.toJsonString(zstackAPIParamMap), this.getZStackAction())

        List diskIdList = dataDiskToDelete(action.uuid)

        def result = action.call()
        result.throwExceptionIfError()

        diskIdList.each { String diskId ->
            deleteDataDisk(diskId)
        }

        this.afterCallZStackAction(result)

        return result
    }

    private List dataDiskToDelete(String vmId) {
        QueryVolumeAction query = new QueryVolumeAction(
                sessionId: sessionId,
                conditions: [
                        "vmInstanceUuid=${vmId}".toString(),
                        "__systemTag__=${EcsSystemTags.DELETE_WITH_INSTANCE.getTagFormat()}".toString()
                ]
        )
        QueryVolumeAction.Result result = query.call()
        result.throwExceptionIfError()
        if (result.value.inventories.size() == 0) {
            return
        }
        return result.value.inventories.stream().map({ vol -> vol.uuid }).collect(Collectors.toList())
    }

    private void deleteDataDisk(String volumeId) {
        DeleteDataVolumeAction deleteAct = new DeleteDataVolumeAction(
                sessionId: sessionId,
                uuid: volumeId
        )
        DeleteDataVolumeAction.Result dResult = deleteAct.call()
        dResult.throwExceptionIfError()

        QuerySystemTagAction query = new QuerySystemTagAction(
                sessionId: sessionId,
                conditions: [
                        "resourceType=VolumeVO",
                        "resourceUuid=${volumeId}".toString()
                ]
        )

        QuerySystemTagAction.Result qResult = query.call()
        qResult.throwExceptionIfError()

        if (qResult.value.inventories.size() == 0) {
            return
        }

        for (SystemTagInventory sTag : qResult.value.inventories) {
            DeleteTagAction delAct = new DeleteTagAction(
                    sessionId: sessionId,
                    uuid: sTag.uuid
            )
            delAct.call()
        }
    }
}
