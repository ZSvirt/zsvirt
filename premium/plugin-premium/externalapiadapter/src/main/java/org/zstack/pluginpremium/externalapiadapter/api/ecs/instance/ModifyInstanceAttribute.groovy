package org.zstack.pluginpremium.externalapiadapter.api.ecs.instance

import org.apache.commons.lang.StringUtils
import org.zstack.header.image.ImagePlatform
import org.zstack.header.vm.VmInstanceState
import org.zstack.pluginpremium.externalapiadapter.api.BaseAPI
import org.zstack.pluginpremium.externalapiadapter.exception.APIAdapterSpecifiedErrorException
import org.zstack.pluginpremium.externalapiadapter.exception.APIParamConvertException
import org.zstack.sdk.*

import static org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants.*
/**
 * @Author: fubang
 * @Date: 2018/4/23
 */
class ModifyInstanceAttribute extends BaseAPI {
    private static final String NOT_RUNNING_CODE = "InvalidInstanceStatus.NotRunning"
    private static final String NOT_RUNNING_MESSAGE = "The current status of the resource is invalid, you can only do this operation when instance is running."

    @Override
    void configAPIConversionSpec() {
        spec = config {
            convertAPIParam {

                simpleConvert {
                    ecsParamName = ECS_INSTANCE_ID
                    zstackParamName = ZSTACK_UUID
                }
                simpleConvert {
                    ecsParamName = ECS_INSTANCE_NAME
                    zstackParamName = ZSTACK_NAME
                }
                simpleConvert {
                    ecsParamName = ECS_API_DESCRIPTION_KEY
                    zstackParamName = ZSTACK_API_DESCRIPTION_KEY
                }

            }
            convertAPIResponse {}
        }
    }

    private void convertPassword(String paramName, Map ecsAPIParamMap, VmInstanceInventory vm) {
        if (vm.state != VmInstanceState.Running.toString()) {
            throw new APIAdapterSpecifiedErrorException(NOT_RUNNING_CODE, NOT_RUNNING_MESSAGE)
        }
        ChangeVmPasswordAction action = new ChangeVmPasswordAction()
        action.sessionId = sessionId
        action.uuid = ecsAPIParamMap[ECS_INSTANCE_ID]
        action.account = getAccountNameByPlatform(vm.platform)
        action.password = ecsAPIParamMap[paramName]

        ChangeVmPasswordAction.Result result = action.call()

        if (result.error != null) {
            throw new APIParamConvertException(paramName, result.error)
        }
    }

    private void convertHostName(String paramName, Map ecsAPIParamMap) {
        SetVmHostnameAction action = new SetVmHostnameAction()
        action.sessionId = sessionId
        action.uuid = ecsAPIParamMap.get(ECS_INSTANCE_ID)
        action.hostname = ecsAPIParamMap.get(paramName)

        SetVmHostnameAction.Result result = action.call()
        if (result.error != null) {
            throw new APIParamConvertException(paramName, result.error)
        }
    }

    private void convertUserdata(String paramName, Map ecsAPIParamMap) {
        QuerySystemTagAction action = new QuerySystemTagAction()
        action.sessionId = sessionId
        action.conditions = ["resourceUuid=${ecsAPIParamMap.get(ECS_INSTANCE_ID)}".toString(), "resourceType=VmInstanceVO"]
        QuerySystemTagAction.Result result = action.call()
        if (result.error != null) {
            throw new APIParamConvertException(paramName, result.error)
        }

        def targetObject = result.value.inventories.grep { it.tag.indexOf("userdata::") >= 0 }
        def targetSize = targetObject.size()
        if (targetSize != 0) {
            DeleteTagAction deleteTagAction = new DeleteTagAction(
                    sessionId: sessionId,
                    uuid: targetObject.first().uuid
            )
            deleteTagAction.call()
        }

        String userdata = ecsAPIParamMap[paramName]
        if (StringUtils.isBlank(userdata)) {
            return
        }

        CreateSystemTagAction createNewTagAction = new CreateSystemTagAction(
                sessionId: sessionId,
                resourceUuid: ecsAPIParamMap[ECS_INSTANCE_ID],
                resourceType: "VmInstanceVO",
                tag: "userdata::$userdata".toString(),
        )

        CreateSystemTagAction.Result creatNewTagResult = createNewTagAction.call()
        if (creatNewTagResult.error != null) {
            throw new APIParamConvertException(paramName, creatNewTagResult.error)
        }
    }

    @Override
    void afterCallZStackAction(Object zstackActionResult) {
        super.afterCallZStackAction(zstackActionResult)
        zstackActionResult = zstackActionResult as UpdateVmInstanceAction.Result

        String paramValue = ecsAPIParamMap[ECS_INSTANCE_PASSWORD]
        if (StringUtils.isNotBlank(paramValue)) {
            convertPassword(ECS_INSTANCE_PASSWORD, ecsAPIParamMap, zstackActionResult.value.inventory)
        }

        paramValue = ecsAPIParamMap[ECS_INSTANCE_HOSTNAME]
        if (StringUtils.isNotBlank(paramValue)) {
            convertHostName(ECS_INSTANCE_HOSTNAME, ecsAPIParamMap)
        }

        paramValue = ecsAPIParamMap[ECS_USERDATA]
        if (StringUtils.isNotBlank(paramValue)) {
            convertUserdata(ECS_USERDATA, ecsAPIParamMap)
        }
    }

    @Override
    Class getZStackAction() {
        return UpdateVmInstanceAction.class
    }

    static String getAccountNameByPlatform(String platform) {
        if (ImagePlatform.Windows.toString() == platform || ImagePlatform.WindowsVirtio.toString() == platform) {
            return "Administrator"
        }
        return "root"
    }
}
