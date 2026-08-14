package org.zstack.pluginpremium.externalapiadapter.convert.param

import org.zstack.aliyunproxy.vpc.AliyunProxyVpcStatus
import org.zstack.appliancevm.ApplianceVmStatus
import org.zstack.header.vm.VmInstanceState
import org.zstack.network.service.lb.LoadBalancerVO
import org.zstack.pciDevice.PciDeviceSystemTags
import org.zstack.pluginpremium.externalapiadapter.EcsSystemTags
import org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterUtils
import org.zstack.pluginpremium.externalapiadapter.datatypes.SLBBackendServer
import org.zstack.pluginpremium.externalapiadapter.exception.APIParamConvertException
import org.zstack.sdk.*
import org.zstack.utils.data.SizeUnit
import org.zstack.utils.logging.CLogger
import org.zstack.utils.logging.CLoggerImpl

import java.util.stream.Collectors

import static org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants.*

/**
 * Created by lining on 2018/4/26.
 */
class ParameterConversionUtils {
    private static CLogger logger = CLoggerImpl.getLogger(ParameterConversionUtils.class)

    static String getDiskOffering(String sessionId, String requestId, String ecsParamName, long size) {
        QueryDiskOfferingAction query = new QueryDiskOfferingAction()
        String queryCondition = String.format("diskSize=%s", SizeUnit.GIGABYTE.toByte(size))
        query.conditions = [queryCondition, "state=Enabled"]
        query.sessionId = sessionId
        query.apiId = requestId
        QueryDiskOfferingAction.Result queryResult = query.call()

        if (queryResult.value.inventories && queryResult.value.inventories.size() > 0) {
            return queryResult.value.inventories[0].uuid
        }

	    String otherRequestId = ExternalAPIAdapterUtils.randomUUID()
	    logger.debug("Request[apiId: ${requestId}] and request[apiId: ${otherRequestId}] are related.".toString())
        CreateDiskOfferingAction action = new CreateDiskOfferingAction()
        action.name = size + "_GB"
        action.diskSize = SizeUnit.GIGABYTE.toByte(size)
        action.sessionId = sessionId
        action.apiId = otherRequestId

        CreateDiskOfferingAction.Result result = action.call()
        if (result.error != null) {
            throw new APIParamConvertException(ecsParamName, result.error)
        }

        return result.value.inventory.uuid
    }

    static Map getNetworkService(String sessionId) {
        QueryNetworkServiceProviderAction action = new QueryNetworkServiceProviderAction(
                sessionId: sessionId
        )
        QueryNetworkServiceProviderAction.Result result = action.call()
        result.throwExceptionIfError()

        def serviceMap = [:]
        for (NetworkServiceProviderInventory inventory in result.value.inventories) {
            serviceMap.put(inventory.type, inventory.uuid)
        }
        return serviceMap
    }

    static String getL3NetworkUuidFromVSwitch(String sessionId, String vSwitchId) {
        QueryAliyunProxyVSwitchAction action = new QueryAliyunProxyVSwitchAction(
                sessionId: sessionId,
                conditions: ["uuid=$vSwitchId".toString()]
        )
        QueryAliyunProxyVSwitchAction.Result result = action.call()
        result.throwExceptionIfError()

        List inventories = result.value.inventories
        if (inventories.size() == 0) {
            throw new APIParamConvertException(ECS_VPC_VSWITCH_ID, "Not found VSwitch[id: $vSwitchId]".toString())
        }

        return inventories.first().vpcL3NetworkUuid
    }

    static String convertZoneId(String sessionId, String zoneId) {
        QueryClusterAction query = new QueryClusterAction(
                sessionId: sessionId,
                conditions: ["name=${zoneId}".toString()]
        )
        QueryClusterAction.Result result = query.call()
        result.throwExceptionIfError()

        if (result.value.inventories.size() > 0) {
            return result.value.inventories.first().uuid
        }

        throw new APIParamConvertException(ECS_API_ZONEID_KEY, "Not found Zone[zoneId: $zoneId]".toString())
    }

    static String convertInstanceType(String sessionId, String instanceTypeId) {
        QueryInstanceOfferingAction query = new QueryInstanceOfferingAction(
                sessionId: sessionId,
                conditions: ["name=${instanceTypeId}".toString()]
        )
        QueryInstanceOfferingAction.Result result = query.call()
        result.throwExceptionIfError()

        if (result.value.inventories.size() > 0) {
            return result.value.inventories.first().uuid
        }

        throw new APIParamConvertException(ECS_INSTANCE_TYPE_ID, "Not found InstanceType[instanceTypeId: $instanceTypeId]".toString())
    }

    static Map getGPUSpecInfoMapByName(String sessionId, String instanceTypeId) {
        return getGPUSpecInfoMap(sessionId, instanceTypeId, false)
    }

    static Map getGPUSpecInfoMapByUuid(String sessionId, String instanceOfferingUuid) {
        return getGPUSpecInfoMap(sessionId, instanceOfferingUuid, true)
    }

    private static Map getGPUSpecInfoMap(String sessionId, String id, boolean isUuid) {
        String instanceOfferingUuid = id
        if (!isUuid) {
            instanceOfferingUuid = convertInstanceType(sessionId, id)
        }

        QuerySystemTagAction tagAction = new QuerySystemTagAction(
                sessionId: sessionId,
                conditions: [
                        "resourceType=InstanceOfferingVO",
                        "resourceUuid=$instanceOfferingUuid".toString()
                ]
        )
        QuerySystemTagAction.Result tagResult = tagAction.call()
        tagResult.throwExceptionIfError()
        SystemTagInventory pciTag = tagResult.value.inventories.find { tag ->
            EcsSystemTags.PCI_DEVICE_INFO.isMatch(tag.tag as String)
        }
        if (pciTag == null) {
            return null
        }

        return EcsSystemTags.PCI_DEVICE_INFO.getTokensByTag(pciTag.tag)
    }

    static String preCheckGPUSpec(String sessionId, Map infoTokens) {
        String deviceId = infoTokens.get(EcsSystemTags.PCI_DEVICE_INFO_DEVICEID_TOKEN)
        String vendorId = infoTokens.get(EcsSystemTags.PCI_DEVICE_INFO_VENDORID_TOKEN)
        String subDeviceId = infoTokens.get(EcsSystemTags.PCI_DEVICE_INFO_SUBDEVICEID_TOKEN)
        QueryPciDeviceSpecAction pciAction = new QueryPciDeviceSpecAction(
                sessionId: sessionId,
                conditions: [
                        "type?=GPU_Video_Controller,GPU_3D_Controller",
                        "deviceId=$deviceId".toString(),
                        "vendorId=$vendorId".toString(),
                        "subdeviceId=$subDeviceId".toString()
                ]
        )
        QueryPciDeviceSpecAction.Result pciResult = pciAction.call()
        if (pciResult.error != null || pciResult.value.inventories.size() == 0) {
            throw new APIParamConvertException(ECS_INSTANCE_TYPE_ID, "Cannot find required pci device (GPU) spec.")
        }
        return pciResult.value.inventories.first().uuid
    }

    static String getGPUSpecTag(String sessionId, String gpuSpecUuid, String zoneId, String amount) {
        List clusterUuids
        if (zoneId != null) {
            clusterUuids = new ArrayList()
            clusterUuids.add(convertZoneId(sessionId, zoneId))
        } else {
            QueryClusterAction clusterAction = new QueryClusterAction(
                    sessionId: sessionId,
            )
            QueryClusterAction.Result clusterResult = clusterAction.call()
            clusterUuids = clusterResult.value.inventories.stream().map { cluster ->
                cluster.uuid
            }.collect(Collectors.toList())
        }

        GetPciDeviceSpecCandidatesAction pciCandidatesAction = new GetPciDeviceSpecCandidatesAction(
                sessionId: sessionId,
                types: [
                        "GPU_Video_Controller",
                        "GPU_3D_Controller"
                ],
                clusterUuids: clusterUuids
        )
        GetPciDeviceSpecCandidatesAction.Result pciCandidatesResult = pciCandidatesAction.call()
        if (pciCandidatesResult.error != null || pciCandidatesResult.value == null || pciCandidatesResult.value.inventories == null) {
            throw new APIParamConvertException(ECS_INSTANCE_TYPE, "cannot find available pci device (GPU) spec at this time.")
        }
        PciDeviceSpecInventory pciCandidate = pciCandidatesResult.value.inventories.stream().filter { PciDeviceSpecInventory res ->
            res.uuid == gpuSpecUuid
        }.findAny().orElseThrow { ->
            new APIParamConvertException(ECS_INSTANCE_TYPE, "cannot find available pci device (GPU) spec at this time.")
        } as PciDeviceSpecInventory

        return PciDeviceSystemTags.PCI_DEVICE_SPEC.instantiateTag([
                (PciDeviceSystemTags.PCI_DEVICE_SPEC_UUID_TOKEN): (pciCandidate.uuid),
                (PciDeviceSystemTags.PCI_DEVICE_NUMBER_TOKEN)   : (amount)
        ])
    }

    static List getSLBBackendServersInfoFromTags(String sessionId, List systemTags) {
        List<SystemTagInventory> extTags = systemTags.stream().filter { SystemTagInventory tag ->
            EcsSystemTags.SLB_BACKEND_SERVER_EXT.isMatch(tag.getTag())
        }.collect(Collectors.toList()) as List<SystemTagInventory>

        List<SLBBackendServer> res = new ArrayList<>()
        List<String> residualTagUuids = new ArrayList<>()

        for (SystemTagInventory tag in extTags) {
            Map tokens = EcsSystemTags.SLB_BACKEND_SERVER_EXT.getTokensByTag(tag.getTag())
            SLBBackendServer server = new SLBBackendServer()
            server.vmInstanceId = tokens[EcsSystemTags.SLB_BACKEND_SERVER_TOKEN]
            server.vmNicId = tokens[EcsSystemTags.SLB_BACKEND_NIC_TOKEN]
            server.type = tokens[EcsSystemTags.SLB_BACKEND_TYPE_TOKEN]
            server.weight = Integer.valueOf(tokens[EcsSystemTags.SLB_BACKEND_WEIGHT_TOKEN] as String)
            server.serverId = server.type == ECS_SLB_BACKEND_SERVER_TYPE_ECS ? server.vmInstanceId : server.vmNicId

            if (server.type == "ecs") {
                QueryVmInstanceAction checkVmAction = new QueryVmInstanceAction(
                        sessionId: sessionId,
                        conditions: ["uuid=$server.vmInstanceId".toString()]
                )
                QueryVmInstanceAction.Result checkVmResult = checkVmAction.call()
                List vm = checkVmResult.value.inventories
                if (vm == null || vm.size() == 0) {
                    residualTagUuids.add(tag.uuid)
                    continue
                }
            }

            QueryVmNicAction checkNicAction = new QueryVmNicAction(
                    sessionId: sessionId,
                    conditions: ["uuid=$server.vmNicId".toString()]
            )
            QueryVmNicAction.Result checkNicResult = checkNicAction.call()
            List nic = checkNicResult.value.inventories
            if (nic == null || nic.size() == 0) {
                residualTagUuids.add(tag.uuid)
                continue
            }

            res.add(server)
        }

        List<SLBBackendServer> oldServers = processObsoleteBackendServerTags(sessionId, systemTags) as List<SLBBackendServer>
        if (!(oldServers == null || oldServers.size() == 0)) {
            res.addAll(oldServers)
        }

        residualTagUuids.each { String tagUuid ->
            DeleteTagAction action = new DeleteTagAction(
                    sessionId: sessionId,
                    uuid: tagUuid
            )
            action.call()
        }

        return res
    }

    private static List processObsoleteBackendServerTags(String sessionId, List systemTags) {
        List obsoleteTags = systemTags.stream().filter { SystemTagInventory tag ->
            EcsSystemTags.SLB_BACKEND_SERVER.isMatch(tag.getTag())
        }.collect(Collectors.toList())

        if (obsoleteTags == null || obsoleteTags.size() == 0) {
            return null
        }

        List ret = new ArrayList<>()
        DeleteTagAction deleteTagAction = new DeleteTagAction(
                sessionId: sessionId,
        )
        CreateSystemTagAction createSystemTagAction = new CreateSystemTagAction(
                sessionId: sessionId,
                resourceType: LoadBalancerVO.class.getSimpleName(),
        )

        for (SystemTagInventory tag : obsoleteTags) {
            Map tokens = EcsSystemTags.SLB_BACKEND_SERVER.getTokensByTag(tag.getTag())
            SLBBackendServer server = new SLBBackendServer()
            ret.add(server)
            server.vmInstanceId = tokens[EcsSystemTags.SLB_BACKEND_SERVER_TOKEN]
            server.vmNicId = tokens[EcsSystemTags.SLB_BACKEND_NIC_TOKEN]
            server.type = ECS_SLB_BACKEND_SERVER_TYPE_ECS
            server.weight = 100

            createSystemTagAction.resourceUuid = tag.resourceUuid
            createSystemTagAction.tag = EcsSystemTags.SLB_BACKEND_SERVER_EXT.instantiateTag(
                    [
                            (EcsSystemTags.SLB_BACKEND_SERVER_TOKEN): server.vmInstanceId,
                            (EcsSystemTags.SLB_BACKEND_NIC_TOKEN)   : server.vmNicId,
                            (EcsSystemTags.SLB_BACKEND_TYPE_TOKEN)  : server.type,
                            (EcsSystemTags.SLB_BACKEND_WEIGHT_TOKEN): server.weight
                    ]
            )
            CreateSystemTagAction.Result creationResult = createSystemTagAction.call()
            if (creationResult.error == null) {
                deleteTagAction.uuid = tag.uuid
                deleteTagAction.call()
            }
        }
        return ret
    }

    static processHttpListener(Map ecsParamMap, Map zstackParamMap) {
        String uri = ecsParamMap[ECS_SLB_LISTENER_HEALTH_CHECK_URI]
        String code = ecsParamMap[ECS_SLB_LISTENER_HEALTH_CHECK_HTTP_CODE]
        if (uri != null) {
            zstackParamMap[ZSTACK_SLB_LISTENER_HEALTH_CHECK_URI] = uri
        } else {
            throw new APIParamConvertException(ECS_SLB_LISTENER_HEALTH_CHECK_URI, "health check uri is required if use http health check.")
        }
        if (code == null) {
            code = "http_2xx"
        }

        zstackParamMap[ZSTACK_SLB_LISTENER_HEALTH_CHECK_HTTP_CODE] = code
        zstackParamMap[ZSTACK_SLB_LISTENER_HEALTH_CHECK_METHOD] = ZSTACK_SLB_LISTENER_HEALTH_CHECK_METHOD_HEAD
    }

    static convertVPCRsp(Map ecsAPIRsp, String vRouterUuid, String sessionId) {
        QueryVpcRouterAction qVRAct
        QueryVpcRouterAction.Result vrRes
        if (vRouterUuid != null) {
            qVRAct = new QueryVpcRouterAction(
                    sessionId: sessionId,
                    conditions: ["uuid=${vRouterUuid}".toString()]
            )
            vrRes = qVRAct.call()
        }

        if (vRouterUuid == null || vrRes == null || vrRes.error != null || vrRes.value.inventories.isEmpty()) {
            ecsAPIRsp[ECS_API_STATUS_KEY] = AliyunProxyVpcStatus.Pending.toString()
            ecsAPIRsp[ECS_VPC_VROUTER_ID] = ""
            ecsAPIRsp[ECS_API_REGIONID_KEY] = ""
            ecsAPIRsp[ECS_API_ZONEID_KEY] = ""
            return
        }

        VpcRouterVmInventory vr = vrRes.value.inventories.first()

        QueryZoneAction qRegAct = new QueryZoneAction(
                sessionId: sessionId,
                conditions: ["uuid=${vr.zoneUuid}".toString()]
        )
        QueryZoneAction.Result regionRes = qRegAct.call()
        if (regionRes.error == null && !regionRes.value.inventories.isEmpty()) {
            ZoneInventory region = regionRes.value.inventories.first()
            ecsAPIRsp[ECS_API_REGIONID_KEY] = region.name
        } else {
            ecsAPIRsp[ECS_API_REGIONID_KEY] = ""
        }

        QueryClusterAction qZoneAct = new QueryClusterAction(
                sessionId: sessionId,
                conditions: ["uuid=${vr.clusterUuid}".toString()]
        )
        QueryClusterAction.Result zoneRes = qZoneAct.call()
        if (zoneRes.error == null && !zoneRes.value.inventories.isEmpty()) {
            ClusterInventory zone = zoneRes.value.inventories.first()
            ecsAPIRsp[ECS_API_ZONEID_KEY] = zone.name
        } else {
            ecsAPIRsp[ECS_API_ZONEID_KEY] = ""
        }

        ecsAPIRsp[ECS_VPC_VROUTER_ID] = vRouterUuid
        if (vr.state == VmInstanceState.Running.toString() || vr.status == ApplianceVmStatus.Connected.toString()) {
            ecsAPIRsp[ECS_API_STATUS_KEY] = AliyunProxyVpcStatus.Available.toString()
        } else {
            ecsAPIRsp[ECS_API_STATUS_KEY] = AliyunProxyVpcStatus.Pending.toString()
        }
    }
}
