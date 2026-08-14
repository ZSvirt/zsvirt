package org.zstack.pluginpremium.externalapiadapter.api.ecs.instance


import org.zstack.compute.vm.VmSystemTags
import org.zstack.ha.HaSystemTags
import org.zstack.ha.VmHaLevel
import org.zstack.header.affinitygroup.AffinityGroupSystemTags
import org.zstack.header.vm.VmCreationStrategy
import org.zstack.pluginpremium.externalapiadapter.EcsSystemTags
import org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterUtils
import org.zstack.pluginpremium.externalapiadapter.api.BaseAsyncAPI
import org.zstack.pluginpremium.externalapiadapter.convert.param.ParameterConversionUtils
import org.zstack.pluginpremium.externalapiadapter.exception.APIParamConvertException
import org.zstack.sdk.*
import org.zstack.utils.data.SizeUnit

import static org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants.*
/**
 * Created by lining on 2018/4/15.
 */
class CreateInstance extends BaseAsyncAPI<CreateVmInstanceAction.Result> {
    @Override
    Class getZStackAction() {
        return CreateVmInstanceAction.class
    }

    @Override
    void configAPIConversionSpec() {
        spec = config {
            convertAPIParam {
                simpleConvert {
                    ecsParamName = ECS_API_ZONEID_KEY
                    zstackParamName = ZSTACK_API_ZONEID_KEY

                    putZstackParamValue = { Map zstackParamMap, String zstackParamValue ->
                        String zoneId = ParameterConversionUtils.convertZoneId(sessionId, zstackParamValue)
                        zstackParamMap.put(zstackParamName, zoneId)
                    }
                }

                simpleConvert {
                    ecsParamName = ECS_IMAGE_ID
                    zstackParamName = ZSTACK_IMAGE_ID
                }

                simpleConvert {
                    ecsParamName = ECS_INSTANCE_TYPE
                    zstackParamName = ZSTACK_INSTANCE_TYPE_ID

                    putZstackParamValue = { Map zstackParamMap, String zstackParamValue ->
                        String instanceOfferingUuid = ParameterConversionUtils.convertInstanceType(sessionId, zstackParamValue)
                        zstackParamMap.put(zstackParamName, instanceOfferingUuid)
                    }
                }

                simpleConvert {
                    ecsParamName = ECS_API_DESCRIPTION_KEY
                    zstackParamName = ZSTACK_API_DESCRIPTION_KEY
                }

                simpleConvert {
                    ecsParamName = ECS_INSTANCE_NAME
                    zstackParamName = ZSTACK_NAME
                    stillConvertParamWhenEcsParamValueIsNull = true

                    putZstackParamValue = { zstackParamMap, zstackParamValue ->
                        if (zstackParamValue == null) {
                            zstackParamValue = "untitled"
                        }
                        zstackParamMap.put(zstackParamName, zstackParamValue)
                    }
                }

                systemTagConvert {
                    ecsParamName = ECS_INSTANCE_HOSTNAME
                    stillConvertParamWhenEcsParamValueIsNull = true

                    getTag = { String ecsParamValue ->
                        if (ecsParamValue == null) {
                            ecsParamValue = "iz"+ ExternalAPIAdapterUtils.randomUUID()
                        }

                        return VmSystemTags.HOSTNAME.instantiateTag([(VmSystemTags.HOSTNAME_TOKEN): ecsParamValue])
                    }
                }

                systemTagConvert {
                    ecsParamName = ECS_INSTANCE_PASSWORD
                    getTag = { ecsParamValue ->
                        return VmSystemTags.ROOT_PASSWORD.instantiateTag([(VmSystemTags.ROOT_PASSWORD_TOKEN): ecsParamValue])
                    }
                }

                systemTagConvert {
                    ecsParamName = ECS_INSTANCE_PRIVATE_IP
                    getTag = { ecsParamValue ->
                        String l3NetworkUuid = ecsAPIParamMap.get(ECS_VPC_VSWITCH_ID)
                        return VmSystemTags.STATIC_IP.instantiateTag([
                                (VmSystemTags.STATIC_IP_L3_UUID_TOKEN): l3NetworkUuid,
                                (VmSystemTags.STATIC_IP_TOKEN): ecsParamValue
                        ])
                    }
                }

                systemTagConvert {
                    ecsParamName = ECS_USERDATA
                    getTag = { String ecsParamValue ->
                        return VmSystemTags.USERDATA.instantiateTag([(VmSystemTags.USERDATA_TOKEN): ecsParamValue])
                    }
                }

                systemTagConvert {
                    ecsParamName = ECS_DEPLOYMENT_SET_ID
                    getTag = { String ecsParamValue ->
                        return AffinityGroupSystemTags.AFFINITY_GROUP_UUID.instantiateTag([(AffinityGroupSystemTags.AFFINITY_GROUP_UUID_TOKEN): ecsParamValue])
                    }
                }

                systemTagConvert {
                    ecsParamName = ECS_INSTANCE_TYPE_ID

                    getTag = { String instanceTypeId ->
                        Map gpuInfoMap = ParameterConversionUtils.getGPUSpecInfoMapByName(sessionId, instanceTypeId)
                        if (gpuInfoMap == null) {
                            return
                        }
                        String pciSpecUuid = ParameterConversionUtils.preCheckGPUSpec(sessionId, gpuInfoMap)
                        String zoneId = ecsAPIParamMap.get(ECS_API_ZONEID_KEY)
                        String amount = gpuInfoMap.get(EcsSystemTags.PCI_DEVICE_INFO_AMOUNT_TOKEN)
                        return ParameterConversionUtils.getGPUSpecTag(sessionId, pciSpecUuid, zoneId, amount)
                    }
                }

                systemTagConvert {
                    stillConvertParamWhenEcsParamValueIsNull = true
                    getTag = { String ecsValue ->
                        return HaSystemTags.HA.instantiateTag([(HaSystemTags.HA_TOKEN): VmHaLevel.NeverStop.toString()])
                    }
                }

                complexConvert {
                    ecsParamName = ECS_INSTANCE_SYSTEM_DISK_SIZE
                    ecsParamType = Integer.class
                    zstackParamName = ZSTACK_INSTANCE_SYSTEM_DISK_SIZE

                    getZstackValue = { String ecsParamValue ->
                        final int dataDiskSize = Integer.parseInt(ecsParamValue)
                        return ParameterConversionUtils.getDiskOffering(sessionId, requestId, ecsParamName, dataDiskSize)
                    }
                }

                for (n in 1..16) {
                    complexConvert {
                        ecsParamName = "DataDisk.${n}.Size".toString()
                        ecsParamType = Integer.class
                        zstackParamName = ZSTACK_INSTANCE_DATA_DISK_SIZE

                        getZstackValue = { String ecsParamValue ->
                            final int dataDiskSize = Integer.parseInt(ecsParamValue)
                            return ParameterConversionUtils.getDiskOffering(sessionId, requestId, ecsParamName, dataDiskSize)
                        }

                        putZstackParamValue = { zstackParamMap, zstackParamValue ->
                            def value = zstackParamMap.get(zstackParamName)
                            if (value == null) {
                                value = []
                                zstackParamMap.put(zstackParamName, value)
                            }
                            value.add(zstackParamValue)
                        }
                    }
                }

                zstackNeedParam {
                    zstackParamName = ZSTACK_INSTANCE_L3NETWORKS
                    getZstackValue = { Map ecsParamMap, Map zstackParamMap ->
                        String vswitchParam = ECS_VPC_VSWITCH_ID
                        String vswitchId = ecsParamMap.get(vswitchParam)
                        zstackParamMap.put(ZSTACK_INSTANCE_DEFAULT_NETWORK, vswitchId)
                        List l3NetworkUuids = [vswitchId]
                        return l3NetworkUuids
                    }
                }

                zstackNeedParam {
                    zstackParamName = ZSTACK_RESOURCEUUID_KEY
                    getZstackValue = { Map ecsParamMap, Map zstackParamMap ->
                        String clientToken = ecsParamMap.get(ECS_API_CLIENTTOKEN_KEY)
                        return ExternalAPIAdapterUtils.randomUUID(clientToken)
                    }
                }

                zstackNeedParam {
                    zstackParamName = ZSTACK_INSTANCE_CREATE_STRATEGY
                    getZstackValue = { Map ecsParamMap, Map zstackParamMap ->
                        return VmCreationStrategy.CreateStopped.toString()
                    }
                }
            }

            convertAPIResponse {

                convertResponseAttribute {
                    ecsAttributeName = ECS_INSTANCE_ID

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

    private void addVmToSecurityGroup(VmInstanceInventory vmInventory) {
        String paramName = ECS_SECURITY_GROUP_ID
        if (!ecsAPIParamMap.containsKey(paramName)) {
            return
        }

        def sgUuid = ecsAPIParamMap.get(paramName)

        QuerySecurityGroupAction action = new QuerySecurityGroupAction(
                conditions : ["uuid=$sgUuid".toString()],
                sessionId : sessionId
        )
        QuerySecurityGroupAction.Result result = action.call()

        if (result.error != null) {
            throw new APIParamConvertException(paramName, result.error)
        }

        if (result.value.inventories.size() <= 0) {
            throw new APIParamConvertException(paramName, new ErrorCode(
                    details: "security group[uuid: ${sgUuid}] is not found"
            ))
        }

        SecurityGroupInventory inventory = result.value.inventories.get(0) as SecurityGroupInventory
        def haveSecrityGroup = inventory.attachedL3NetworkUuids.find {
            it == vmInventory.defaultL3NetworkUuid
        }

        if (haveSecrityGroup == null) {
            AttachSecurityGroupToL3NetworkAction action2 = new AttachSecurityGroupToL3NetworkAction()
            action2.sessionId = sessionId
            action2.l3NetworkUuid = vmInventory.defaultL3NetworkUuid
            action2.securityGroupUuid = sgUuid

            AttachSecurityGroupToL3NetworkAction.Result result2 = action2.call()
            if (result2.error != null) {
                throw new APIParamConvertException(paramName, result2.error)
            }

        }

        AddVmNicToSecurityGroupAction action3 = new AddVmNicToSecurityGroupAction()
        action3.vmNicUuids = vmInventory.vmNics.collect { it.uuid }
        action3.securityGroupUuid = sgUuid
        action3.sessionId = sessionId

        AddVmNicToSecurityGroupAction.Result result3 = action3.call()
        if (result3.error != null) {
            throw new APIParamConvertException(paramName, result3.error)
        }
    }

    private void convertDisk(String diskName, String diskDescription, Map paramMap, String diskUuid) {

        def name = paramMap.get(diskName)
        def description = paramMap.get(diskDescription)
        UpdateVolumeAction action = new UpdateVolumeAction()
        action.uuid = diskUuid
        action.sessionId = sessionId
        if (name == null && description == null) {
            return
        }

        if (name != null) {
            action.name = name
        }

        if (description != null) {
            action.description = description
        }

        UpdateVolumeAction.Result result = action.call()
        if (result.error != null) {
            throw new APIParamConvertException("SystemDisk.DiskName || SystemDisk.Description", result.error)
        }
    }

    //  vm does not have a public network nic.
	private void setVmPublicNicBandWidth(VmInstanceInventory vmInventory) {

		String internetMaxBandwidthIn = ecsAPIParamMap.get("InternetMaxBandwidthIn")
		String internetMaxBandwidthOut = ecsAPIParamMap.get("InternetMaxBandwidthOut")

		String nicId = vmInventory.vmNics.get(0).uuid
		SetNicQosAction action = new SetNicQosAction(
				sessionId: sessionId,
				uuid: nicId,
				inboundBandwidth: internetMaxBandwidthIn ? SizeUnit.MEGABYTE.toByte(Integer.parseInt(internetMaxBandwidthIn)) : SizeUnit.MEGABYTE.toByte(200),
				outboundBandwidth: internetMaxBandwidthIn ? SizeUnit.MEGABYTE.toByte(Integer.parseInt(internetMaxBandwidthOut)) : SizeUnit.KILOBYTE.toByte(9),
		)

		SetNicQosAction.Result result = action.call()
		result.throwExceptionIfError()
	}

    private void updateVolumeNameAndDesc(VmInstanceInventory vmInventory) {
        String rootUuid = vmInventory.rootVolumeUuid as String
        convertDisk("SystemDisk.DiskName", "SystemDisk.Description", ecsAPIParamMap, rootUuid)

        int j = 0
        def dataDisks = vmInventory.allVolumes.grep {
            it.uuid != rootUuid
        }.collect { it.uuid }
        for (i in 1..16) {
            String diskName = "DataDisk.${i}.DiskName"
            if (ExternalAPIAdapterUtils.checkParam(diskName, ecsAPIParamMap)) {
                String diskDesc = "DataDisk.${i}.Description"
                String diskUuid = dataDisks.get(j)
                convertDisk(diskName, diskDesc, ecsAPIParamMap, diskUuid)
                j++
            }
        }
    }

    @Override
    void setEcsAPIParamDefaultValue(Map ecsAPIParamMap) {
        super.setEcsAPIParamDefaultValue(ecsAPIParamMap)

        String vSwitchId = ecsAPIParamMap.get(ECS_VPC_VSWITCH_ID) as String
        if (vSwitchId != null) { // VSwitchId is not required
            String l3NetworkUuid = ParameterConversionUtils.getL3NetworkUuidFromVSwitch(sessionId, vSwitchId)
            ecsAPIParamMap.put(ECS_VPC_VSWITCH_ID, l3NetworkUuid)
        }
    }

    @Override
    void afterCallZStackAction(Object zstackActionResult) {
        super.afterCallZStackAction(zstackActionResult)

        CreateVmInstanceAction.Result result = zstackActionResult
        VmInstanceInventory vmInventory = result.value.inventory

        updateVolumeNameAndDesc(vmInventory)

        addVmToSecurityGroup(vmInventory)

        //setVmPublicNicBandWidth(vmInventory)
    }
}
