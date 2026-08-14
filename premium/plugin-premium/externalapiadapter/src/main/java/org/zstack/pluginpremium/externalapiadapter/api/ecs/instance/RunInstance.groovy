package org.zstack.pluginpremium.externalapiadapter.api.ecs.instance

import org.apache.commons.lang.StringUtils
import org.zstack.compute.vm.VmSystemTags
import org.zstack.ha.HaSystemTags
import org.zstack.ha.VmHaLevel
import org.zstack.header.affinitygroup.AffinityGroupSystemTags
import org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterUtils
import org.zstack.pluginpremium.externalapiadapter.api.BaseAsyncAPI
import org.zstack.pluginpremium.externalapiadapter.convert.param.ParameterConversionUtils
import org.zstack.pluginpremium.externalapiadapter.exception.APIAdapterSpecifiedErrorException
import org.zstack.pluginpremium.externalapiadapter.exception.APIParamConvertException
import org.zstack.sdk.*
import org.zstack.utils.data.SizeUnit

import static org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants.*
/**
 * @Author: fubang* @Date: 2018/5/28
 */
class RunInstance extends BaseAsyncAPI<CreateVmInstanceAction.Result> {
    private static final String SECURITY_GROUP_NOT_FOUND_CODE = "InvalidSecurityGroupId"
    private static final String SECURITY_GROUP_NOT_FOUND_MESSAGE = "The specified SecurityGroupId is invalid or does not exist."

    String instanceUuid
    int instanceIndex = -1

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
                }

                simpleConvert {
                    ecsParamName = ECS_IMAGE_ID
                    zstackParamName = ZSTACK_IMAGE_ID
                }

                simpleConvert {
                    ecsParamName = ECS_INSTANCE_TYPE
                    zstackParamName = ZSTACK_INSTANCE_TYPE_ID
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
                        if (instanceIndex != -1) {
                            zstackParamValue = zstackParamValue + "-" + instanceIndex
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
                    getTag = { String ecsParamValue ->
                        if (StringUtils.isBlank(ecsParamValue)) {
                            return
                        }
                        String l3NetworkUuid = ecsAPIParamMap[ECS_VPC_VSWITCH_ID]
                        return VmSystemTags.STATIC_IP.instantiateTag([
                                (VmSystemTags.STATIC_IP_L3_UUID_TOKEN): l3NetworkUuid,
                                (VmSystemTags.STATIC_IP_TOKEN)        : ecsParamValue
                        ])
                    }
                }

                systemTagConvert {
                    ecsParamName = ECS_USERDATA
                    getTag = { String ecsParamValue ->
                        if (StringUtils.isNotBlank(ecsParamValue)) {
                            return VmSystemTags.USERDATA.instantiateTag([(VmSystemTags.USERDATA_TOKEN): ecsParamValue])
                        }
                    }
                }

                systemTagConvert {
                    ecsParamName = ECS_DEPLOYMENT_SET_ID
                    getTag = { String ecsParamValue ->
                        return AffinityGroupSystemTags.AFFINITY_GROUP_UUID.instantiateTag([(AffinityGroupSystemTags.AFFINITY_GROUP_UUID_TOKEN): ecsParamValue])
                    }
                }

                systemTagConvert {
                    ecsParamName = ZSTACK_PCI_DEVICE_SPEC_UUID_KEY
                    getTag = { String pciSpecUuid ->
                        String amount = ecsAPIParamMap.get(ZSTACK_PCI_DEVICE_SPEC_AMOUNT_KEY)
                        String zoneId = ecsAPIParamMap.get(ECS_API_ZONEID_KEY)
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
                        zstackParamName = "dataDiskOfferingUuids"

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
                        String vswitchId = ecsParamMap.get(ECS_VPC_VSWITCH_ID)
                        zstackParamMap.put(ZSTACK_INSTANCE_DEFAULT_NETWORK, vswitchId)
                        List l3NetworkUuids = [vswitchId]
                        return l3NetworkUuids
                    }
                }

                zstackNeedParam {
                    zstackParamName = ZSTACK_RESOURCEUUID_KEY
                    getZstackValue = { Map ecsParamMap, Map zstackParamMap ->
                        return instanceUuid
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

        QuerySecurityGroupAction querySG = new QuerySecurityGroupAction(
                conditions: ["uuid=$sgUuid".toString()],
                sessionId: sessionId
        )
        QuerySecurityGroupAction.Result sgRes = querySG.call()

        if (sgRes.error != null || sgRes.value.inventories.isEmpty()) {
            throw new APIAdapterSpecifiedErrorException(SECURITY_GROUP_NOT_FOUND_CODE, SECURITY_GROUP_NOT_FOUND_MESSAGE)
        }

        SecurityGroupInventory inventory = sgRes.value.inventories.first()
        def existedSG = inventory.attachedL3NetworkUuids.find {
            it == vmInventory.defaultL3NetworkUuid
        }

        if (existedSG == null) {
            AttachSecurityGroupToL3NetworkAction attachSG = new AttachSecurityGroupToL3NetworkAction()
            attachSG.sessionId = sessionId
            attachSG.l3NetworkUuid = vmInventory.defaultL3NetworkUuid
            attachSG.securityGroupUuid = sgUuid

            AttachSecurityGroupToL3NetworkAction.Result attachSGRes = attachSG.call()
            if (attachSGRes.error != null) {
                // this is an internal error
                throw new APIParamConvertException(paramName, attachSGRes.error)
            }

        }

        AddVmNicToSecurityGroupAction sgAddNic = new AddVmNicToSecurityGroupAction()
        sgAddNic.vmNicUuids = vmInventory.vmNics.collect { it.uuid }
        sgAddNic.securityGroupUuid = sgUuid
        sgAddNic.sessionId = sessionId

        AddVmNicToSecurityGroupAction.Result sgAddNicRes = sgAddNic.call()
        if (sgAddNicRes.error != null) {
            throw new APIParamConvertException(paramName, sgAddNicRes.error)
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
            // this is an internal error
            throw new APIParamConvertException("SystemDisk.DiskName || SystemDisk.Description", result.error)
        }
    }

    private void setVmPublicNicBandWidth(VmInstanceInventory vmInventory) {
        //  vm does not have a public network nic.
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

    void setQGA(String instanceUuid) {
        if (instanceUuid == null) {
            return
        }
        SetVmQgaAction action = new SetVmQgaAction(
                sessionId: sessionId,
                uuid: instanceUuid,
                enable: true
        )
        action.call()
    }

    void setPassword(String instanceUuid, String password) {
        if (password == null || instanceUuid == null) {
            return
        }
        ChangeVmPasswordAction action = new ChangeVmPasswordAction(
                sessionId: sessionId,
                uuid: instanceUuid,
                password: password,
                account: "root"
        )
        action.call()
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

    private void convertAmount(Object zstackActionResult) {
        String amount = ecsAPIParamMap.get("Amount")
        if (amount == null) {
            return
        }
        int count = Integer.parseInt(amount)
        if (count <= 1) {
            return
        }

        String otherRequestId = ExternalAPIAdapterUtils.randomUUID()
        String sourceInstanceId = zstackActionResult.value.inventory.uuid
        String sourceName = zstackActionResult.value.inventory.name
        def names = []
        for (i in 2..count) {
            names.add("${sourceName}-${i - 1}".toString())
        }

        logger.debug("Request[apiId: ${requestId}] and request[apiId: ${otherRequestId}] are related.")
        CloneVmInstanceAction action = new CloneVmInstanceAction(
                sessionId: sessionId,
                apiId: otherRequestId,
                vmInstanceUuid: sourceInstanceId,
                names: names
        )

        CloneVmInstanceAction.Result result = action.call()
        result.throwExceptionIfError()

        String rootVolumeUuid = zstackActionResult.value.inventory.rootVolumeUuid
        List<VolumeInventory> inventories = zstackActionResult.value.inventory.allVolumes.grep {
            if (it.uuid != rootVolumeUuid) {
                return it
            }
        }
        if (inventories.size() == 0) {
            return
        }

        for (Object cloneVmInstanceInventory in result.value.result.inventories) {
            for (int i = 0; i < inventories.size(); i++) {
                VolumeInventory volumeInventory = inventories.get(i)
                String createDataVolumeRequestId = ExternalAPIAdapterUtils.randomUUID()
                String name = ecsAPIParamMap.get("DataDisk.${i + 1}.DiskName".toString())
                String description = ecsAPIParamMap.get("DataDisk.${i + 1}.Description".toString())
                logger.debug("Request[apiId: ${requestId}] and request[apiId: ${createDataVolumeRequestId}] are related.")
                CreateDataVolumeAction createDataVolumeAction = new CreateDataVolumeAction(
                        sessionId: sessionId,
                        apiId: createDataVolumeRequestId,
                        name: name != null ? name : volumeInventory.name,
                        description: description != null ? description : volumeInventory.description,
                        diskOfferingUuid: volumeInventory.diskOfferingUuid
                )

                CreateDataVolumeAction.Result createDataVolumeResult = createDataVolumeAction.call()
                createDataVolumeResult.throwExceptionIfError()

                String attachDataVolumeToVmRequestId = ExternalAPIAdapterUtils.randomUUID()
                logger.debug("Request[apiId: ${requestId}] and request[apiId: ${attachDataVolumeToVmRequestId}] are related.")
                AttachDataVolumeToVmAction attachDataVolumeToVmAction = new AttachDataVolumeToVmAction(
                        sessionId: sessionId,
                        apiId: attachDataVolumeToVmRequestId,
                        vmInstanceUuid: cloneVmInstanceInventory.inventory.uuid,
                        volumeUuid: createDataVolumeResult.value.inventory.uuid
                )

                AttachDataVolumeToVmAction.Result attachDataVolumeToVmResult = attachDataVolumeToVmAction.call()
                attachDataVolumeToVmResult.throwExceptionIfError()
            }
        }
    }

    @Override
    void afterCallZStackAction(Object zstackActionResult) {
        super.afterCallZStackAction(zstackActionResult)

        CreateVmInstanceAction.Result result = zstackActionResult
        VmInstanceInventory vmInventory = result.value.inventory

        addVmToSecurityGroup(vmInventory)

        updateVolumeNameAndDesc(vmInventory)

        setQGA(vmInventory.uuid)

        setPassword(vmInventory.uuid, ecsAPIParamMap.get(ECS_INSTANCE_PASSWORD) as String)

        //setVmPublicNicBandWidth(vmInventory)

        //convertAmount(zstackActionResult)
    }
}
