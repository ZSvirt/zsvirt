package org.zstack.pluginpremium.externalapiadapter.api.ecs.snapshot


import org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterUtils
import org.zstack.pluginpremium.externalapiadapter.api.BaseAPI
import org.zstack.pluginpremium.externalapiadapter.exception.APIParamConvertException
import org.zstack.sdk.ApiException
import org.zstack.sdk.DeleteSchedulerJobAction
import org.zstack.sdk.QuerySchedulerJobAction

import static org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants.ECS_DISK_IDS

/**
 * @Author: fubang* @Date: 2018/5/1
 */
class CancelAutoSnapshotPolicy extends BaseAPI {

    @Override
    protected void configAPIConversionSpec() {
        spec = config {
            convertAPIParam {}
            convertAPIResponse {}
        }
    }

    @Override
    Object callZStackAction() {
        def ecsDiskIds = ecsAPIParamMap.get(ECS_DISK_IDS)
        if (ecsDiskIds == null) {
            throw new APIParamConvertException(ECS_DISK_IDS, "Action[name: CancelAutoSnapshotPolicy] should include param 'DiskIds'")
        }

        ArrayList<ApiException> queryErrors = new ArrayList<>()
        ArrayList<ApiException> deleteErrors = new ArrayList<>()
        ArrayList<String> diskIds = ExternalAPIAdapterUtils.changeValueType(ecsDiskIds, ArrayList.class)
        for (diskId in diskIds) {
            QuerySchedulerJobAction querySchedulerJobAction = new QuerySchedulerJobAction(
                    sessionId: sessionId,
                    conditions: ["targetResourceUuid=${diskId}".toString()]
            )

            QuerySchedulerJobAction.Result querySchedulerJobActionResult = querySchedulerJobAction.call()
            if (querySchedulerJobActionResult.error != null) {
                // should record exceptions and continue here
                queryErrors.add(querySchedulerJobActionResult.error)
//                println(querySchedulerJobActionResult.error.details)
                continue
            }

            def jobInventories = querySchedulerJobActionResult.value.inventories
            if (jobInventories.size() == 0) {
                logger.warn("[RequestId: ${this.requestId}] ${querySchedulerJobActionResult.class.simpleName} result is empty when querying condition is job.targetResourceUuid=${diskId}".toString())
                continue
            }

            for (jobInventory in jobInventories) {
                String job = jobInventory.uuid

                DeleteSchedulerJobAction deleteSchedulerJobAction = new DeleteSchedulerJobAction()
                deleteSchedulerJobAction.sessionId = sessionId
                deleteSchedulerJobAction.uuid = job

                DeleteSchedulerJobAction.Result deleteSchedulerJobActionResult = deleteSchedulerJobAction.call()
                if (deleteSchedulerJobActionResult.error != null) {

                    deleteErrors.add(deleteSchedulerJobActionResult.error)
//                    println(deleteSchedulerJobActionResult.error.details)
                }
            }

        }
        // TODO handle the exception
//        if (queryErrors.size() != 0) {}
//        if (deleteErrors.size() != 0) {}

        return null
    }

    /*
    @Override
    Object callZStackAction() {
        def ecsDiskIds = ecsAPIParamMap.get("DiskIds")
        if (ecsDiskIds == null){
            throw new APIParamConvertException("DiskIds","Action[name: CancelAutoSnapshotPolicy] should include param 'DiskIds'")
        }

        ArrayList<String> diskIds = ExternalAPIAdapterUtils.changeValueType(ecsDiskIds, ArrayList.class)
        for (diskId in diskIds) {
            QuerySchedulerTriggerAction querySchedulerTriggerAction = new QuerySchedulerTriggerAction()
            querySchedulerTriggerAction.sessionId = sessionId
            querySchedulerTriggerAction.conditions = ["job.targetResourceUuid=$diskId".toString()]

            QuerySchedulerTriggerAction.Result querySchedulerTriggerActionResult = querySchedulerTriggerAction.call()
            if (querySchedulerTriggerActionResult.error != null) {
                // TODO how to handler the exception
                // should record exceptions and continue here
                println(querySchedulerTriggerActionResult.error.details)
                continue
            }

            def triggerInventories = querySchedulerTriggerActionResult.value.inventories
            if (triggerInventories.size() == 0) {
                logger.warn("[RequestId: ${this.requestId}] ${querySchedulerTriggerActionResult.class.simpleName} result is empty when querying condition is job.targetResourceUuid=$diskId".toString())
                continue
            }

            for (triggerInventory in triggerInventories){
                String trigger = triggerInventory.uuid

                QuerySchedulerJobAction querySchedulerJobAction = new QuerySchedulerJobAction()
                querySchedulerJobAction.sessionId = sessionId
                querySchedulerJobAction.conditions = ["targetResourceUuid=$diskId".toString(),"trigger.uuid=$trigger".toString()]

                QuerySchedulerJobAction.Result querySchedulerJobActionResult = querySchedulerJobAction.call()
                if (querySchedulerJobActionResult.error != null) {
                    // TODO how to handler the exception
                    println(querySchedulerJobActionResult.error.details)
                    continue
                }

                def jobInventories = querySchedulerJobActionResult.value.inventories
                if (jobInventories.size() == 0) {
                    logger.warn("[RequestId: ${this.requestId}] ${querySchedulerJobActionResult.class.simpleName} result is empty when querying condition is targetResourceUuid=$diskId".toString())
                    continue
                }

                for (jobInventory in jobInventories){
                    String job = jobInventory.uuid

                    //this step is not required
                    RemoveSchedulerJobFromSchedulerTriggerAction removeSchedulerJobFromSchedulerTriggerAction = new RemoveSchedulerJobFromSchedulerTriggerAction()
                    removeSchedulerJobFromSchedulerTriggerAction.sessionId = sessionId
                    removeSchedulerJobFromSchedulerTriggerAction.schedulerTriggerUuid = trigger
                    removeSchedulerJobFromSchedulerTriggerAction.schedulerJobUuid = job

                    RemoveSchedulerJobFromSchedulerTriggerAction.Result removeSchedulerJobFromSchedulerTriggerActionResult = removeSchedulerJobFromSchedulerTriggerAction.call()
                    if (removeSchedulerJobFromSchedulerTriggerActionResult.error != null) {

                        println(removeSchedulerJobFromSchedulerTriggerActionResult.error.details)
                        continue
                    }

                    DeleteSchedulerJobAction deleteSchedulerJobAction = new DeleteSchedulerJobAction()
                    deleteSchedulerJobAction.sessionId = sessionId
                    deleteSchedulerJobAction.uuid = job

                    DeleteSchedulerJobAction.Result deleteSchedulerJobActionResult = deleteSchedulerJobAction.call()
                    if (deleteSchedulerJobActionResult.error != null) {
                        // TODO how to handler the exception
                        println(deleteSchedulerJobActionResult.error.details)
                    }
                }
            }
        }

        return null
    }

     */

    @Override
    Class getZStackAction() {
        return null
    }
}
