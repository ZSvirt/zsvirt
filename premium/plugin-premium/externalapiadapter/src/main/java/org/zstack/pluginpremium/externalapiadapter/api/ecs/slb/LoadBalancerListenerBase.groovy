package org.zstack.pluginpremium.externalapiadapter.api.ecs.slb


import org.apache.commons.lang.StringUtils
import org.zstack.network.service.lb.LoadBalancerSystemTags
import org.zstack.network.service.lb.LoadBalancerVO
import org.zstack.pluginpremium.externalapiadapter.api.BaseAsyncAPI
import org.zstack.pluginpremium.externalapiadapter.convert.param.ParameterConversionUtils
import org.zstack.pluginpremium.externalapiadapter.exception.APIAdapterSpecifiedErrorException
import org.zstack.sdk.*

import java.util.stream.Collectors

import static org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants.*
/**
 * Created by Qi Le on 2019-07-26
 */
abstract class LoadBalancerListenerBase extends BaseAsyncAPI {

    private static final String protocol = "undefined"
    protected static final String INVALID_PARAMETER_CODE = "InvalidParameter"

    @Override
    Class getZStackAction() {
        return CreateLoadBalancerListenerAction.class
    }

    @Override
    protected void configAPIConversionSpec() {
        spec = config {
            convertAPIParam {
                simpleConvert {
                    ecsParamName = ECS_API_DESCRIPTION_KEY
                    zstackParamName = ZSTACK_API_DESCRIPTION_KEY
                }

                simpleConvert {
                    ecsParamName = "LoadBalancerId"
                    zstackParamName = "loadBalancerUuid"
                }

                simpleConvert {
                    ecsParamName = "BackendServerPort"
                    zstackParamName = "instancePort"
                }

                simpleConvert {
                    ecsParamName = "ListenerPort"
                    zstackParamName = "loadBalancerPort"
                }

                zstackNeedParam {
                    zstackParamName = "protocol"

                    getZstackValue = { ecsParamMap, zstackParamMap ->
                        return protocol
                    }
                }

                zstackNeedParam {
                    zstackParamName = ZSTACK_NAME

                    getZstackValue = { Map ecsParamMap, zstackParamMap ->
                        String loadBalancerId = ecsParamMap.get("LoadBalancerId")
                        return "${protocol}-listener-slb-${loadBalancerId}".toString()
                    }
                }

                zstackNeedParam {
                    zstackParamName = ZSTACK_SLB_LISTENER_HEALTH_CHECK_TYPE

                    getZstackValue = { Map ecsParamMap, Map zstackParamMap ->
                        return convertHealthCheckType(ecsParamMap, zstackParamMap)
                    }
                }
            }

            convertAPIResponse {}
        }
    }

    String convertHealthCheckType(Map ecsParamMap, Map zstackParamMap) {
        return null
    }

    void addBackendServersToListener(String listenerUuid) {
        List<Tuple2> serverNic = getAllServerNicUuidAndWeight()
        if (serverNic == null || serverNic.size() == 0) {
            return
        }

        List nicUuids = new ArrayList<>()
        List weightTags = new ArrayList<>()

        serverNic.forEach { tuple ->
            nicUuids.add(tuple.getFirst())
            String tag = LoadBalancerSystemTags.BALANCER_WEIGHT.instantiateTag(
                    [
                            (LoadBalancerSystemTags.BALANCER_NIC_TOKEN): tuple.getFirst(),
                            (LoadBalancerSystemTags.BALANCER_WEIGHT_TOKEN): tuple.getSecond()
                    ]
            )
            weightTags.add(tag)
        }

        AddVmNicToLoadBalancerAction action = new AddVmNicToLoadBalancerAction(
                sessionId: sessionId,
                listenerUuid: listenerUuid,
                vmNicUuids: nicUuids,
                systemTags: weightTags
        )
        AddVmNicToLoadBalancerAction.Result result = action.call()
        if (result.error != null) {
            rollback(listenerUuid)
        }
        result.throwExceptionIfError()
    }

    void rollback(String listenerUuid) {
        DeleteLoadBalancerListenerAction action = new DeleteLoadBalancerListenerAction(
                sessionId: sessionId,
                uuid: listenerUuid,
        )
        action.call()
    }

    List getAllServerNicUuidAndWeight() {
        QuerySystemTagAction querySystemTagAction = new QuerySystemTagAction(
                sessionId: sessionId,
                conditions: [
                        "resourceType=${LoadBalancerVO.class.getSimpleName()}".toString(),
                        "resourceUuid=${zstackAPIParamMap.get("loadBalancerUuid")}".toString()
                ]
        )
        QuerySystemTagAction.Result result = querySystemTagAction.call()
        result.throwExceptionIfError()
        List serversInfo = ParameterConversionUtils.getSLBBackendServersInfoFromTags(sessionId, result.value.inventories)
        List res = serversInfo.stream().map { server ->
            new Tuple2<>(server.vmNicId, server.weight)
        }.collect(Collectors.toList())
        res
    }

    @Override
    void afterCallZStackAction(Object zstackActionResult) {
        LoadBalancerListenerInventory listener = (LoadBalancerListenerInventory) zstackActionResult.value.inventory
        ChangeLoadBalancerListenerAction action = new ChangeLoadBalancerListenerAction(
                sessionId: sessionId,
                uuid: listener.uuid
        )
        boolean needDoAction = setChangeListenerParam(action)
        if (needDoAction) {
            ChangeLoadBalancerListenerAction.Result result = action.call()
            result.throwExceptionIfError()
        }

        if (ecsAPIParamMap.containsKey("ServerCertificateId")) {
            AddCertificateToLoadBalancerListenerAction certificateAction =
                    new AddCertificateToLoadBalancerListenerAction(
                            sessionId: sessionId,
                            listenerUuid: listener.uuid,
                            certificateUuid: ecsAPIParamMap["ServerCertificateId"]
                    )
            AddCertificateToLoadBalancerListenerAction.Result certificateResult = certificateAction.call()
            certificateResult.throwExceptionIfError()
        }

        addBackendServersToListener(listener.uuid)
    }

    boolean setChangeListenerParam(ChangeLoadBalancerListenerAction action) {
        boolean needDoAction = false
        if (ecsAPIParamMap.containsKey("HealthCheckConnectPort")) {
            String port = ecsAPIParamMap["HealthCheckConnectPort"]
            if (port != "-520") {
                action.healthCheckTarget = port
                needDoAction = true
            }
        }
        if (ecsAPIParamMap.containsKey("HealthyThreshold")) {
            action.healthyThreshold = Integer.parseInt((String) ecsAPIParamMap["HealthyThreshold"])
            needDoAction = true
        }
        if (ecsAPIParamMap.containsKey("UnhealthyThreshold")) {
            action.unhealthyThreshold = Integer.parseInt((String) ecsAPIParamMap["UnhealthyThreshold"])
            needDoAction = true
        }
        if (ecsAPIParamMap.containsKey("healthCheckInterval")) {
            action.healthCheckInterval = Integer.parseInt((String) ecsAPIParamMap["healthCheckInterval"])
            needDoAction = true
        }
        if (ecsAPIParamMap.containsKey("IdleTimeout")) {
            action.connectionIdleTimeout = Integer.parseInt((String) ecsAPIParamMap["IdleTimeout"])
            needDoAction = true
        }
        if (ecsAPIParamMap.containsKey("Scheduler")) {
            String scheduler = ecsAPIParamMap["Scheduler"]
            String algorithm
            if (scheduler == "wrr") {
                algorithm = "weightroundrobin"
            } else {
                algorithm = "roundrobin"
            }
            action.balancerAlgorithm = algorithm
            needDoAction = true
        }
        if (ecsAPIParamMap.containsKey("HealthCheckMethod")) {
            String method = ecsAPIParamMap["HealthCheckMethod"]
            if (StringUtils.equalsIgnoreCase("GET", method)) {
                method = "GET"
            } else if (StringUtils.equalsIgnoreCase("HEAD", method)) {
                method = "HEAD"
            } else {
                throw new APIAdapterSpecifiedErrorException(INVALID_PARAMETER_CODE, "The specified parameter HealthCheckMethod is not valid.")
            }
            action.healthCheckMethod = method
            needDoAction = true
        }
        return needDoAction
    }
}
