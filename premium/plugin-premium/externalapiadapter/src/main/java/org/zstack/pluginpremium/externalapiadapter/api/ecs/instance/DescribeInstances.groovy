package org.zstack.pluginpremium.externalapiadapter.api.ecs.instance

import org.apache.commons.lang.StringUtils
import org.zstack.compute.vm.VmSystemTags
import org.zstack.header.network.l3.L3NetworkConstant
import org.zstack.header.vm.VmInstanceState
import org.zstack.header.vm.VmInstanceVO
import org.zstack.header.vpc.VpcConstants
import org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterGlobalProperty
import org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterUtils
import org.zstack.pluginpremium.externalapiadapter.api.BaseQueryAPI
import org.zstack.pluginpremium.externalapiadapter.convert.param.ParameterConversionUtils
import org.zstack.pluginpremium.externalapiadapter.exception.APIParamConvertException
import org.zstack.sdk.*
import org.zstack.sdk.zwatch.api.GetAuditDataAction
import org.zstack.utils.data.SizeUnit

import java.time.ZonedDateTime

import static org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants.*
/**
 * Created by lining on 2018/4/15.
 */
class DescribeInstances extends BaseQueryAPI {

    void configAPIConversionSpec() {
        spec = config {
            convertQueryAPIParam {
                beforeZstackAPIParam = { zstackParamMap ->
                    zstackParamMap.put(ZSTACK_QUERY_CONDITIONS_KEY, [])
                    zstackParamMap.put(ZSTACK_QUERY_REPLYWITHCOUNT_KEY, true)
                }

                querySimpleConvert {
                    ecsParamName = ECS_API_ZONEID_KEY
                    zstackParamName = ZSTACK_API_ZONEID_KEY

                    putZstackParamValue = { Map zstackParamMap, String zstackParamValue ->
                        String zoneId = ParameterConversionUtils.convertZoneId(sessionId, zstackParamValue)
                        List conditions = zstackParamMap.get(ZSTACK_QUERY_CONDITIONS_KEY) as List
                        conditions.add("$zstackParamName=$zoneId".toString())
                    }
                }

                querySimpleConvert {
                    ecsParamName = ECS_INSTANCE_NAME
                    zstackParamName = ZSTACK_NAME
                }

                querySimpleConvert {
                    ecsParamName = ECS_IMAGE_ID
                    zstackParamName = ZSTACK_IMAGE_ID
                }

                querySimpleConvert {
                    ecsParamName = ECS_API_STATUS_KEY
                    zstackParamName = ZSTACK_API_STATE_KEY
                    stillConvertParamWhenEcsParamValueIsNull = true
                    putZstackParamValue = { zstackParamMap, zstackParamValue ->
                        List conditions = zstackParamMap.get(ZSTACK_QUERY_CONDITIONS_KEY)

                        def validValue = ["Running", "Starting", "Stopping", "Stopped", VmInstanceState.Rebooting.toString()]
                        if (zstackParamValue == null){
                            conditions.add("${zstackParamName}?=${StringUtils.join(validValue, ",")}".toString())
                            return
                        }

                        def result = validValue.find { it == zstackParamValue }
                        if (result == null) {
                            throw new APIParamConvertException(ecsParamName,"Status[value: $zstackParamValue] is an invalid value".toString())
                        }
                        conditions.add("${zstackParamName}=${zstackParamValue}".toString())
                    }
                }

                querySimpleConvert {
                    ecsParamName = ECS_INSTANCE_IDS
                    ecsParamType = ArrayList.class
                    zstackParamName = ZSTACK_UUID
                    zstackParamType = String.class

                    putZstackParamValue = { zstackParamMap, zstackParamValue ->
                        List conditions = zstackParamMap.get(ZSTACK_QUERY_CONDITIONS_KEY)
                        String uuids = StringUtils.join(zstackParamValue, ",")
                        conditions.add("$zstackParamName?=$uuids".toString())
                    }
                }

                querySimpleConvert {
                    ecsParamName = ECS_SECURITY_GROUP_ID
                    zstackParamName = "vmNics.securityGroup.uuid"
                    putZstackParamValue = { zstackParamMap, zstackParamValue ->
                        List conditions = zstackParamMap.get(ZSTACK_QUERY_CONDITIONS_KEY)
                        conditions.add("${zstackParamName}=${zstackParamValue}".toString())
                    }
                }

                querySimpleConvert {
                    // include VpcId and VSwitchId
                    ecsParamName = ECS_VPC_VPC_ID
                    zstackParamName = "vmNics.l3NetworkUuid"
                    stillConvertParamWhenEcsParamValueIsNull = true
                    putZstackParamValue = { zstackParamMap, zstackParamValue ->
                        List conditions = zstackParamMap.get(ZSTACK_QUERY_CONDITIONS_KEY)

                        String vswitchId = ecsAPIParamMap.get(ECS_VPC_VSWITCH_ID)
                        if (vswitchId != null){
                            QueryAliyunProxyVSwitchAction queryAliyunProxyVSwitchAction = new QueryAliyunProxyVSwitchAction(
                                    sessionId: sessionId,
                                    conditions: ["uuid=$vswitchId".toString()]
                            )
                            QueryAliyunProxyVSwitchAction.Result queryAliyunProxyVSwitchResult = queryAliyunProxyVSwitchAction.call()
                            queryAliyunProxyVSwitchResult.throwExceptionIfError()

                            List vswitches = queryAliyunProxyVSwitchResult.value.inventories
                            if (vswitches.size() != 0) {
                                AliyunProxyVSwitchInventory vSwitchInventory = vswitches.get(0) as AliyunProxyVSwitchInventory
                                conditions.add("${zstackParamName}=${vSwitchInventory.getVpcL3NetworkUuid()}".toString())
                                return
                            }
                            logger.debug("Not found AliyunProxyVSwitch[uuid: $vswitchId]".toString())
                        }

                        if (zstackParamValue == null){
                            return
                        }

                        QueryAliyunProxyVpcAction action = new QueryAliyunProxyVpcAction(
                                sessionId: sessionId,
                                conditions: ["uuid=$zstackParamValue".toString()]
                        )

                        QueryAliyunProxyVpcAction.Result result = action.call()
                        result.throwExceptionIfError()

                        List inventories = result.value.inventories
                        if (inventories.size() == 0) {
                            logger.debug("Not found AliyunProxyVpc[uuid: $zstackParamValue]".toString())
                            return
                        }

                        AliyunProxyVpcInventory vpcInventory = inventories.get(0) as AliyunProxyVpcInventory
                        List<String> l3Uuids = vpcInventory.aliyunProxyVSwitches.collect {it.vpcL3NetworkUuid}
                        conditions.add("${zstackParamName}?=${StringUtils.join(l3Uuids, ",")}".toString())
                    }
                }

                querySimpleConvert {
                    ecsParamName = "InstanceNetworkType"
                    zstackParamName = "vmNics.l3Network.type"
                    putZstackParamValue = { zstackParamMap, zstackParamValue ->
                        List conditions = zstackParamMap.get(ZSTACK_QUERY_CONDITIONS_KEY)
                        if (zstackParamValue == ECS_NETWORK_TYPE_VPC) {
                            conditions.add("${zstackParamName}=${VpcConstants.VPC_L3_NETWORK_TYPE}".toString())
                            String privateIpsString = ecsAPIParamMap.get("PrivateIpAddresses")
                            if (privateIpsString == null) {
                                return
                            }

                            List privateIps = ExternalAPIAdapterUtils.changeValueType(privateIpsString, ArrayList.class)
                            if (privateIps == null || privateIps.size() == 0) {
                                return
                            }
                            conditions.add("vmNics.ip?=${StringUtils.join(privateIps, ",")}".toString())
                        } else if (zstackParamValue == ECS_NETWORK_TYPE_CLASSIC) {
                            conditions.add("${zstackParamName}=${L3NetworkConstant.L3_BASIC_NETWORK_TYPE}".toString())

                            String innerIpsString = ecsAPIParamMap.get("InnerIpAddresses")
                            List innerIps = null
                            String publicIpsString = ecsAPIParamMap.get("PublicIpAddresses")
                            List publicIps = null
                            if (innerIpsString != null) {
                                innerIps = ExternalAPIAdapterUtils.changeValueType(innerIpsString, ArrayList.class)
                            }
                            if (publicIpsString != null) {
                                publicIps = ExternalAPIAdapterUtils.changeValueType(publicIpsString, ArrayList.class)
                            }

                            if (innerIps != null && innerIps.size() > 0) {
                                conditions.add("vmNics.ip?=${StringUtils.join(innerIps, ",")}".toString())
                                conditions.add("vmNics.l3Network.category=Private")
                                return
                            }

                            if (publicIps != null && publicIps.size() > 0) {
                                conditions.add("vmNics.ip?=${StringUtils.join(publicIps, ",")}".toString())
                                conditions.add("vmNics.l3Network.category=Public")
                            }
                        } else {
                            logger.debug("Invalid InstanceNetworkType[value: $zstackParamValue]".toString())
                        }
                    }
                }

                zstackNeedParam {
                    zstackParamName = ZSTACK_API_TYPE_KEY

                    getZstackValue = { Map ecsParamMap, Map zstackParamMap ->
                        return null
                    }

                    putZstackParamValue = { Map zstackParamMap, Object zstackParamValue ->
                        List conditions = zstackParamMap.get(ZSTACK_QUERY_CONDITIONS_KEY) as List
                        conditions.add("$zstackParamName=UserVm".toString())
                    }
                }

                zstackNeedParam {
                    zstackParamName = "hypervisorType"

                    getZstackValue = { Map ecsParamMap, Map zstackParamMap ->
                        return null
                    }

                    putZstackParamValue = { Map zstackParamMap, Object zstackParamValue ->
                        List conditions = zstackParamMap.get(ZSTACK_QUERY_CONDITIONS_KEY) as List
                        conditions.add("$zstackParamName=KVM".toString())
                    }
                }
            }

            convertQueryAPIResponse {

                convertResponseAttribute {
                    ecsAttributeName = ECS_INSTANCES
                    ecsAttributeValue = new HashMap<>()

                    addEcsValueToEcsAPIRsp = { ecsAPIRsp ->
                        ecsAPIRsp.put(ecsAttributeName, ecsAttributeValue)
                    }

                    convertList {
                        ecsAttributeName = ECS_INSTANCE
                        ecsAttributeValue = new ArrayList<>()

                        getZstackAttributeValue = {
                            return zstackAPIRsp.value.inventories
                        }

                        getElementZstackValues = {
                            return zstackAttributeValue
                        }

                        addEcsValueToFather = { fatherValue ->
                            fatherValue.put(ecsAttributeName, ecsAttributeValue)
                        }

                        addListElement = { VmInstanceInventory vmInventory ->

                            addConvertResponseAttribute {
                                ecsAttributeValue = new HashMap<>()

                                addEcsValueToFather = { fatherValue ->
                                    fatherValue.add(ecsAttributeValue)
                                }

                                zstackAttributeValue = vmInventory

                                convertResponseAttribute {
                                    ecsAttributeName = ECS_INSTANCE_ID

                                    zstackAttributeValue = vmInventory.uuid

                                    addEcsValueToFather = { fatherValue ->
                                        fatherValue.put(ecsAttributeName, zstackAttributeValue)
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = "DeviceAvailable"

                                    addEcsValueToFather = { fatherValue ->
                                        fatherValue.put(ecsAttributeName, vmInventory.allVolumes == null || vmInventory.allVolumes.size() < 16)
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = "InnerIpAddress"

                                    def getValue = {
                                        VmNicInventory privateL3Nic = vmInventory.vmNics.find { it.l3NetworkUuid == ExternalAPIAdapterGlobalProperty.PRIVATEL3NETWORKUUID } as VmNicInventory
                                        if (privateL3Nic == null) {
                                            return [IpAddress : []]
                                        }

                                        return [IpAddress : [privateL3Nic.getIp()]]
                                    }

                                    addEcsValueToFather = { fatherValue ->
                                        fatherValue.put(ecsAttributeName, getValue())
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = ECS_DEPLOYMENT_SET_ID

                                    addEcsValueToFather = { fatherValue ->
                                        QueryAffinityGroupAction action = new QueryAffinityGroupAction(
                                                sessionId : sessionId,
                                                conditions: [
                                                        "usages.resourceUuid=$vmInventory.uuid".toString(),
                                                        "usages.resourceType=${VmInstanceVO.class.simpleName}".toString()
                                                ]
                                        )
                                        QueryAffinityGroupAction.Result result = action.call()
                                        result.throwExceptionIfError()

                                        if (result.value.inventories.size() == 0) {
                                            return
                                        }

                                        String deploymentSetId = result.value.inventories.get(0).uuid
                                        fatherValue.put(ecsAttributeName, deploymentSetId)
                                    }
                                }

	                            convertResponseAttribute {
                                    ecsAttributeName = ECS_EIP_ADDRESS

                                    addEcsValueToFather = { fatherValue ->
                                        QueryEipAction action = new QueryEipAction(
                                                sessionId: sessionId,
                                                conditions: ["vmNic.vmInstanceUuid=$vmInventory.uuid".toString()]
                                        )

                                        QueryEipAction.Result result = action.call()
                                        result.throwExceptionIfError()

                                        String eipIp = result.value.inventories.size() > 0 ? result.value.inventories.get(0).vipIp : ""
                                        if (eipIp == ""){
                                            fatherValue.put(ecsAttributeName, [:])
                                            return
                                        }

                                        GetVipQosAction getVipQosAction = new GetVipQosAction()
                                        getVipQosAction.sessionId = sessionId
                                        getVipQosAction.uuid = result.value.inventories.get(0).vipUuid

                                        GetVipQosAction.Result getVipQosResult = getVipQosAction.call()
                                        getVipQosResult.throwExceptionIfError()

                                        List inventories = getVipQosResult.value.inventories
                                        int bandWidth = inventories.size() > 0 ? SizeUnit.BYTE.toMegaByte(inventories.get(0).inboundBandwidth.toDouble()).toInteger() : 5

			                            fatherValue.put(ecsAttributeName, [
                                                (ECS_EIP_ALLOCATION_ID) : vmInventory.uuid,
                                                (ECS_NETWORK_IP_ADDRESS): eipIp,
                                                (ECS_NETWORK_BANDWIDTH) : bandWidth,
                                                "IsSupportUnassociate"  : true
                                        ])
		                            }
	                            }

                                convertResponseAttribute {
                                    ecsAttributeName = "PublicIpAddress"

                                    def getValue = {
                                        VmNicInventory publicL3Nic = vmInventory.vmNics.find { it.l3NetworkUuid == ExternalAPIAdapterGlobalProperty.PUBLICL3NETWORKUUID } as VmNicInventory
                                        if (publicL3Nic == null) {
                                            return null
                                        }

                                        return [IpAddress : [publicL3Nic.getIp()]]
                                    }

                                    addEcsValueToFather = { fatherValue ->
                                        fatherValue.put(ecsAttributeName, getValue())
                                    }
                                }

	                            // include InternetMaxBandwidthIn/InternetMaxBandwidthOut
                                convertResponseAttribute {
                                    ecsAttributeName = "InternetMaxBandwidthIn"

                                    def getValue = {
	                                    if (vmInventory.vmNics.size() == 0) {
		                                    return null
	                                    }

	                                    VmNicInventory nicInventory = vmInventory.vmNics.find {
		                                    it.l3NetworkUuid == vmInventory.defaultL3NetworkUuid
	                                    } as VmNicInventory

	                                    if (nicInventory == null){
		                                    return null
	                                    }

	                                    GetNicQosAction action = new GetNicQosAction(
			                                    sessionId: sessionId,
			                                    uuid: nicInventory.uuid
	                                    )
	                                    GetNicQosAction.Result result = action.call()
	                                    result.throwExceptionIfError()

	                                    return result.value
                                    }

	                                addEcsValueToFather = { fatherValue ->
		                                GetNicQosResult result = getValue()
		                                int bandwidthIn = result != null && result.inboundBandwidth != -1 ? SizeUnit.BYTE.toMegaByte(result.inboundBandwidth.toDouble()).toInteger() : 200
		                                int bandwidthOut = result != null && result.outboundBandwidth != -1 ? SizeUnit.BYTE.toMegaByte(result.outboundBandwidth.toDouble()).toInteger() : 100
		                                fatherValue.put(ecsAttributeName, bandwidthIn)
		                                fatherValue.put("InternetMaxBandwidthOut", bandwidthOut)
	                                }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = ECS_INSTANCE_NAME

                                    zstackAttributeValue = vmInventory.name

                                    addEcsValueToFather = { fatherValue ->
                                        fatherValue.put(ecsAttributeName, zstackAttributeValue)
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = ECS_API_DESCRIPTION_KEY

                                    zstackAttributeValue = vmInventory.description

                                    addEcsValueToFather = { fatherValue ->
                                        fatherValue.put(ecsAttributeName, zstackAttributeValue)
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = ECS_IMAGE_ID

                                    zstackAttributeValue = vmInventory.description

                                    addEcsValueToFather = { fatherValue ->
                                        fatherValue.put(ecsAttributeName, zstackAttributeValue)
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = ECS_API_ZONEID_KEY

                                    zstackAttributeValue = ecsAPIParamMap.get(ecsAttributeName)

                                    addEcsValueToFather = { fatherValue ->
                                        if (zstackAttributeValue == null) {
                                            QueryClusterAction clusterAction = new QueryClusterAction(
                                                    sessionId: sessionId,
                                                    conditions: ["uuid=$vmInventory.clusterUuid".toString()]
                                            )
                                            QueryClusterAction.Result clusterResult = clusterAction.call()
                                            if (clusterResult.error == null && clusterResult.value.inventories.size() != 0) {
                                                zstackAttributeValue = clusterResult.value.inventories.first().name
                                            } else {
                                                zstackAttributeValue = vmInventory.clusterUuid
                                            }
                                        }
                                        fatherValue.put(ecsAttributeName, zstackAttributeValue)
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = ECS_API_REGIONID_KEY

                                    addEcsValueToFather = { fatherValue ->
                                        fatherValue.put(ecsAttributeName, ecsAPIParamMap.get(ecsAttributeName))
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = "Cpu"

                                    zstackAttributeValue = vmInventory.cpuNum

                                    addEcsValueToFather = { fatherValue ->
                                        fatherValue.put(ecsAttributeName, zstackAttributeValue)
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = "Memory"

                                    zstackAttributeValue = SizeUnit.BYTE.toMegaByte(vmInventory.memorySize)

                                    addEcsValueToFather = { fatherValue ->
                                        fatherValue.put(ecsAttributeName, zstackAttributeValue)
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = ECS_INSTANCE_TYPE

                                    zstackAttributeValue = vmInventory.instanceOfferingUuid

                                    addEcsValueToFather = { fatherValue ->
                                        QueryInstanceOfferingAction action = new QueryInstanceOfferingAction(
                                                sessionId: sessionId,
                                                conditions: ["uuid=$zstackAttributeValue".toString()]
                                        )
                                        QueryInstanceOfferingAction.Result result = action.call()
                                        result.throwExceptionIfError()
                                        String type
                                        if (result.value.inventories.size() == 0) {
                                            type = zstackAttributeValue
                                        } else {
                                            type = result.value.inventories.first().name
                                        }
                                        fatherValue.put(ecsAttributeName, type)
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = ECS_API_STATUS_KEY

                                    zstackAttributeValue = vmInventory.state

                                    addEcsValueToFather = { fatherValue ->
                                        // todo
                                        if (VmInstanceState.Rebooting.toString() == zstackAttributeValue) {
                                            zstackAttributeValue = VmInstanceState.Starting.toString()
                                        } else if (VmInstanceState.Starting.toString() == zstackAttributeValue) {
                                            // background: In ecs, the vm is being created, the status is pending
                                            GetAuditDataAction action = new GetAuditDataAction(
                                                    sessionId: sessionId,
                                                    conditions: ["resourceUuid=${vmInventory.uuid}".toString()],
                                                    limit: 1
                                            )
                                            GetAuditDataAction.Result result = action.call()
                                            result.throwExceptionIfError()

                                            if (result.value.audits.size() == 0) {
                                                zstackAttributeValue = ECS_STATE_PENDING
                                            }
                                        }

                                        fatherValue.put(ecsAttributeName, zstackAttributeValue)
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = ECS_API_CREATION_TIME

                                    zstackAttributeValue = vmInventory.createDate

                                    addEcsValueToFather = { Map parentMap ->
                                        String timeStr = ExternalAPIAdapterUtils.formatIso8601Date(zstackAttributeValue)
                                        (parentMap[ecsAttributeName] = timeStr)
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = ECS_INSTANCE_HOSTNAME

                                    addEcsValueToFather = { fatherValue ->
                                        GetVmHostnameAction action = new GetVmHostnameAction()
                                        action.sessionId = sessionId
                                        action.uuid = vmInventory.uuid

                                        GetVmHostnameAction.Result result = action.call()
                                        if (result.error != null) {
                                            throw new APIParamConvertException(ecsAttributeName, result.error)
                                        }

                                        fatherValue.put(ecsAttributeName, result.value.hostname)
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = "SerialNumber"

                                    addEcsValueToFather = { fatherValue ->
//                                        String serial = VmSystemTags.VM_SYSTEM_SERIAL_NUMBER.getTokenByResourceUuid(vmInventory.uuid, VmSystemTags.VM_SYSTEM_SERIAL_NUMBER_TOKEN)
                                        QuerySystemTagAction qAction = new QuerySystemTagAction(
                                                sessionId: sessionId,
                                                conditions: ["resourceUuid=${vmInventory.uuid}".toString(),
                                                             "resourceType=${VmInstanceVO.getSimpleName()}".toString()]
                                        )
                                        QuerySystemTagAction.Result result = qAction.call()
                                        if (result.error != null) {
                                            throw new APIParamConvertException("SerialNumber", result.error)
                                        }
                                        def targetObject = result.value.inventories.grep { it.tag.indexOf("vmSystemSerialNumber::") >= 0 }

                                        if (targetObject.size() == 0) {
                                            fatherValue.put(ecsAttributeName, "")
                                            return
//                                            throw new APIParamConvertException("SerialNumber", "VM serial number not found")
                                        }
                                        String serial = VmSystemTags.VM_SYSTEM_SERIAL_NUMBER.getTokenByTag(targetObject.first().tag, VmSystemTags.VM_SYSTEM_SERIAL_NUMBER_TOKEN)
                                        fatherValue.put(ecsAttributeName, serial)
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = "ExpiredTime"

                                    zstackAttributeValue = vmInventory.createDate

                                    addEcsValueToFather = { Map parentMap ->
                                        ZonedDateTime dateTime = ExternalAPIAdapterUtils.convertDateTime(zstackAttributeValue)
                                        dateTime.plusYears(100)
                                        String timeStr = ExternalAPIAdapterUtils.formatZonedDateTime(dateTime)

                                        parentMap[ecsAttributeName] = timeStr
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = "OSName"

                                    zstackAttributeValue = vmInventory.platform

                                    addEcsValueToFather = { Map parentMap ->
                                        String osName = zstackAttributeValue
                                        String osType = "Linux"
                                        if ("Windows" == osName || "WindowsVirtio" == osName) {
                                            osType = "windows"
                                        } else if ("Paravirtualization" == osName || "Other" == osName) {
                                            osType = "other"
                                        }
                                        parentMap.put(ecsAttributeName, osName)
                                        parentMap.put("OSNameEn", osName)
                                        parentMap.put("OSType", osType)
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = ECS_IMAGE_ID

                                    zstackAttributeValue = vmInventory.imageUuid

                                    addEcsValueToFather = { fatherValue ->
                                        fatherValue.put(ecsAttributeName, zstackAttributeValue)
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = "SecurityGroupIds"

                                    zstackAttributeValue = vmInventory.vmNics

                                    addEcsValueToFather = { fatherValue ->
                                        QuerySecurityGroupAction action = new QuerySecurityGroupAction()
                                        action.sessionId = sessionId
                                        def nics = "vmNic.uuid?=${StringUtils.join(zstackAttributeValue.collect { it.uuid }, ",")}".toString()
                                        action.conditions = [nics]

                                        QuerySecurityGroupAction.Result result = action.call()
                                        if (result.error != null) {
                                            throw new APIParamConvertException(ecsAttributeName, result.error)
                                        }

                                        def securityGroupIds = result.value.inventories.collect {
                                            it.uuid
                                        }
                                        def value = [(ECS_SECURITY_GROUP_ID): securityGroupIds]
                                        fatherValue.put(ecsAttributeName, value)
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = "NetworkInterfaces"

                                    zstackAttributeValue = vmInventory

                                    addEcsValueToFather = { Map fatherValue ->
                                        List<VmNicInventory> allNics = zstackAttributeValue.vmNics

                                        def vmNics = []
                                        def vpcAttributes = [:]
                                        for (VmNicInventory vmNic : allNics) {
                                            vmNics.add([
                                                    "NetworkInterfaceId": vmNic.uuid,
                                                    (ECS_INSTANCE_PRIMARY_IP): vmNic.ip,
                                                    "MacAddress": vmNic.mac
                                            ])

                                            if (vpcAttributes.size() == 0) {
                                                QueryL3NetworkAction queryL3NetworkAction = new QueryL3NetworkAction(
                                                        sessionId: sessionId,
                                                        conditions: ["uuid=${vmNic.l3NetworkUuid}".toString()]
                                                )
                                                QueryL3NetworkAction.Result queryL3NetworkResult = queryL3NetworkAction.call()
                                                queryL3NetworkResult.throwExceptionIfError()
                                                List l3Inventories = queryL3NetworkResult.value.inventories
                                                if (l3Inventories.size() == 0) {
                                                    logger.debug("Not found L3Network[uuid: ${vmNic.l3NetworkUuid}], maybe it has been deleted".toString())
                                                    continue
                                                }

                                                L3NetworkInventory l3NetworkInventory = l3Inventories[0] as L3NetworkInventory
                                                if (VpcConstants.VPC_L3_NETWORK_TYPE != l3NetworkInventory.type) {
                                                    continue
                                                }

                                                QueryAliyunProxyVSwitchAction queryAliyunProxyVSwitchAction = new QueryAliyunProxyVSwitchAction(
                                                        sessionId: sessionId,
                                                        conditions: ["vpcL3NetworkUuid=$vmNic.l3NetworkUuid".toString()]
                                                )
                                                QueryAliyunProxyVSwitchAction.Result queryAliyunProxyVSwitchResult = queryAliyunProxyVSwitchAction.call()
                                                queryAliyunProxyVSwitchResult.throwExceptionIfError()

                                                List vswitches = queryAliyunProxyVSwitchResult.value.inventories
                                                if (vswitches.size() == 0) {
                                                    logger.debug("Not Found AliyunProxyVSwitch[vpcL3NetworkUuid: ${vmNic.l3NetworkUuid}]".toString())
                                                    continue
                                                }
                                                AliyunProxyVSwitchInventory aliyunProxyVSwitchInventory = vswitches.get(0) as AliyunProxyVSwitchInventory
                                                vpcAttributes = [
                                                        (ECS_VPC_VPC_ID): aliyunProxyVSwitchInventory.aliyunProxyVpcUuid,
                                                        (ECS_VPC_VSWITCH_ID): aliyunProxyVSwitchInventory.uuid,
                                                        (ECS_INSTANCE_PRIVATE_IP): [(ECS_NETWORK_IP_ADDRESS): [vmNic.ip]]
                                                ]
                                            }
                                        }

                                        fatherValue.put(ecsAttributeName, ["NetworkInterface": vmNics])
                                        fatherValue.put("VpcAttributes", vpcAttributes)
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = ECS_INSTANCE_NETWORK_TYPE

                                    zstackAttributeValue = vmInventory.defaultL3NetworkUuid

                                    addEcsValueToFather = {Map parentValue ->
                                        QueryL3NetworkAction l3NetworkAction = new QueryL3NetworkAction(
                                                sessionId: sessionId,
                                                conditions: ["uuid=$zstackAttributeValue".toString()]
                                        )
                                        QueryL3NetworkAction.Result l3NetworkResult = l3NetworkAction.call()
                                        l3NetworkResult.throwExceptionIfError()
                                        List<L3NetworkInventory> inventories = l3NetworkResult.value.inventories
                                        if (inventories.size() > 0 && VpcConstants.VPC_L3_NETWORK_TYPE != inventories[0].type) {
                                            parentValue[ecsAttributeName] = ECS_NETWORK_TYPE_CLASSIC
                                        } else {
                                            parentValue[ecsAttributeName] = ECS_NETWORK_TYPE_VPC
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    Class getZStackAction() {
        return QueryVmInstanceAction.class
    }

}
