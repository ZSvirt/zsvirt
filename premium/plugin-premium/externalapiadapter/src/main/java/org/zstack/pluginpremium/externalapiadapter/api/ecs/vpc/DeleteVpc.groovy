package org.zstack.pluginpremium.externalapiadapter.api.ecs.vpc

import org.zstack.pluginpremium.externalapiadapter.EcsSystemTags
import org.zstack.pluginpremium.externalapiadapter.api.BaseAsyncAPI
import org.zstack.pluginpremium.externalapiadapter.exception.APIAdapterSpecifiedErrorException
import org.zstack.pluginpremium.externalapiadapter.exception.APIParamConvertException
import org.zstack.sdk.*

import static org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants.ECS_VPC_VPC_ID
import static org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants.ZSTACK_UUID

/**
 * Created by lining on 2018/5/30.
 */
class DeleteVpc extends BaseAsyncAPI {

    String defaultRouteTableUuid
    String vRouterUuid

    @Override
    Class getZStackAction() {
        return DeleteAliyunProxyVpcAction.class
    }

    @Override
    void afterCallZStackAction(def zstackActionResult) {
        if (defaultRouteTableUuid != null) {
            DeleteVRouterRouteTableAction deleteRouteTableAction = new DeleteVRouterRouteTableAction(
                    sessionId: sessionId,
                    uuid: defaultRouteTableUuid
            )
            DeleteVRouterRouteTableAction.Result deleteRouteTableResult = deleteRouteTableAction.call()
            if (deleteRouteTableResult.error != null) {
                logger.error("failed to delete route table: ${deleteRouteTableResult.error.details}".toString())
            }
        }

        DestroyVmInstanceAction destroyVpcVrAction = new DestroyVmInstanceAction(
                sessionId: sessionId,
                uuid: vRouterUuid
        )
        DestroyVmInstanceAction.Result destroyVRouterRes = destroyVpcVrAction.call()
        if (destroyVRouterRes.error != null) {
            logger.error("failed to destroy vRouter: ${destroyVRouterRes.error.details}".toString())
        }
    }

    @Override
    Object callZStackAction() {
        String vpcUuid = zstackAPIParamMap[ZSTACK_UUID]

        QueryAliyunProxyVSwitchAction queryVSwitchAction = new QueryAliyunProxyVSwitchAction(
                sessionId: sessionId,
                conditions: ["aliyunProxyVpcUuid=${vpcUuid}".toString()]
        )
        QueryAliyunProxyVSwitchAction.Result vSwitchResult = queryVSwitchAction.call()
        vSwitchResult.throwExceptionIfError()
        if (vSwitchResult.value.inventories.size() != 0) {
            throw new APIAdapterSpecifiedErrorException("DependencyViolation.VSwitch", "Delete the vSwitches attached on this VPC first.")
        }

        QuerySystemTagAction querySystemTagAction = new QuerySystemTagAction(
                sessionId: sessionId,
                conditions: ["resourceUuid=${vpcUuid}".toString()]
        )
        QuerySystemTagAction.Result systemTagResult = querySystemTagAction.call()

        if (systemTagResult.value.inventories.size() != 0) {
            List<SystemTagInventory> sysTags = systemTagResult.value.inventories as List<SystemTagInventory>
            for (SystemTagInventory tag : sysTags) {
                String securityUuid = EcsSystemTags.SECURITYGROUP_ID.getTokenByTag(tag.tag, EcsSystemTags.SECURITYGROUP_ID_TOKEN)
                if (securityUuid != null) {
                    throw new APIAdapterSpecifiedErrorException("DependencyViolation.SecurityGroup", "Delete the security groups attached on this VPC first")
                }
                String routeTableUuid = EcsSystemTags.DEFAULT_ROUTE_TABLE.getTokenByTag(tag.tag, EcsSystemTags.DEFAULT_ROUTE_TABLE_TOKEN)
                if (routeTableUuid != null) {
                    defaultRouteTableUuid = routeTableUuid
                }
            }
        }

        QueryAliyunProxyVpcAction queryAliyunProxyVpcAction = new QueryAliyunProxyVpcAction(
                sessionId: sessionId,
                conditions: ["uuid=$vpcUuid".toString()]
        )
        QueryAliyunProxyVpcAction.Result aliyunProxyVpcResult = queryAliyunProxyVpcAction.call()
        aliyunProxyVpcResult.throwExceptionIfError()

        if (aliyunProxyVpcResult.value.inventories.isEmpty()) {
            throw new APIParamConvertException(ECS_VPC_VPC_ID, "Cannot found vpc[id:$vpcUuid]".toString())
        }

        AliyunProxyVpcInventory vpc = aliyunProxyVpcResult.value.inventories.first()

        vRouterUuid = vpc.vRouterUuid

        if (defaultRouteTableUuid == null && vRouterUuid != null) {
            QueryVRouterRouteTableAction queryVRouterRouteTableAction = new QueryVRouterRouteTableAction(
                    sessionId: sessionId,
                    conditions: ["attachedRouterRef.virtualRouterVmUuid=${vRouterUuid}".toString()]
            )

            QueryVRouterRouteTableAction.Result routeTableResult = queryVRouterRouteTableAction.call()
            routeTableResult.throwExceptionIfError()
            if (routeTableResult.value.inventories.size() != 0) {
                defaultRouteTableUuid = routeTableResult.value.inventories.first().uuid
            }
        }

        return super.callZStackAction()
    }

    @Override
    void configAPIConversionSpec() {
        spec = config {
            convertAPIParam {
                simpleConvert {
                    ecsParamName = ECS_VPC_VPC_ID
                    zstackParamName = ZSTACK_UUID
                }
            }

            convertAPIResponse {
            }
        }
    }
}
