package org.zstack.pluginpremium.externalapiadapter.api.ecs.instance

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.zstack.pluginpremium.externalapiadapter.api.BaseAPI
import org.zstack.pluginpremium.externalapiadapter.exception.APIParamConvertException
import org.zstack.sdk.*
import org.zstack.utils.gson.JSONObjectUtil

import static org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants.*

/**
 * Created by lining on 2018/5/31.
 */

class ModifyInstanceVpcAttribute extends BaseAPI {

    @Override
    Class getZStackAction() {
        return null
    }

    @Override
    protected void configAPIConversionSpec() {
        spec = config {
            convertAPIParam {
                simpleConvert {
                    ecsParamName = ECS_INSTANCE_ID
                    zstackParamName = ZSTACK_VM_INSTANCE_UUID
                }

                complexConvert {
                    ecsParamName = ECS_VPC_VSWITCH_ID
                    zstackParamName = "l3NetworkUuid"

                    getZstackValue = { String ecsParamValue ->
                        QueryAliyunProxyVSwitchAction action = new QueryAliyunProxyVSwitchAction(
                                sessionId: sessionId,
                                conditions: ["uuid=$ecsParamValue".toString()],
                                limit: 1
                        )
                        QueryAliyunProxyVSwitchAction.Result result = action.call()
                        result.throwExceptionIfError()

                        if (result.value.inventories.size() <= 0) {
                            throw new APIParamConvertException(ecsParamName, new ErrorCode(
                                    details: "The VSwitch[id: ${ecsParamValue}] is not found"
                            ))
                        }

                        AliyunProxyVSwitchInventory vSwitchInventory = result.value.inventories.first()
                        return vSwitchInventory.vpcL3NetworkUuid
                    }
                }
            }

            convertAPIResponse {
            }
        }
    }

    @Override
    Object callZStackAction() {
        QueryVmInstanceAction instanceAction = new QueryVmInstanceAction(
                sessionId: sessionId,
                conditions: ["uuid=${ecsAPIParamMap.get(ECS_INSTANCE_ID)}".toString()]
        )
        QueryVmInstanceAction.Result instanceResult = instanceAction.call()
        instanceResult.throwExceptionIfError()
        if (instanceResult.value.inventories.size() == 0) {
            throw new APIParamConvertException(ECS_INSTANCE_ID, "cannot find vm instance with id[${ecsAPIParamMap.get()}].")
        }
        VmInstanceInventory instanceInv = instanceResult.value.inventories.first()
        Optional<VmNicInventory> opt = instanceInv.vmNics.stream().filter { nic -> nic.l3NetworkUuid == zstackAPIParamMap.get("l3NetworkUuid") }.findAny()
        Class zstackAction
        if (opt.isPresent()) {
            zstackAction = SetVmStaticIpAction.class
            zstackAPIParamMap.put(ZSTACK_NIC_IP, ecsAPIParamMap.get(ECS_INSTANCE_PRIVATE_IP))
        } else {
            zstackAction = AttachL3NetworkToVmAction.class
            zstackAPIParamMap.put("staticIp", ecsAPIParamMap.get(ECS_INSTANCE_PRIVATE_IP))
        }
        Gson gson = new GsonBuilder().create()
        AbstractAction action = gson.fromJson(JSONObjectUtil.toJsonString(zstackAPIParamMap), zstackAction)
        def result = action.call()
        result.throwExceptionIfError()

        this.afterCallZStackAction(result)

        return result
    }

    @Override
    void afterCallZStackAction(Object zstackActionResult) {
        super.afterCallZStackAction(zstackActionResult)
        if (zstackActionResult instanceof AttachL3NetworkToVmAction.Result) {
            AttachL3NetworkToVmAction.Result result = zstackActionResult
            VmInstanceInventory vmInstanceInventory = result.value.inventory

            UpdateVmInstanceAction updateVmInstanceAction = new UpdateVmInstanceAction(
                    sessionId: sessionId,
                    uuid: vmInstanceInventory.uuid,
                    defaultL3NetworkUuid: zstackAPIParamMap.get("l3NetworkUuid"),
            )
            updateVmInstanceAction.call()

            VmNicInventory defaultNic = vmInstanceInventory.getVmNics().stream()
                    .filter { VmNicInventory nic -> nic.l3NetworkUuid == vmInstanceInventory.defaultL3NetworkUuid }
                    .findAny().get()
            DetachL3NetworkFromVmAction detachL3NetworkFromVmAction = new DetachL3NetworkFromVmAction(
                    sessionId: sessionId,
                    vmNicUuid: defaultNic.uuid
            )
            detachL3NetworkFromVmAction.call()
        }
    }
}
