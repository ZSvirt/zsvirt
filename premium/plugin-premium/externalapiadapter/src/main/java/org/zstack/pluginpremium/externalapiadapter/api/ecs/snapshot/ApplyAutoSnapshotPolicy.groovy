package org.zstack.pluginpremium.externalapiadapter.api.ecs.snapshot


import org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterUtils
import org.zstack.pluginpremium.externalapiadapter.api.BaseAPI
import org.zstack.pluginpremium.externalapiadapter.exception.APIParamConvertException
import org.zstack.sdk.*

import static org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants.ECS_DISK_IDS

/**
 * @Author: fubang* @Date: 2018/5/1
 */
class ApplyAutoSnapshotPolicy extends BaseAPI {
    @Override
    protected void configAPIConversionSpec() {
        spec = config {
            convertAPIParam {}
            convertAPIResponse {}
        }
    }

    @Override
    Object callZStackAction() {
        String autoSnapshotPolicyId = ecsAPIParamMap.get("AutoSnapshotPolicyId")
        if (autoSnapshotPolicyId == null){
            throw new APIParamConvertException(ECS_DISK_IDS, "Action[name: ApplyAutoSnapshotPolicy] should include param 'AutoSnapshotPolicyId'")
        }

        def ecsDiskIds = ecsAPIParamMap.get(ECS_DISK_IDS)
        if (ecsDiskIds == null){
            throw new APIParamConvertException(ECS_DISK_IDS, "Action[name: ApplyAutoSnapshotPolicy] should include param 'DiskIds'")
        }

        ArrayList<String> diskIds = ExternalAPIAdapterUtils.changeValueType(ecsDiskIds as String, ArrayList.class)

        ArrayList<String> schedulerJobs = new ArrayList<>()

        for (diskId in diskIds) {
            QuerySchedulerJobAction querySchedulerJobAction = new QuerySchedulerJobAction(
                    sessionId: sessionId,
                    conditions: ["targetResourceUuid=${diskId}".toString()]
            )
            QuerySchedulerJobAction.Result querySchedulerJobResult = querySchedulerJobAction.call()
            querySchedulerJobResult.throwExceptionIfError()
            if (querySchedulerJobResult.value.inventories.size() != 0) {
                for (SchedulerJobInventory jobInventory in querySchedulerJobResult.value.inventories) {
                    //an userTag may be a better solution
                    if (jobInventory.name == "AutoSnapshotPolicyJob") {
                        DeleteSchedulerJobAction delete = new DeleteSchedulerJobAction(
                                sessionId: sessionId,
                                uuid: jobInventory.uuid
                        )
                        DeleteSchedulerJobAction.Result dResult = delete.call()
                        dResult.throwExceptionIfError()
                    }
                }
            }

            CreateSchedulerJobAction createSchedulerJobAction = new CreateSchedulerJobAction()
            createSchedulerJobAction.name = "AutoSnapshotPolicyJob"
            createSchedulerJobAction.targetResourceUuid = diskId
            createSchedulerJobAction.type = "volumeSnapshot"
            createSchedulerJobAction.sessionId = sessionId

            CreateSchedulerJobAction.Result createSchedulerJobActionResult = createSchedulerJobAction.call()
            if (createSchedulerJobActionResult.error != null) {

                rollbackCreatedJobs(schedulerJobs)
                createSchedulerJobActionResult.throwExceptionIfError()
            }

            String jobId = createSchedulerJobActionResult.value.inventory.uuid

            schedulerJobs.add(jobId)

            AddSchedulerJobToSchedulerTriggerAction addSchedulerJobToSchedulerTriggerAction = new AddSchedulerJobToSchedulerTriggerAction()
            addSchedulerJobToSchedulerTriggerAction.schedulerJobUuid = jobId
            addSchedulerJobToSchedulerTriggerAction.schedulerTriggerUuid = autoSnapshotPolicyId
            addSchedulerJobToSchedulerTriggerAction.sessionId = sessionId

            AddSchedulerJobToSchedulerTriggerAction.Result addSchedulerJobToSchedulerTriggerActionResult = addSchedulerJobToSchedulerTriggerAction.call()
            if (addSchedulerJobToSchedulerTriggerActionResult.error != null) {
                rollbackCreatedJobs(schedulerJobs)
                addSchedulerJobToSchedulerTriggerActionResult.throwExceptionIfError()
            }

        }

        return null
    }

    private void rollbackCreatedJobs(ArrayList<String> schedulerJobs) {
        for (jobId in schedulerJobs) {
            DeleteSchedulerJobAction deleteSchedulerJobAction = new DeleteSchedulerJobAction(
                    sessionId: sessionId,
                    uuid: jobId
            )
            deleteSchedulerJobAction.call()
        }
    }

//    private void rollbackAppliedJobs(ArrayList<String> schedulerJobs) {}

    @Override
    Class getZStackAction() {
        return null
    }
}
