package org.zstack.pluginpremium.externalapiadapter.api.ecs.eip

import org.zstack.network.service.eip.EipVO
import org.zstack.pluginpremium.externalapiadapter.EcsSystemTags
import org.zstack.pluginpremium.externalapiadapter.api.BaseAsyncAPI
import org.zstack.pluginpremium.externalapiadapter.exception.APIAdapterSpecifiedErrorException
import org.zstack.sdk.*

import static org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants.*

class AssociateEipAddress extends BaseAsyncAPI {
    static final String INSTANCE_NOT_FOUND_CODE = "InvalidInstanId.NotFound"
    static final String INSTANCE_NOT_FOUND_MESSAGE = "Specified instance does not exist."
    static final String INVALID_INSTANCE_TYPE_CODE = "InvalidInstanceType.ValueNotSupported"
    static final String INVALID_INSTANCE_TYPE_MESSAGE = "The specified value of InstanceType is not supported."
    static final String BAD_INSTANCE_STATUS_CODE = "IncorrectInstanceStatus"
    static final String BAD_INSTANCE_STATUS_MESSAGE = "Current instance status does not support this operation."

    SystemTagInventory intermediateStatusTag

    @Override
    protected void configAPIConversionSpec() {
        spec = config {
            convertAPIParam {
                simpleConvert {
                    ecsParamName = ECS_EIP_ALLOCATION_ID

                    zstackParamName = "eipUuid"
                }

                complexConvert {
                    ecsParamName = ECS_INSTANCE_ID

                    zstackParamName = "vmNicUuid"

                    getZstackValue = { Map ecsParamMap, String ecsParamValue ->
                        String instanceType = ecsParamMap.getOrDefault(ECS_INSTANCE_TYPE, "EcsInstance")

                        switch (instanceType) {
                            case "EcsInstance":
                                QueryVmInstanceAction queryVm = new QueryVmInstanceAction(
                                        sessionId: sessionId,
                                        conditions: ["uuid=$ecsParamValue".toString()]
                                )
                                QueryVmInstanceAction.Result vmRes = queryVm.call()
                                if (vmRes.error != null || vmRes.value.inventories.isEmpty()) {
                                    throw new APIAdapterSpecifiedErrorException(
                                            INSTANCE_NOT_FOUND_CODE,
                                            INSTANCE_NOT_FOUND_MESSAGE
                                    )
                                }

                                VmInstanceInventory vm = vmRes.value.inventories.first()
                                String defL3 = vm.defaultL3NetworkUuid
                                VmNicInventory nic = vm.vmNics.find { it.l3NetworkUuid == defL3 }
                                return nic.uuid

                            case "NetworkInterface":
                                QueryVmNicAction queryNic = new QueryVmNicAction(
                                        sessionId: sessionId,
                                        conditions: ["uuid=$ecsParamValue".toString()]
                                )
                                QueryVmNicAction.Result nicRes = queryNic.call()
                                if (nicRes.error != null || nicRes.value.inventories.isEmpty()) {
                                    throw new APIAdapterSpecifiedErrorException(
                                            INSTANCE_NOT_FOUND_CODE,
                                            INSTANCE_NOT_FOUND_MESSAGE
                                    )
                                }

                                VmNicInventory nic = nicRes.value.inventories.first()
                                if (nic.vmInstanceUuid == null) {
                                    throw new APIAdapterSpecifiedErrorException(
                                            BAD_INSTANCE_STATUS_CODE,
                                            BAD_INSTANCE_STATUS_MESSAGE
                                    )
                                }
                                return nic.uuid

                                // not support these instance types
                            case "SlbInstance":
                            case "Nat":
                            case "HaVip":
                            default:
                                throw new APIAdapterSpecifiedErrorException(
                                        INVALID_INSTANCE_TYPE_CODE,
                                        INVALID_INSTANCE_TYPE_MESSAGE
                                )
                        }
                    }

                    putZstackParamValue = { Map zstackParamMap, String zstackParamValue ->
                        (zstackParamMap[zstackParamName] = zstackParamValue)
                    }
                }
            }

            convertAPIResponse {}
        }
    }

    @Override
    Class getZStackAction() {
        return AttachEipAction.class
    }

    @Override
    Object callZStackAction() {
        String eipUuid = zstackAPIParamMap["eipUuid"]
        CreateSystemTagAction tagAct = new CreateSystemTagAction(
                sessionId: sessionId,
                resourceUuid: eipUuid,
                resourceType: EipVO.class.getSimpleName(),
                tag: EcsSystemTags.EIP_INTERMEDIATE_STATUS.instantiateTag([
                        (EcsSystemTags.EIP_INTERMEDIATE_STATUS_TOKEN): "Associating"
                ])
        )
        CreateSystemTagAction.Result tagRes = tagAct.call()
        intermediateStatusTag = tagRes?.value?.inventory
        return super.callZStackAction()
    }

    @Override
    void finishAsyncCallZStackAction() {
        if (intermediateStatusTag != null) {
            DeleteTagAction delTagAct = new DeleteTagAction(
                    sessionId: sessionId,
                    uuid: intermediateStatusTag.uuid
            )
            delTagAct.call()
        }
        super.finishAsyncCallZStackAction()
    }
}
