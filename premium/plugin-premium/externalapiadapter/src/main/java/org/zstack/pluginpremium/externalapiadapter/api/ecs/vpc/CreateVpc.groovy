package org.zstack.pluginpremium.externalapiadapter.api.ecs.vpc

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.apache.commons.lang.StringUtils
import org.zstack.aliyunproxy.vpc.AliyunProxyVpcStatus
import org.zstack.aliyunproxy.vpc.AliyunProxyVpcVO
import org.zstack.pluginpremium.externalapiadapter.EcsSystemTags
import org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterGlobalProperty
import org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterUtils
import org.zstack.pluginpremium.externalapiadapter.api.BaseAPI
import org.zstack.pluginpremium.externalapiadapter.exception.APIParamConvertException
import org.zstack.pluginpremium.externalapiadapter.zstacksdkwrapper.CreateVpcVRouterActionCallWrapper
import org.zstack.sdk.*
import org.zstack.utils.gson.JSONObjectUtil

import java.util.concurrent.TimeUnit

import static org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants.*

/**
 * Created by lining on 2018/5/28.
 */

class CreateVpc extends BaseAPI {

    String routeTableUuid
    boolean isProxyVpcReady = false

    @Override
    Class getZStackAction() {
        return CreateAliyunProxyVpcAction.class
    }

    @Override
    void setEcsAPIParamDefaultValue(Map ecsAPIParamMap) {
        super.setEcsAPIParamDefaultValue(ecsAPIParamMap)

        if (!ecsAPIParamMap.containsKey("CidrBlock")) {
            ecsAPIParamMap.put("CidrBlock", "172.16.0.0/12")
        }

        if (!ecsAPIParamMap.containsKey(ECS_VPC_VPC_NAME)) {
            ecsAPIParamMap.put(ECS_VPC_VPC_NAME, "untitledVpc")
        }
    }

    @Override
    protected void configAPIConversionSpec() {
        spec = config {
            convertAPIParam {
                simpleConvert {
                    ecsParamName = "CidrBlock"
                    zstackParamName = "cidrBlock"
                }

                simpleConvert {
                    ecsParamName = ECS_API_DESCRIPTION_KEY
                    zstackParamName = ZSTACK_API_DESCRIPTION_KEY
                }

                simpleConvert {
                    ecsParamName = ECS_VPC_VPC_NAME
                    zstackParamName = ZSTACK_NAME
                }

                zstackNeedParam {
                    zstackParamName = "vRouterUuid"
                    getZstackValue = { Map ecsParamMap, Map zstackParamMap ->
                        return ExternalAPIAdapterUtils.randomUUID()
                    }
                }

                zstackNeedParam {
                    zstackParamName = ZSTACK_RESOURCEUUID_KEY
                    getZstackValue = { Map ecsParamMap, Map zstackParamMap ->
                        String clientToken = ecsParamMap.get(ECS_API_CLIENTTOKEN_KEY)
                        return ExternalAPIAdapterUtils.randomUUID(clientToken)
                    }
                }
            }

            convertAPIResponse {
                convertResponseAttribute {
                    ecsAttributeName = ECS_VPC_VPC_ID

                    getZstackAttributeValue = {
                        return zstackAPIParamMap.get(ZSTACK_RESOURCEUUID_KEY)
                    }

                    addEcsValueToEcsAPIRsp = {ecsAPIRsp ->
                        ecsAPIRsp.put(ecsAttributeName, zstackAttributeValue)
                    }
                }

                convertResponseAttribute {
                    ecsAttributeName = ECS_VPC_VROUTER_ID

                    getZstackAttributeValue = {
                        return zstackAPIParamMap.get("vRouterUuid")
                    }

                    addEcsValueToEcsAPIRsp = {ecsAPIRsp ->
                        ecsAPIRsp.put(ecsAttributeName, zstackAttributeValue)
                    }
                }

                convertResponseAttribute {
                    ecsAttributeName = ECS_ROUTE_TABLE_ID

                    addEcsValueToEcsAPIRsp = {ecsAPIRsp ->
                        ecsAPIRsp.put(ecsAttributeName, routeTableUuid)
                    }
                }
            }
        }
    }

    @Override
    Object callZStackAction() {

        Gson gson = new GsonBuilder().create()
        CreateAliyunProxyVpcAction action = gson.fromJson(JSONObjectUtil.toJsonString(zstackAPIParamMap), this.getZStackAction())

        createVpcVRouter(action)
        checkVRouterExisted(action)
        def result = action.call()
        result.throwExceptionIfError()
        isProxyVpcReady = true

        this.afterCallZStackAction(result)

        return result
    }

    void createVpcVRouter(CreateAliyunProxyVpcAction action) {
        CreateVpcVRouterAction createVpcVRouterAction = new CreateVpcVRouterActionCallWrapper(
                sessionId: sessionId,
                resourceUuid: action.vRouterUuid,
                name: "VPC-VRouter-${action.name}".toString(),
                virtualRouterOfferingUuid: ExternalAPIAdapterGlobalProperty.VIRTUALROUTEROFFERINGUUID,
        )

        CreateVpcVRouterAction.Result vRouterResult = createVpcVRouterAction.call2(new Completion<CreateVpcVRouterAction.Result>() {
            @Override
            void complete(CreateVpcVRouterAction.Result ret) {
                if (ret.error != null) {
                    QueryAliyunProxyVpcAction queryVpcAction = new QueryAliyunProxyVpcAction(
                            sessionId: sessionId,
                            conditions: ["vRouterUuid=$action.vRouterUuid"]
                    )
                    QueryAliyunProxyVpcAction.Result queryVpcResult = queryVpcAction.call()
                    AliyunProxyVpcInventory vpc = queryVpcResult.value.inventories.first()
                    if (vpc != null) {
                        DeleteAliyunProxyVpcAction deleteVpcAction = new DeleteAliyunProxyVpcAction(
                                sessionId: sessionId,
                                uuid: vpc.uuid
                        )
                        deleteVpcAction.call()
                    }
                }
                ret.throwExceptionIfError()

                String[] dnsArray = StringUtils.split(ExternalAPIAdapterGlobalProperty.ALI_SERVICE_DNS, ',')

                if (dnsArray == null || dnsArray.length == 0) {
                    dnsArray[0] = "223.5.5.5"
                }

                for (String dns : dnsArray) {
                    AddDnsToVpcRouterAction addDnsToVpcRouterAction = new AddDnsToVpcRouterAction(
                            sessionId: sessionId,
                            uuid: action.vRouterUuid,
                            dns: dns
                    )
                    AddDnsToVpcRouterAction.Result addDnsToVpcRouterResult = addDnsToVpcRouterAction.call()
                    addDnsToVpcRouterResult.throwExceptionIfError()
                }

                checkProxyVpcExisted()

                UpdateAliyunProxyVpcAction updateVpcAction = new UpdateAliyunProxyVpcAction(
                        sessionId: sessionId,
                        uuid: action.resourceUuid,
                        status: AliyunProxyVpcStatus.Available.toString()
                )
                UpdateAliyunProxyVpcAction.Result updateVpcResult = updateVpcAction.call()
                updateVpcResult.throwExceptionIfError()
            }
        })

        vRouterResult.throwExceptionIfError()
    }


    @Override
    void afterCallZStackAction(Object zstackActionResult) {
        CreateAliyunProxyVpcAction.Result result = zstackActionResult
        AliyunProxyVpcInventory vpcInventory = result.value.inventory

        CreateVRouterRouteTableAction cAction = new CreateVRouterRouteTableAction(
                sessionId: sessionId,
                name: "VPC-RouteTable-${vpcInventory.vpcName}".toString()
        )
        CreateVRouterRouteTableAction.Result cResult = cAction.call()
        cResult.throwExceptionIfError()
        routeTableUuid = cResult.value.inventory.uuid

        CreateSystemTagAction tagAction = new CreateSystemTagAction(
                sessionId: sessionId,
                resourceType: AliyunProxyVpcVO.class.getSimpleName(),
                resourceUuid: vpcInventory.uuid,
                tag: EcsSystemTags.DEFAULT_ROUTE_TABLE.instantiateTag([(EcsSystemTags.DEFAULT_ROUTE_TABLE_TOKEN): routeTableUuid])
        )
        CreateSystemTagAction.Result tagResult = tagAction.call()
        tagResult.throwExceptionIfError()
        //note: a route table cannot be attached to vrouter till a vswitch is created
    }

    void checkVRouterExisted(CreateAliyunProxyVpcAction action) {
        QueryVpcRouterAction queryVRouterAction = new QueryVpcRouterAction(
                sessionId: sessionId,
                conditions: ["uuid=${action.vRouterUuid}".toString()]
        )

        int count = ZSTACK_ASYNC_QUERY_COUNT
        while (count >= 0) {
            count -= QUERY_INTERVAL_TIME

            QueryVpcRouterAction.Result queryResult = queryVRouterAction.call()
            if (queryResult.value.inventories.size() != 0) {
                return
            }

            TimeUnit.SECONDS.sleep(QUERY_INTERVAL_TIME)
        }
        throw new APIParamConvertException("Internal", "VRouter not ready in $ZSTACK_ASYNC_QUERY_COUNT seconds.".toString())
    }

    void checkProxyVpcExisted() {
        int count = ZSTACK_ASYNC_QUERY_COUNT
        while (count >= 0) {
            count -= QUERY_INTERVAL_TIME

            if (isProxyVpcReady) {
                return
            }

            TimeUnit.SECONDS.sleep(QUERY_INTERVAL_TIME)
        }
        throw new APIParamConvertException("Internal", "Proxy vpc not created in $ZSTACK_ASYNC_QUERY_COUNT seconds.".toString())
    }
}
