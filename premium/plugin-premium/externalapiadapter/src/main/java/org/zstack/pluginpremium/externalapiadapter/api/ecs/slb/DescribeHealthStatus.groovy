package org.zstack.pluginpremium.externalapiadapter.api.ecs.slb

import org.apache.commons.lang.StringUtils
import org.zstack.pluginpremium.externalapiadapter.api.BaseAPI
import org.zstack.sdk.*
import org.zstack.sdk.zwatch.api.GetMetricDataAction

import java.util.stream.Collectors

import static org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants.*

/**
 * Created by Qi Le on 2019/10/9
 */
class DescribeHealthStatus extends BaseAPI {

    List nicRefs
    Map listeners
    Map nics

    @Override
    Class getZStackAction() {
        return QueryLoadBalancerListenerAction.class
    }

    @Override
    protected void configAPIConversionSpec() {
        spec = config {
            convertAPIParam {
                beforeZstackAPIParam = { zstackParamMap ->
                    zstackParamMap.put(ZSTACK_QUERY_CONDITIONS_KEY, [])
                    zstackParamMap.put(ZSTACK_QUERY_REPLYWITHCOUNT_KEY, true)
                }

                simpleConvert {
                    ecsParamName = "LoadBalancerId"
                    zstackParamName = "loadBalancerUuid"

                    putZstackParamValue = { Map zstackParamMap, zstackParamValue ->
                        List conditions = zstackParamMap.get(ZSTACK_QUERY_CONDITIONS_KEY) as List
                        conditions.add("$zstackParamName=$zstackParamValue".toString())
                        if (ecsAPIParamMap.containsKey("ListenerPort")) {
                            conditions.add("loadBalancerPort=${ecsAPIParamMap.get("ListenerPort")}".toString())
                        }
                        if (ecsAPIParamMap.containsKey("ListenerProtocol")) {
                            conditions.add("protocol=${ecsAPIParamMap.get("ListenerProtocol")}".toString())
                        }
                    }
                }
            }

            convertAPIResponse {
                convertResponseAttribute {
                    ecsAttributeName = ECS_SLB_BACKEND_SERVERS
                    ecsAttributeValue = new HashMap<>()

                    addEcsValueToEcsAPIRsp = { Map ecsAPIRsp ->
                        ecsAPIRsp.put(ecsAttributeName, ecsAttributeValue)
                    }

                    convertList {
                        ecsAttributeName = ECS_SLB_BACKEND_SERVER
                        ecsAttributeValue = new ArrayList<>()

                        getZstackAttributeValue = {
                            return nicRefs
                        }

                        getElementZstackValues = {
                            return zstackAttributeValue
                        }

                        addEcsValueToFather = { Map parentMap ->
                            parentMap.put(ecsAttributeName, ecsAttributeValue)
                        }

                        addListElement = { LoadBalancerListenerVmNicRefInventory nicRef ->
                            addConvertResponseAttribute {
                                ecsAttributeValue = new HashMap<>()

                                addEcsValueToFather = { List parentList ->
                                    parentList.add(ecsAttributeValue)
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = "ServerId"
                                    zstackAttributeValue = nicRef.vmNicUuid

                                    addEcsValueToFather = { Map parentMap ->
                                        parentMap.put(ecsAttributeName, zstackAttributeValue)
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = "EniHost"
                                    zstackAttributeValue = nics.get(nicRef.vmNicUuid)

                                    addEcsValueToFather = { Map parentMap ->
                                        if (zstackAttributeValue != null) {
                                            parentMap.put(ecsAttributeName, zstackAttributeValue.ip)
                                        }
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = "Port"
                                    zstackAttributeValue = listeners.get(nicRef.listenerUuid)

                                    addEcsValueToFather = { Map parentMap ->
                                        if (zstackAttributeValue != null) {
                                            parentMap.put(ecsAttributeName, zstackAttributeValue.instancePort)
                                        }
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = "ListenerPort"
                                    zstackAttributeValue = listeners.get(nicRef.listenerUuid)

                                    addEcsValueToFather = { Map parentMap ->
                                        if (zstackAttributeValue != null) {
                                            parentMap.put(ecsAttributeName, zstackAttributeValue.loadBalancerPort)
                                        }
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = "Protocol"
                                    zstackAttributeValue = listeners.get(nicRef.listenerUuid)

                                    addEcsValueToFather = { Map parentMap ->
                                        if (zstackAttributeValue != null) {
                                            parentMap.put(ecsAttributeName, zstackAttributeValue.protocol)
                                        }
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = ECS_API_TYPE_KEY
                                    zstackAttributeValue = ECS_SLB_BACKEND_SERVER_TYPE_ENI

                                    addEcsValueToFather = { Map parentMap ->
                                        parentMap.put(ecsAttributeName, zstackAttributeValue)
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = "ServerIp"

                                    addEcsValueToFather = { Map parentMap ->
                                        VmNicInventory vmNicInventory = nics.get(nicRef.vmNicUuid)
                                        if (vmNicInventory == null) {
                                            return
                                        }
                                        QueryVmInstanceAction action = new QueryVmInstanceAction(
                                                sessionId: sessionId,
                                                conditions: [
                                                        "uuid=${vmNicInventory.vmInstanceUuid}".toString()
                                                ]
                                        )
                                        QueryVmInstanceAction.Result result = action.call()
                                        if (result.error != null) {
                                            return
                                        }
                                        VmInstanceInventory vmInstanceInventory = result.value.inventories.first()
                                        for (VmNicInventory nic : vmInstanceInventory.vmNics) {
                                            if (nic.l3NetworkUuid == vmInstanceInventory.defaultL3NetworkUuid) {
                                                parentMap.put(ecsAttributeName, nic.ip)
                                                break
                                            }
                                        }
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = "ServerHealthStatus"

                                    addEcsValueToFather = { Map parentMap ->
                                        VmNicInventory vmNicInventory = nics.get(nicRef.vmNicUuid)
                                        if (vmNicInventory == null) {
                                            return
                                        }
                                        GetMetricDataAction action = new GetMetricDataAction(
                                                sessionId: sessionId,
                                                namespace: "ZStack/LoadBalancer",
                                                metricName: "LoadBalancerBackendStatus",
                                                offsetAheadOfCurrentTime: 1L,
                                                labels: [
                                                        "ListenerUuid=${nicRef.listenerUuid}".toString(),
                                                        "NicIpAddress=${vmNicInventory.ip}".toString()
                                                ]
                                        )
                                        GetMetricDataAction.Result result = action.call()
                                        String healthStatus

                                        if (result.error != null || result.value.data.size() == 0 || result.value.data.first().value <= 0) {
                                            healthStatus = "abnormal"
                                        } else {
                                            healthStatus = "normal"
                                        }
                                        parentMap.put(ecsAttributeName, healthStatus)
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
    void afterCallZStackAction(def zstackActionResult) {
        listeners = zstackActionResult.value.inventories.stream()
                .collect(Collectors.toMap({ listener -> listener.uuid }, { listener -> listener }))
        nicRefs = zstackActionResult.value.inventories.stream().flatMap { LoadBalancerListenerInventory listener ->
            listener.vmNicRefs.stream()
        }.collect(Collectors.toList())

        List nicUuids = nicRefs.stream().map { nicRef -> nicRef.vmNicUuid }.collect(Collectors.toList())
        QueryVmNicAction action = new QueryVmNicAction(
                sessionId: sessionId,
                conditions: ["uuid?=${StringUtils.join(nicUuids, ',')}".toString()]
        )
        QueryVmNicAction.Result result = action.call()
        result.throwExceptionIfError()
        nics = result.value.inventories.stream()
                .collect(Collectors.toMap({ nic -> nic.uuid }, { nic -> nic }))
    }
}
