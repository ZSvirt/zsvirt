package org.zstack.pluginpremium.externalapiadapter.api

import org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterGlobalConfig
import org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterGlobalProperty
import org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterUtils
import org.zstack.pluginpremium.externalapiadapter.ZStackSessionFactory
import org.zstack.pluginpremium.externalapiadapter.api.ecs.deploymentset.CreateDeploymentSet
import org.zstack.pluginpremium.externalapiadapter.api.ecs.deploymentset.DeleteDeploymentSet
import org.zstack.pluginpremium.externalapiadapter.api.ecs.deploymentset.DescribeDeploymentSets
import org.zstack.pluginpremium.externalapiadapter.api.ecs.deploymentset.ModifyDeploymentSetAttribute
import org.zstack.pluginpremium.externalapiadapter.api.ecs.disk.*
import org.zstack.pluginpremium.externalapiadapter.api.ecs.eip.*
import org.zstack.pluginpremium.externalapiadapter.api.ecs.image.CreateImage
import org.zstack.pluginpremium.externalapiadapter.api.ecs.image.DeleteImage
import org.zstack.pluginpremium.externalapiadapter.api.ecs.image.DescribeImages
import org.zstack.pluginpremium.externalapiadapter.api.ecs.image.ModifyImageSharePermission
import org.zstack.pluginpremium.externalapiadapter.api.ecs.instance.*
import org.zstack.pluginpremium.externalapiadapter.api.ecs.instance.networkinterface.*
import org.zstack.pluginpremium.externalapiadapter.api.ecs.networkinterface.AllocatePublicIpAddress
import org.zstack.pluginpremium.externalapiadapter.api.ecs.others.*
import org.zstack.pluginpremium.externalapiadapter.api.ecs.securitygroup.*
import org.zstack.pluginpremium.externalapiadapter.api.ecs.slb.*
import org.zstack.pluginpremium.externalapiadapter.api.ecs.snapshot.*
import org.zstack.pluginpremium.externalapiadapter.api.ecs.vpc.*
import org.zstack.pluginpremium.externalapiadapter.api.ecs.zone.DescribeAvailableResource
import org.zstack.pluginpremium.externalapiadapter.api.ecs.zone.DescribeZones
import org.zstack.pluginpremium.externalapiadapter.exception.*
import org.zstack.utils.Utils
import org.zstack.utils.logging.CLogger

import static org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants.*

/**
 * Created by lining on 2018/4/21.
 */
class APIAdapter {
    private final static CLogger logger = Utils.getLogger(APIAdapter.class)
    private static Map<String, Class> actionAPI = new HashMap<>()

    static {
        putAPI(CreateInstance.class)
        putAPI(DescribeInstances.class)
        putAPI(StartInstance.class)
        putAPI(StopInstance.class)
        putAPI(RebootInstance.class)
        putAPI(DeleteInstance.class)
        putAPI(ModifyInstanceAttribute.class)
        putAPI(CreateDisk.class)
        putAPI(DescribeDisks.class)
        putAPI(AttachDisk.class)
        putAPI(DetachDisk.class)
        putAPI(DeleteDisk.class)
        putAPI(ModifyDiskAttribute.class)
        putAPI(ReInitDisk.class)
        putAPI(ResetDisk.class)
        putAPI(ReplaceSystemDisk.class)
        putAPI(ResizeDisk.class)
        putAPI(DescribeImages.class)
        putAPI(CreateImage.class)
        putAPI(DeleteImage.class)
        putAPI(ModifyImageSharePermission.class)
        putAPI(CreateSnapshot.class)
        putAPI(DeleteSnapshot.class)
        putAPI(DescribeSnapshots.class)
        putAPI(DescribeInstanceTypes.class)
        putAPI(DescribeRegions.class)
        putAPI(CreateNetworkInterface.class)
        putAPI(AttachNetworkInterface.class)
        putAPI(DetachNetworkInterface.class)
	    putAPI(DeleteNetworkInterface.class)
	    putAPI(DescribeNetworkInterfaces.class)
	    putAPI(ModifyNetworkInterfaceAttribute.class)
        putAPI(DescribeZones.class)
        putAPI(CreateDeploymentSet.class)
        putAPI(ModifyDeploymentSetAttribute.class)
        putAPI(DescribeDeploymentSets.class)
        putAPI(DeleteDeploymentSet.class)
        putAPI(DescribeInstanceVncUrl.class)
        putAPI(ModifyInstanceVncPasswd.class)
        putAPI(DescribeInstanceTypeFamilies.class)
        putAPI(CreateAutoSnapshotPolicy.class)
        putAPI(DeleteAutoSnapshotPolicy.class)
        putAPI(ModifyAutoSnapshotPolicyEx.class)
        putAPI(DescribeAutoSnapshotPolicyEx.class)
        putAPI(ApplyAutoSnapshotPolicy.class)
        putAPI(CreateSecurityGroup.class)
        putAPI(AuthorizeSecurityGroup.class)
        putAPI(AuthorizeSecurityGroupEgress.class)
        putAPI(JoinSecurityGroup.class)
        putAPI(DescribeSecurityGroups.class)
        putAPI(RevokeSecurityGroup.class)
        putAPI(DeleteSecurityGroup.class)
        putAPI(ModifySecurityGroupAttribute.class)
        putAPI(AllocatePublicIpAddress.class)
        putAPI(RevokeSecurityGroupEgress.class)
        putAPI(LeaveSecurityGroup.class)
        putAPI(DescribeEndpoints.class)
        putAPI(DescribeUserData.class)
        putAPI(DescribeSecurityGroupAttribute.class)
        putAPI(CancelAutoSnapshotPolicy.class)
        putAPI(InnerVpcCloudQueryVgwVipByEcsId.class)
        putAPI(InnerVpcCloudQuerySimpleVpcInfo.class)
	    putAPI(AllocateEipAddress.class)
	    putAPI(AssociateEipAddress.class)
	    putAPI(DescribeEipAddresses.class)
        putAPI(UnassociateEipAddress.class)
        putAPI(ReleaseEipAddress.class)
        putAPI(RunInstances.class)
        putAPI(CreateVpc.class)
        putAPI(CreateVSwitch.class)
        putAPI(DescribeVpcs.class)
        putAPI(DescribeVSwitches.class)
        putAPI(DeleteVSwitch.class)
        putAPI(DeleteVpc.class)
        putAPI(ModifyInstanceVpcAttribute.class)
        putAPI(DescribeInstanceStatus.class)
        putAPI(CreateRouteTable.class)
        putAPI(CreateRouteEntry.class)
        putAPI(DeleteRouteTable.class)
        putAPI(DeleteRouteEntry.class)
        putAPI(ModifyVpcAttribute.class)
        putAPI(ModifyVRouterAttribute.class)
        putAPI(ModifyVSwitchAttribute.class)
        putAPI(DeleteLoadBalancer.class)
        putAPI(AddBackendServers.class)
        putAPI(CreateLoadBalancerUDPListener.class)
        putAPI(SetLoadBalancerUDPListenerAttribute.class)
        putAPI(CreateLoadBalancerTCPListener.class)
        putAPI(SetLoadBalancerTCPListenerAttribute.class)
        putAPI(CreateLoadBalancerHTTPListener.class)
        putAPI(SetLoadBalancerHTTPListenerAttribute.class)
        putAPI(CreateLoadBalancerHTTPSListener.class)
        putAPI(SetLoadBalancerHTTPSListenerAttribute.class)
        putAPI(UploadServerCertificate.class)
        putAPI(DescribeServerCertificates.class)
        putAPI(CreateLoadBalancer.class)
        putAPI(RemoveBackendServers.class)
        putAPI(DescribeLoadBalancers.class)
        putAPI(DescribeSlbQuotas.class)
        putAPI(DescribeVpcAttribute.class)
        putAPI(StartLoadBalancerListener.class)
        putAPI(DescribeTags.class)
        putAPI(SetServerCertificateName.class)
        putAPI(SetLoadBalancerName.class)
        putAPI(DeleteLoadBalancerListener.class)
        putAPI(DescribeHealthStatus.class)
        putAPI(DescribeLoadBalancerAttribute.class)
        putAPI(DescribeVServerGroups.class)
        putAPI(DescribeRouteTableList.class)
        putAPI(DescribeRouteTables.class)
        putAPI(DescribeAvailableResource.class)
        putAPI(DescribeVRouters.class)
        putAPI(DescribeIpv6Gateways.class)
        putAPI(DeleteServerCertificate.class)
        putAPI(DescribeCommonBandwidthPackages.class)
        putAPI(DescribeLoadBalancerUDPListenerAttribute.class)
        putAPI(DescribeLoadBalancerTCPListenerAttribute.class)
        putAPI(DescribeLoadBalancerHTTPListenerAttribute.class)
        putAPI(DescribeLoadBalancerHTTPSListenerAttribute.class)
        putAPI(SetBackendServers.class)
        putAPI(AllocateEipAddressPro.class)
    }

    private static putAPI(Class api) {
        actionAPI.put(api.simpleName, api)
    }

    APIResult callZStackAPI(Map ecsAPIParamMap) {
        String requestId = ExternalAPIAdapterUtils.randomUUID()
        logger.info("[RequestId:${requestId}] Received ECS API request: ${ecsAPIParamMap}".toString())
        return callZStackAPI("GET", ecsAPIParamMap, requestId, null, null)
    }

    APIResult callZStackAPI(String method, Map ecsAPIParamMap, String requestId, String traceId, String rpcId) {
        APIResult result = new APIResult()
        APIError error

        try {
            validateECSAPIBasicParameters(ecsAPIParamMap, method)

            String accessKeyId = ecsAPIParamMap.get(ECS_API_ACCESSKEYID_KEY)
            String accountName = ExternalAPIAdapterUtils.getZStackAccountNameByAccessKey(accessKeyId)
            String sessionId = ZStackSessionFactory.getZStackSessionId(accountName)

            String actionName = ecsAPIParamMap.get(ECS_API_ACTION_KEY)
            API api = actionAPI.get(actionName).newInstance() as API
            api.setSessionId(sessionId)
            api.setRequestId(requestId)

            String value = api.call(ecsAPIParamMap)
            result.value = value
            return result

        } catch (MissingMandatoryParameterException e) {
            error = new APIError(
                    message: "The input parameter \"${e.parameterName}\" that is mandatory for processing this request is not supplied.".toString(),
                    code: ECSErrorCode.InvalidParameter
            )

        } catch (InvalidParameterException e) {
            if (traceId == null || rpcId == null) {
                logger.warn("[RequestId:${requestId}] ${e.parameterName} is not valid".toString(), e)
            } else {
                logger.warn("[EagleEyeTraceId:${traceId}][RpcId:${rpcId}][RequestId:${requestId}] ${e.parameterName} is not valid".toString(), e)
            }

            String details = e?.errorCode?.details
            details = details ? " " + details : ""
            String errCode = e ? e.errorCode.code : ECSErrorCode.InvalidParameter
            error = new APIError(
                    message: "The specified parameter \"${e.parameterName}\" is not valid.$details".toString(),
                    code: errCode
            )

        } catch (APIAdapterSpecifiedErrorException e) {
            if (traceId == null || rpcId == null) {
                logger.warn("[RequestId: ${requestId}] code: ${e.code}, message: ${e.message}.".toString(), e)
            } else {
                logger.warn("[EagleEyeTraceId:${traceId}][RpcId:${rpcId}][RequestId:${requestId}] code: ${e.code}, message: ${e.message}.".toString(), e)
            }
            error = new APIError(
                    message: e.message,
                    code: e.code
            )

        } catch (APIAdapterGlobalPropertyConfigException e) {
            if (traceId == null || rpcId == null) {
                logger.warn("[RequestId:${requestId}] ${e.propertyName} ${e.message}".toString(), e)
            } else {
                logger.warn("[EagleEyeTraceId:${traceId}][RpcId:${rpcId}][RequestId:${requestId}] ${e.propertyName} ${e.message}".toString(), e)
            }
            error = new APIError(
                    message: "APIAdapter configuration error",
                    code: ECSErrorCode.InternalError
            )

        } catch (APIParamConvertException e) {
            error = new APIError(
                    message: "Parameter \" ${e.ecsParamName} \" processing error, ${e.msg}".toString(),
                    code: ECSErrorCode.InvalidParameter
            )

        } catch (APIResponseConvertException e) {
            error = new APIError(
                    message: "API result attribute \"${e.ecsAttributeName}\" processing error, ${e.msg}".toString(),
                    code: ECSErrorCode.InternalError
            )

        } catch (RuntimeException e) {
            if (traceId == null || rpcId == null) {
                logger.warn("[RequestId:${requestId}] ${e.getMessage()}".toString(), e)
            } else {
                logger.warn("[EagleEyeTraceId:${traceId}][RpcId:${rpcId}][RequestId:${requestId}] ${e.getMessage()}".toString(), e)
            }
            error = new APIError(
                    message: "Internal error occurred, ${e.getMessage()}".toString(),
                    code: ECSErrorCode.InternalError
            )
            // todo remove it
            e.printStackTrace()
        }

        error.setRequestId(requestId)
        error.setHostId(ExternalAPIAdapterGlobalProperty.ECS_ENDPOINT_URL)
        result.setError(error)
        return result
    }

    // Action, AccessKeyId, Signature
    private void validateECSAPIBasicParameters(Map ecsAPIParamMap, String method) {
        String actionName = ecsAPIParamMap.get(ECS_API_ACTION_KEY)
        if (actionName == null) {
            throw new MissingMandatoryParameterException(ECS_API_ACTION_KEY)
        }

        String accessKeyId = ecsAPIParamMap.get(ECS_API_ACCESSKEYID_KEY)
        if (accessKeyId == null) {
            throw new MissingMandatoryParameterException(ECS_API_ACCESSKEYID_KEY)
        }

        String signature = ecsAPIParamMap.get(ECS_API_SIGNATURE_KEY)
        if (signature == null) {
            throw new MissingMandatoryParameterException(ECS_API_SIGNATURE_KEY)
        }

        if (!actionAPI.containsKey(actionName)) {
            throw new APIAdapterSpecifiedErrorException(ECSErrorCode.ApiUnsupported, "Unknown action $actionName".toString())
        }

        // todo need remove it
        if (ecsAPIParamMap.containsKey("ignoreSignature")) {
             return
        }

        if (!ExternalAPIAdapterGlobalConfig.ENABLE_SIGNATURE_CHECKING.value(Boolean.class)) {
            return
        }

        if (!ExternalAPIAdapterUtils.validateECSAPISignature(ecsAPIParamMap, method)) {
            throw new InvalidParameterException(ECS_API_SIGNATURE_KEY, null)
        }

    }
}
