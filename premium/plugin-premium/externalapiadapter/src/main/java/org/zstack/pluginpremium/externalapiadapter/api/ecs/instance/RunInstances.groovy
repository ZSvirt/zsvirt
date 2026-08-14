package org.zstack.pluginpremium.externalapiadapter.api.ecs.instance

import org.apache.commons.lang.StringUtils
import org.zstack.aliyunproxy.vpc.AliyunProxyVSwitchStatus
import org.zstack.appliancevm.ApplianceVmStatus
import org.zstack.core.thread.AsyncThread
import org.zstack.header.vm.VmInstanceState
import org.zstack.pluginpremium.externalapiadapter.EcsSystemTags
import org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterGlobalProperty
import org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterUtils
import org.zstack.pluginpremium.externalapiadapter.api.BaseAPI
import org.zstack.pluginpremium.externalapiadapter.convert.param.ParameterConversionUtils
import org.zstack.pluginpremium.externalapiadapter.exception.APIAdapterSpecifiedErrorException
import org.zstack.sdk.*

import java.util.concurrent.TimeUnit

import static org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants.*
/**
 * @Author: lining* @Date: 2018/07/19
 */
class RunInstances extends BaseAPI {
    private static final String VSWITCH_NOT_FOUND_CODE = "InvalidVSwitchId.NotFound"
    private static final String VSWITCH_NOT_FOUND_MESSAGE = "Specified virtual switch does not exist."
    private static final String VSWITCH_NOT_READY_CODE = "IncorrectVSwitchStatus"
    private static final String VSWITCH_NOT_READY_MESSAGE = "The current status of virtual switch does not support this operation."
    private static final String VPC_NOT_READY_CODE = "IncorrectVpcStatus"
    private static final String VPC_NOT_READY_MESSAGE = "The current status of vpc does not support this operation."
    private static final String SECURITY_GROUP_NOT_FOUND_CODE = "InvalidSecurityGroupId"
    private static final String SECURITY_GROUP_NOT_FOUND_MESSAGE = "The specified SecurityGroupId is invalid or does not exist."

    @Override
    Class getZStackAction() {
        return null
    }

    @Override
    void setEcsAPIParamDefaultValue(Map ecsAPIParamMap) {
        super.setEcsAPIParamDefaultValue(ecsAPIParamMap)

        if (!ecsAPIParamMap.containsKey("Amount")) {
            ecsAPIParamMap.put("Amount", "1")
        }
    }

    @Override
    void configAPIConversionSpec() {
        spec = config {
            convertAPIParam {
                simpleConvert {
                    ecsParamName = ECS_API_ZONEID_KEY

                    putZstackParamValue = { Map zstackParamMap, String zstackParamValue ->
                        String zoneId = ParameterConversionUtils.convertZoneId(sessionId, zstackParamValue)
                        ecsAPIParamMap.put(ecsParamName, zoneId)
                    }
                }

                simpleConvert {
                    ecsParamName = ECS_VPC_VSWITCH_ID
                    stillConvertParamWhenEcsParamValueIsNull = true

                    putZstackParamValue = { Map zstackParamMap, String zstackParamValue ->
                        if (zstackParamValue == null) {
                            ecsAPIParamMap[ECS_VPC_VSWITCH_ID] = ExternalAPIAdapterGlobalProperty.PUBLICL3NETWORKUUID
                            return
                        }

                        QueryAliyunProxyVSwitchAction queryVSwitch = new QueryAliyunProxyVSwitchAction(
                                sessionId: sessionId,
                                conditions: ["uuid=$zstackParamValue".toString()]
                        )
                        QueryAliyunProxyVSwitchAction.Result vswitchRes = queryVSwitch.call()
                        if (vswitchRes.error != null || vswitchRes.value.inventories.isEmpty()) {
                            throw new APIAdapterSpecifiedErrorException(VSWITCH_NOT_FOUND_CODE, VSWITCH_NOT_FOUND_MESSAGE)
                        }
                        AliyunProxyVSwitchInventory vSwitch = vswitchRes.value.inventories.first()
                        if (vSwitch.status != AliyunProxyVSwitchStatus.Available.toString()) {
                            throw new APIAdapterSpecifiedErrorException(VSWITCH_NOT_READY_CODE, VSWITCH_NOT_READY_MESSAGE)
                        }

                        QueryApplianceVmAction queryVRouter = new QueryApplianceVmAction(
                                sessionId: sessionId,
                                conditions: ["vmNics.l3NetworkUuid=$vSwitch.vpcL3NetworkUuid".toString()]
                        )
                        QueryApplianceVmAction.Result vRouterRes = queryVRouter.call()
                        if (vRouterRes.error == null && !vRouterRes.value.inventories.isEmpty()) {
                            ApplianceVmInventory vRouter = vRouterRes.value.inventories.first()
                            if (vRouter.state == VmInstanceState.Running.toString() && vRouter.status == ApplianceVmStatus.Connected.toString()) {
                                ecsAPIParamMap[ECS_VPC_VSWITCH_ID] = vSwitch.vpcL3NetworkUuid
                                return
                            }
                        }
                        throw new APIAdapterSpecifiedErrorException(VPC_NOT_READY_CODE, VPC_NOT_READY_MESSAGE)
                    }
                }

                simpleConvert {
                    ecsParamName = ECS_INSTANCE_TYPE

                    putZstackParamValue = { Map zstackParamMap, String zstackParamValue ->
                        String instanceOfferingUuid = ParameterConversionUtils.convertInstanceType(sessionId, zstackParamValue)
                        ecsAPIParamMap.put(ecsParamName, instanceOfferingUuid)
                        Map gpuSpecInfo = ParameterConversionUtils.getGPUSpecInfoMapByUuid(sessionId, instanceOfferingUuid)
                        if (gpuSpecInfo != null) {
                            String pciDeviceSpecUuid = ParameterConversionUtils.preCheckGPUSpec(sessionId, gpuSpecInfo)
                            String zoneId = ecsAPIParamMap.get(ECS_API_ZONEID_KEY)
                            String amount = gpuSpecInfo.get(EcsSystemTags.PCI_DEVICE_INFO_AMOUNT_TOKEN)
                            //As a pre-check
                            ParameterConversionUtils.getGPUSpecTag(sessionId, pciDeviceSpecUuid, zoneId, amount ?: "1")
                            ecsAPIParamMap[ZSTACK_PCI_DEVICE_SPEC_UUID_KEY] = pciDeviceSpecUuid
                            ecsAPIParamMap[ZSTACK_PCI_DEVICE_SPEC_AMOUNT_KEY] = amount ?: "1"
                        }
                    }
                }

                simpleConvert {
                    ecsParamName = ECS_SECURITY_GROUP_ID
                    zstackParamName = ZSTACK_SECURITY_GROUP_ID

                    putZstackParamValue = { Map zstackParamMap, String zstackParamValue ->
                        String sgUuid = zstackParamValue
                        QuerySecurityGroupAction action = new QuerySecurityGroupAction(
                                conditions: ["uuid=$sgUuid".toString()],
                                sessionId: sessionId
                        )
                        QuerySecurityGroupAction.Result result = action.call()
                        if (result.error != null || result.value.inventories.isEmpty()) {
                            throw new APIAdapterSpecifiedErrorException(SECURITY_GROUP_NOT_FOUND_CODE, SECURITY_GROUP_NOT_FOUND_MESSAGE)
                        }

                        return sgUuid
                    }
                }
            }

            convertAPIResponse {
                convertResponseAttribute {
                    ecsAttributeName = "InstanceIdSets"

                    getZstackAttributeValue = {
                        return zstackAPIRsp
                    }

                    addEcsValueToEcsAPIRsp = { Map ecsAPIRsp ->
                        (ecsAPIRsp[ecsAttributeName] = ["InstanceIdSet": zstackAttributeValue])
                    }
                }
            }
        }
    }

    @Override
    Object callZStackAction() {
        String amount = ecsAPIParamMap["Amount"]
        int count = Integer.parseInt(amount)
        String token = ecsAPIParamMap[ECS_API_CLIENTTOKEN_KEY]
        List instanceIndexedIds = []
        List instanceIds = []
        for (int idx = 1; idx <= count; idx++) {
            String instanceUuid
            if (token != null) {
                instanceUuid = ExternalAPIAdapterUtils.randomUUID(token + idx)
            } else {
                instanceUuid = ExternalAPIAdapterUtils.randomUUID()
            }
            instanceIndexedIds.add(new Tuple2(instanceUuid, idx))
            instanceIds.add(instanceUuid)
        }

        List errRes = new ArrayList<>()

        DoRunInstance runInstance = new DoRunInstance(instanceIndexedIds, errRes)
        Thread t = new Thread(runInstance)
        t.start()

        int waitingTime = ZSTACK_ASYNC_QUERY_COUNT
        int checkingInterval = QUERY_INTERVAL_TIME
        while (waitingTime >= 0) {
            waitingTime -= checkingInterval
            if (t.state == Thread.State.TERMINATED && errRes.isEmpty()) {
                return instanceIds
            }

            if (!errRes.isEmpty()) {
                Tuple2 tuple = errRes.first() as Tuple2
                Exception e = tuple?.second as Exception
                throw new APIAdapterSpecifiedErrorException("CreateInstanceFailed", "Failed to create one or more instance, details: ${e?.message}")
            }

            TimeUnit.SECONDS.sleep(checkingInterval)
        }

        // after waiting time we thought these actions will finally success
        return instanceIds
    }

    class DoRunInstance implements Runnable {
        List instanceIndexedIds
        List errRes

        DoRunInstance(List instanceIndexedIds, List errRes) {
            this.instanceIndexedIds = new ArrayList(instanceIndexedIds)
            this.errRes = Collections.synchronizedList(errRes)
        }

        @Override
        void run() {
            doRunInstance(instanceIndexedIds)
        }

        void doRunInstance(List indexedIds) {
            List results = Collections.synchronizedList(new ArrayList<>())
            List failedRes = Collections.synchronizedList(new ArrayList<>())
            boolean addIndex = indexedIds.size() > 1
            indexedIds.parallelStream().forEach { Tuple2<String, Integer> indexedId ->
                String subRequestId = ExternalAPIAdapterUtils.randomUUID()
                try {
                    RunInstance api = new RunInstance()
                    api.setSessionId(sessionId)
                    api.setRequestId(subRequestId)
                    api.instanceUuid = indexedId.getFirst()
                    if (addIndex) {
                        api.instanceIndex = indexedId.getSecond()
                    }
                    logger.info("RunInstances[requestId=${requestId}], One of the sub tasks is RunInstance[requestId=${subRequestId}]".toString())

                    String valueStr = api.call(ecsAPIParamMap)
                    Map value = ExternalAPIAdapterUtils.gson.fromJson(valueStr, Map.class)
                    results.add(value[ECS_INSTANCE_ID])
                    logger.info("RunInstances[requestId=${requestId}], One of the sub tasks RunInstance[requestId=${subRequestId}] complete successfully.")
                } catch (Exception e) {
                    logger.error("RunInstances[requestId=${requestId}], One of the sub tasks RunInstance[requestId=${subRequestId}] failed".toString(), e)
                    failedRes.add(indexedId.getFirst())
                    errRes.add(new Tuple2(indexedId.getFirst(), e))
                }
                logger.info("RunInstances[requestId=${requestId}], instances have been created:[${StringUtils.join(results)}]")
                if (failedRes.size() != 0) {
                    logger.debug("RunInstances[requestId=${requestId}], instances failed to be created:[${StringUtils.join(failedRes)}]")
                }
            }
        }
    }

    @AsyncThread
    private void needRollbackVms(List vmUuids) {
        if (vmUuids == null || vmUuids.isEmpty()) {
            return
        }

        for (String vmUuid : vmUuids) {
            DestroyVmInstanceAction action = new DestroyVmInstanceAction(
                    sessionId: sessionId,
                    uuid: vmUuid,
            )
            DestroyVmInstanceAction.Result result = action.call()
            if (result.error != null) {
                continue
            }

            ExpungeVmInstanceAction expungeAction = new ExpungeVmInstanceAction(
                    sessionId: sessionId,
                    uuid: vmUuid
            )
            expungeAction.call()
        }
    }
}
