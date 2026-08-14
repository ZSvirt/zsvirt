package org.zstack.pluginpremium.externalapiadapter.api.ecs.vpc

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.apache.commons.lang.StringUtils
import org.zstack.aliyunproxy.vpc.AliyunProxyVSwitchStatus
import org.zstack.header.network.service.NetworkServiceType
import org.zstack.header.vipQos.VipQosConstants
import org.zstack.header.vpc.VpcConstants
import org.zstack.ipsec.IPsecConstants
import org.zstack.network.securitygroup.SecurityGroupConstant
import org.zstack.network.service.eip.EipConstant
import org.zstack.network.service.lb.LoadBalancerConstants
import org.zstack.network.service.portforwarding.PortForwardingConstant
import org.zstack.network.service.userdata.UserdataConstant
import org.zstack.pluginpremium.externalapiadapter.EcsSystemTags
import org.zstack.pluginpremium.externalapiadapter.api.BaseAPI
import org.zstack.pluginpremium.externalapiadapter.exception.APIAdapterSpecifiedErrorException
import org.zstack.sdk.*
import org.zstack.utils.gson.JSONObjectUtil
import org.zstack.utils.network.NetworkUtils
import org.zstack.vrouterRoute.VRouterRouteConstants

import java.util.stream.Collectors

import static org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants.*
import static org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants.ECSErrorCode.InvalidParameter
import static org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterGlobalProperty.*
/**
 * Created by lining on 2018/5/28.
 */

class CreateVSwitch extends BaseAPI {
    private static final String VPC_NOT_FOUND_CODE = "InvalidVpcId.NotFound"
    private static final String VPC_NOT_FOUND_MESSAGE = "Specified VPC does not exist."
    private static final String NOT_SUB_CIDR_MESSAGE = "Specified CIDR block is not valid in VPC."
    private static final String OVERLAP_CIDR_CODE = "InvalidCidrBlock.Overlapped"
    private static final String OVERLAP_CIDR_MESSAGE = "Specified CIDR block overlapped with other subnets."
    private static final String BAD_VPC_STATUS_CODE = "IncorrectVpcStatus"
    private static final String BAD_VPC_STATUS_MESSAGE = "Current VPC status does not support this operation."

    @Override
    Class getZStackAction() {
        return CreateAliyunProxyVSwitchAction.class
    }

    @Override
    protected void configAPIConversionSpec() {
        spec = config {
            convertAPIParam {
                simpleConvert {
                    ecsParamName = ECS_VPC_VPC_ID
                    zstackParamName = "aliyunProxyVpcUuid"
                }

                simpleConvert {
                    ecsParamName = "CidrBlock"
                    zstackParamName = "cidrBlock"
                }

                simpleConvert {
                    ecsParamName = ECS_VPC_VSWITCH_NAME
                    zstackParamName = "l3NetworkName"
                }

                simpleConvert {
                    ecsParamName = ECS_API_DESCRIPTION_KEY
                    zstackParamName = "l3NetworkDescription"
                }
            }

            convertAPIResponse {
                convertResponseAttribute {
                    ecsAttributeName = ECS_VPC_VSWITCH_ID

                    getZstackAttributeValue = {
                        return zstackAPIRsp.value.inventory.uuid
                    }

                    addEcsValueToEcsAPIRsp = {ecsAPIRsp ->
                        ecsAPIRsp.put(ecsAttributeName, zstackAttributeValue)
                    }
                }
            }
        }
    }

    @Override
    Object callZStackAction() {
        Gson gson = new GsonBuilder().create()
        CreateAliyunProxyVSwitchAction action = gson.fromJson(JSONObjectUtil.toJsonString(zstackAPIParamMap), this.getZStackAction())

        //do some check first
        String vpcUuid = action.aliyunProxyVpcUuid
        QueryAliyunProxyVpcAction queryVpc = new QueryAliyunProxyVpcAction(
                sessionId: sessionId,
                conditions: ["uuid=${vpcUuid}".toString()]
        )
        QueryAliyunProxyVpcAction.Result vpcResult = queryVpc.call()
        if (vpcResult.error != null || vpcResult.value.inventories.isEmpty()) {
            throw new APIAdapterSpecifiedErrorException(VPC_NOT_FOUND_CODE, VPC_NOT_FOUND_MESSAGE)
        }

        AliyunProxyVpcInventory proxyVpc = vpcResult.value.inventories.first()
        String vpcCidr = proxyVpc.cidrBlock
        String vsCidr = ecsAPIParamMap["CidrBlock"]
        if (!NetworkUtils.isSubCidr(vpcCidr, vsCidr)) {
            throw new APIAdapterSpecifiedErrorException(InvalidParameter, NOT_SUB_CIDR_MESSAGE)
        }

        checkCidrConflict(vpcUuid, vsCidr)

        String vpcVrUuid = proxyVpc.vRouterUuid
        if (vpcVrUuid == null) {
            throw new APIAdapterSpecifiedErrorException(BAD_VPC_STATUS_CODE, BAD_VPC_STATUS_MESSAGE)
        }
        QueryApplianceVmAction queryVRouterAction = new QueryApplianceVmAction(
                sessionId: sessionId,
                conditions: ["uuid=$vpcVrUuid".toString()]
        )
        QueryApplianceVmAction.Result vrtRes = queryVRouterAction.call()
        vrtRes.throwExceptionIfError()
        if (vrtRes.error != null || vrtRes.value.inventories.isEmpty()) {
            throw new APIAdapterSpecifiedErrorException(BAD_VPC_STATUS_CODE, BAD_VPC_STATUS_MESSAGE)
        }
        ApplianceVmInventory vRouter = vrtRes.value.inventories.first()
        if (vRouter == null || vRouter.status != "Connected" || vRouter.state != "Running") {
            throw new APIAdapterSpecifiedErrorException(BAD_VPC_STATUS_CODE, BAD_VPC_STATUS_MESSAGE)
        }

        //create l3 network for vs
        String vpcL3Uuid = createVpcL3Network(ecsAPIParamMap)
        action.vpcL3NetworkUuid = vpcL3Uuid

        //create proxy vswitch
        CreateAliyunProxyVSwitchAction.Result result
        result = action.call()
        result.throwExceptionIfError()

        AttachL3NetworkToVmAction attachVpcL3NetworkToVpcVrAction = new AttachL3NetworkToVmAction(
                sessionId: sessionId,
                vmInstanceUuid: vpcVrUuid,
                l3NetworkUuid: action.vpcL3NetworkUuid
        )
        AttachL3NetworkToVmAction.Result attachL3Result
        attachVpcL3NetworkToVpcVrAction.call(new Completion<AttachL3NetworkToVmAction.Result>() {
            @Override
            void complete(AttachL3NetworkToVmAction.Result ret) {
                try {
                    attachL3Result = ret
                    attachL3Result.throwExceptionIfError()
                    afterCallZStackAction(result)
                } catch (Throwable t) {
                    logger.error("[RequestId:${requestId}] Async API has an internal error".toString(), t)
                    t.printStackTrace()
                    throw t
                }
            }
        })

        if (attachL3Result != null) {
            attachL3Result.throwExceptionIfError()
        }

        return result
    }

    @Override
    void afterCallZStackAction(Object ZStackActionResult) {
        CreateAliyunProxyVSwitchAction.Result result = ZStackActionResult
        AliyunProxyVSwitchInventory vSwitchInv = result.value.inventory
        List routeTables = []
        List securityGroups = []
        String tagUuid = fetchSystemTags(vSwitchInv, routeTables, securityGroups)
        attachRouteTable(vSwitchInv.aliyunProxyVpcUuid, routeTables, tagUuid)
        attachSecurityGroup(vSwitchInv, securityGroups)
        setVSwitchAvailable(vSwitchInv)
    }

    String fetchSystemTags(AliyunProxyVSwitchInventory vSwitchInv, List routeTables, List securityGroups) {
        String routeTableTagUuid
        QuerySystemTagAction querySystemTag = new QuerySystemTagAction(
                sessionId: sessionId,
                conditions: ["resourceUuid=${vSwitchInv.aliyunProxyVpcUuid}".toString()]
        )
        QuerySystemTagAction.Result systemTagResult = querySystemTag.call()
        systemTagResult.throwExceptionIfError()
        List systemTags = systemTagResult.value.inventories
        systemTags.forEach { SystemTagInventory it ->
            if (EcsSystemTags.DEFAULT_ROUTE_TABLE.isMatch(it.tag)) {
                routeTableTagUuid = it.uuid
                routeTables.add(EcsSystemTags.DEFAULT_ROUTE_TABLE.getTokenByTag(it.tag, EcsSystemTags.DEFAULT_ROUTE_TABLE_TOKEN))
            } else if (EcsSystemTags.VPC_SECURITYGROUP_ID.isMatch(it.tag)) {
                securityGroups.add(EcsSystemTags.VPC_SECURITYGROUP_ID.getTokenByTag(it.tag, EcsSystemTags.SECURITYGROUP_ID_TOKEN))
            }
        }
        return routeTableTagUuid
    }

    void attachRouteTable(String vpcId, List routeTables, String tagUuid) {
        if (routeTables.isEmpty()) {
            return
        }
        logger.debug("Attach default route table[id:${routeTables.first()}] to vpc[${vpcId}] due to this is the first vSwitch".toString())

        QueryAliyunProxyVpcAction queryVpc = new QueryAliyunProxyVpcAction(
                sessionId: sessionId,
                conditions: ["uuid=${vpcId}".toString()]
        )
        QueryAliyunProxyVpcAction.Result vpcResult = queryVpc.call()
        if (vpcResult.error != null || vpcResult.value.inventories.isEmpty()) {
            throw new APIAdapterSpecifiedErrorException(VPC_NOT_FOUND_CODE, VPC_NOT_FOUND_MESSAGE)
        }
        AliyunProxyVpcInventory vpcInventory = vpcResult.value.inventories.first()

        //we should have only one route table for each vpc
        AttachVRouterRouteTableToVRouterAction attachRouteTable = new AttachVRouterRouteTableToVRouterAction(
                sessionId: sessionId,
                virtualRouterVmUuid: vpcInventory.vRouterUuid,
                routeTableUuid: routeTables.first()
        )
        AttachVRouterRouteTableToVRouterAction.Result attachResult = attachRouteTable.call()
        attachResult.throwExceptionIfError()

        DeleteTagAction deleteTag = new DeleteTagAction(
                sessionId: sessionId,
                uuid: tagUuid
        )
        deleteTag.call()
    }

    void attachSecurityGroup(AliyunProxyVSwitchInventory vSwitchInv, List securityGroups) {
        if (securityGroups.isEmpty()) {
            logger.warn("there are no security group found for Vpc[id:${vSwitchInv.aliyunProxyVpcUuid}], something maybe went wrong".toString())
            return
        }

        List errors = []

        securityGroups.forEach { String it ->
            AttachSecurityGroupToL3NetworkAction attachSecurityGroup = new AttachSecurityGroupToL3NetworkAction(
                    sessionId: sessionId,
                    securityGroupUuid: it,
                    l3NetworkUuid: vSwitchInv.vpcL3NetworkUuid
            )

            AttachSecurityGroupToL3NetworkAction.Result result = attachSecurityGroup.call()
            if (result.error != null) {
                errors.add(result.error)
            }
        }
        //todo handle errors
//        if (errors.size() != 0) {}
    }

    String createVpcL3Network(Map params) {
        String name = params[ECS_VPC_VSWITCH_NAME]
        String description = params[ECS_API_DESCRIPTION_KEY]
        String cidrBlock = params["CidrBlock"]
        name = name ?: "vs_" + cidrBlock

        CreateL2VxlanNetworkAction createL2VxlanNetworkAction = new CreateL2VxlanNetworkAction(
                sessionId: sessionId,
                poolUuid: VXLAN_POOL_UUID,
                name: "Vxlan-L2-create-by-apiAdapter-for-${name}".toString(),
        )
        CreateL2VxlanNetworkAction.Result l2VxlanNetworkResult = createL2VxlanNetworkAction.call()
        l2VxlanNetworkResult.throwExceptionIfError()

        String l2Uuid = l2VxlanNetworkResult.value.inventory.uuid
        CreateL3NetworkAction createL3NetworkAction = new CreateL3NetworkAction(
                sessionId: sessionId,
                name: name,
                description: description,
                l2NetworkUuid: l2Uuid,
                type: VpcConstants.VPC_L3_NETWORK_TYPE
        )
        CreateL3NetworkAction.Result vpcL3Result = createL3NetworkAction.call()
        vpcL3Result.throwExceptionIfError()

        String vpcL3Uuid = vpcL3Result.value.inventory.uuid
        AddIpRangeByNetworkCidrAction addIpRangeByNetworkCidrAction = new AddIpRangeByNetworkCidrAction(
                sessionId: sessionId,
                name: "IpRange-create-by-apiAdapter",
                l3NetworkUuid: vpcL3Uuid,
                networkCidr: cidrBlock,
        )
        AddIpRangeByNetworkCidrAction.Result vpcL3IpRangeResult = addIpRangeByNetworkCidrAction.call()
        vpcL3IpRangeResult.throwExceptionIfError()

        AttachNetworkServiceToL3NetworkAction attachNetworkServiceToL3NetworkAction = new AttachNetworkServiceToL3NetworkAction(
                sessionId: sessionId,
                l3NetworkUuid: vpcL3Uuid,
                networkServices: [
                        (SECURITYGROUP_SERVICE_UUID): [
                                SecurityGroupConstant.SECURITY_GROUP_NETWORK_SERVICE_TYPE
                        ],
                        (VROUTER_SERVICE_UUID)      : [
                                NetworkServiceType.DNS.toString(),
                                NetworkServiceType.SNAT.toString(),
                                NetworkServiceType.Centralized_DNS.toString(),
                                VRouterRouteConstants.VROUTER_ROUTE_NETWORK_SERVICE_TYPE.toString(),
                                LoadBalancerConstants.LB_NETWORK_SERVICE_TYPE.toString(),
                                PortForwardingConstant.PORTFORWARDING_NETWORK_SERVICE_TYPE,
                                IPsecConstants.IPSEC_NETWORK_SERVICE_TYPE.toString(),
                                EipConstant.EIP_NETWORK_SERVICE_TYPE,
                                VipQosConstants.VIPQOS_NETWORK_SERVICE_TYPE.toString(),
                        ],
                        (FLAT_SERVICE_UUID)         : [
                                UserdataConstant.USERDATA_TYPE.toString(),
                                NetworkServiceType.DHCP.toString()
                        ]
                ]
        )
        AttachNetworkServiceToL3NetworkAction.Result attachNetworkServiceToL3NetworkResult = attachNetworkServiceToL3NetworkAction.call()
        attachNetworkServiceToL3NetworkResult.throwExceptionIfError()

        return vpcL3Uuid
    }

    void setVSwitchAvailable(AliyunProxyVSwitchInventory vSwitchInventory) {
        UpdateAliyunProxyVSwitchAction updateVSwitchAction = new UpdateAliyunProxyVSwitchAction(
                sessionId: sessionId,
                uuid: vSwitchInventory.uuid,
                status: AliyunProxyVSwitchStatus.Available.toString()
        )
        UpdateAliyunProxyVSwitchAction.Result updateVSwitchResult = updateVSwitchAction.call()
        if (updateVSwitchResult.error != null) {
            logger.warn("Set vswitch status failed. details: ${updateVSwitchResult.error.description}")
        }
    }

    void checkCidrConflict(String vpcUuid, String vsCidr) {
        QueryAliyunProxyVSwitchAction queryProxyVSAction = new QueryAliyunProxyVSwitchAction(
                sessionId: sessionId,
                conditions: ["aliyunProxyVpcUuid=$vpcUuid".toString()]
        )
        QueryAliyunProxyVSwitchAction.Result queryVSResult = queryProxyVSAction.call()
        queryVSResult.throwExceptionIfError()
        if (queryVSResult.value.inventories.isEmpty()) {
            return
        }
        List vss = queryVSResult.value.inventories
        List l3Ids = vss.stream().map({ vs -> vs.vpcL3NetworkUuid }).collect(Collectors.toList())
        if (l3Ids.size() == 0) {
            return
        }

        String qCondition = "uuid?=${StringUtils.join(l3Ids, ',')}".toString()

        QueryL3NetworkAction queryL3Action = new QueryL3NetworkAction(
                sessionId: sessionId,
                conditions: [qCondition]
        )
        QueryL3NetworkAction.Result l3Result = queryL3Action.call()
        l3Result.throwExceptionIfError()
        if (l3Result.value.inventories.isEmpty()) {
            return
        }
        List l3Nets = l3Result.value.inventories
        List cidrs = l3Nets.stream()
                .flatMap({ l3 -> l3.ipRanges.stream() })
                .map({ ipr -> ipr.networkCidr })
                .collect(Collectors.toList())
        for (String cidr : cidrs) {
            if (NetworkUtils.isCidrOverlap(cidr, vsCidr)) {
                throw new APIAdapterSpecifiedErrorException(OVERLAP_CIDR_CODE, OVERLAP_CIDR_MESSAGE)
            }
        }
    }
}
