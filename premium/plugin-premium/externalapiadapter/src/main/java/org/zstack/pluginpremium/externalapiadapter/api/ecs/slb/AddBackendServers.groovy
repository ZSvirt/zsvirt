package org.zstack.pluginpremium.externalapiadapter.api.ecs.slb

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import org.apache.commons.lang.StringUtils
import org.zstack.network.service.lb.LoadBalancerSystemTags
import org.zstack.network.service.lb.LoadBalancerVO
import org.zstack.pluginpremium.externalapiadapter.EcsSystemTags
import org.zstack.pluginpremium.externalapiadapter.api.BaseAPI
import org.zstack.pluginpremium.externalapiadapter.convert.param.ParameterConversionUtils
import org.zstack.pluginpremium.externalapiadapter.datatypes.SLBBackendServer
import org.zstack.pluginpremium.externalapiadapter.exception.APIAdapterSpecifiedErrorException
import org.zstack.pluginpremium.externalapiadapter.exception.APIParamConvertException
import org.zstack.sdk.*

import java.lang.reflect.Type
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.TimeUnit
import java.util.stream.Collectors

import static org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants.*

/**
 * Created by Qi Le on 2019/10/17
 */
class AddBackendServers extends BaseAPI {
    private static final String INVALID_PARAMETER_CODE = "InvalidParameter"
    private static final String TYPE_NOT_SUPPORT_MESSAGE = "The specified load balancer does not support the network type of the ECS instance."
    private static final String NIC_NOT_FOUND_MESSAGE = "No nic device found for the specified instance."
    private static final String INSTANCE_NOT_FOUND_MESSAGE = "The specified nic device not attached on an instance."

    List<SLBBackendServer> servers
    Queue affectedTags = new ConcurrentLinkedQueue()
    Queue errorOut = new ConcurrentLinkedQueue()
    Queue successAct = new ConcurrentLinkedQueue()
    Thread worker

    @Override
    Class getZStackAction() {
        return null
    }

    @Override
    void setEcsAPIParamDefaultValue(Map ecsAPIParamMap) {
        super.setEcsAPIParamDefaultValue(ecsAPIParamMap)
        String backendServersStr = ecsAPIParamMap[ECS_SLB_BACKEND_SERVERS]
        if (backendServersStr != null) {
            Gson gson = new GsonBuilder().create()
            Type type = new TypeToken<List<SLBBackendServer>>() {}.getType()
            try {
                servers = gson.fromJson(backendServersStr, type)
            } catch (JsonSyntaxException e) {
                throw new APIParamConvertException(ECS_SLB_BACKEND_SERVERS, e.message)
            }
            completeServerInfo(servers)
        }
    }

    @Override
    protected void configAPIConversionSpec() {
        spec = config {
            convertAPIParam {

                simpleConvert {
                    ecsParamName = ECS_SLB_ID
                    zstackParamName = ZSTACK_SLB_ID
                }
            }

            convertAPIResponse {
                convertResponseAttribute {
                    ecsAttributeName = ECS_SLB_ID
                    addEcsValueToEcsAPIRsp = { Map ecsAPIRsp ->
                        (ecsAPIRsp[ecsAttributeName] = zstackAPIRsp.get(ecsAttributeName))
                    }
                }

                convertResponseAttribute {
                    ecsAttributeName = ECS_SLB_BACKEND_SERVERS
                    ecsAttributeValue = new HashMap<>()

                    addEcsValueToEcsAPIRsp = { Map ecsAPIRsp ->
                        (ecsAPIRsp[ecsAttributeName] = ecsAttributeValue)
                    }

                    convertList {
                        ecsAttributeName = ECS_SLB_BACKEND_SERVER
                        ecsAttributeValue = new ArrayList<>()

                        getZstackAttributeValue = {
                            return zstackAPIRsp.get(ecsAttributeName)
                        }

                        getElementZstackValues = {
                            return zstackAttributeValue
                        }

                        addEcsValueToFather = { Map parentMap ->
                            (parentMap[ecsAttributeName] = ecsAttributeValue)
                        }

                        addListElement = { SLBBackendServer serverInfo ->
                            addConvertResponseAttribute {
                                ecsAttributeValue = new HashMap<>()

                                addEcsValueToFather = { List parentList ->
                                    parentList.add(ecsAttributeValue)
                                }

                                zstackAttributeValue = serverInfo

                                convertResponseAttribute {
                                    ecsAttributeName = "ServerId"

                                    zstackAttributeValue = serverInfo.serverId

                                    addEcsValueToFather = { Map parentMap ->
                                        (parentMap[ecsAttributeName] = zstackAttributeValue)
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = "Weight"
                                    zstackAttributeValue = serverInfo.weight

                                    addEcsValueToFather = { Map parentMap ->
                                        (parentMap[ecsAttributeName] = zstackAttributeValue)
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = ECS_API_TYPE_KEY

                                    zstackAttributeValue = serverInfo.type

                                    addEcsValueToFather = { Map parentMap ->
                                        (parentMap[ecsAttributeName] = zstackAttributeValue)
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
    Object callZStackAction() {
        QueryLoadBalancerAction.Result queryLoadBalancerResult = queryLoadBalancer()
        LoadBalancerInventory loadBalancerInventory = queryLoadBalancerResult.value.inventories.first()
        filterBackendServers()
        handleServersTag(zstackAPIParamMap[ZSTACK_SLB_ID] as String)
        if (loadBalancerInventory.listeners.size() != 0) {
            handleServersAndListener(loadBalancerInventory.listeners)
        }

        Map result = new HashMap<>()
        result[ECS_SLB_ID] = zstackAPIParamMap[ZSTACK_SLB_ID]
        List serverList = queryBackendServers()
        result[ECS_SLB_BACKEND_SERVER] = serverList

        this.afterCallZStackAction(result)

        return result
    }

    @Override
    void afterCallZStackAction(Object zstackActionResult) {
        super.afterCallZStackAction(zstackActionResult)
        if (worker == null) {
            return
        }
        int waitingTime = ZSTACK_ASYNC_QUERY_COUNT
        int checkingInterval = QUERY_INTERVAL_TIME
        while (waitingTime >= 0) {
            waitingTime -= checkingInterval
            if (worker.state == Thread.State.TERMINATED && errorOut.isEmpty()) {
                return
            }

            if (!errorOut.isEmpty()) {
                AddVmNicToLoadBalancerAction.Result result = errorOut.peek() as AddVmNicToLoadBalancerAction.Result
                handleError(result.error)
            }

            TimeUnit.SECONDS.sleep(checkingInterval)
        }
    }

    void filterBackendServers() {
        //do nothing
    }

    List queryBackendServers() {
        QuerySystemTagAction.Result result = getAllTags()
        List res = ParameterConversionUtils.getSLBBackendServersInfoFromTags(sessionId, result.value.inventories)
        return res
    }

    protected QuerySystemTagAction.Result getAllTags() {
        QuerySystemTagAction querySystemTagAction = new QuerySystemTagAction(
                sessionId: sessionId,
                conditions: [
                        "resourceType=${LoadBalancerVO.class.getSimpleName()}".toString(),
                        "resourceUuid=${zstackAPIParamMap[ZSTACK_SLB_ID]}".toString()
                ]
        )
        QuerySystemTagAction.Result result = querySystemTagAction.call()
        result.throwExceptionIfError()
        return result
    }

    protected QueryLoadBalancerAction.Result queryLoadBalancer() {
        QueryLoadBalancerAction queryLoadBalancerAction = new QueryLoadBalancerAction(
                sessionId: sessionId,
                conditions: ["uuid=${zstackAPIParamMap[ZSTACK_SLB_ID]}".toString()]
        )
        QueryLoadBalancerAction.Result queryLoadBalancerResult = queryLoadBalancerAction.call()
        queryLoadBalancerResult.throwExceptionIfError()
        if (queryLoadBalancerResult.value.inventories.size() == 0) {
            throw new APIParamConvertException(ECS_SLB_ID, "Cannot find load balancer by Id: ${zstackAPIParamMap[ZSTACK_SLB_ID]}".toString())
        }
        return queryLoadBalancerResult
    }

    void getVmToNicMap(List vmNicUuids, Map nics) {
        if (vmNicUuids == null || vmNicUuids.size() == 0) {
            return
        }

        QueryVmNicAction queryVmNicAction = new QueryVmNicAction(
                sessionId: sessionId,
                conditions: ["uuid?=${StringUtils.join(vmNicUuids, ',')}".toString()]
        )
        QueryVmNicAction.Result queryVmNicResult = queryVmNicAction.call()
        queryVmNicResult.throwExceptionIfError()
        queryVmNicResult.value.inventories.forEach { VmNicInventory nicInventory ->
            nics.put(nicInventory.vmInstanceUuid, nicInventory.uuid)
        }
    }

    void handleServersTag(String slbUuid) {
        // create tags in parallel way, but wait them finished
        servers.parallelStream().forEach { server ->
            CreateSystemTagAction action = new CreateSystemTagAction(
                    sessionId: sessionId,
                    resourceType: LoadBalancerVO.class.getSimpleName(),
                    resourceUuid: slbUuid,
            )
            action.tag = EcsSystemTags.SLB_BACKEND_SERVER_EXT.instantiateTag(
                    [
                            (EcsSystemTags.SLB_BACKEND_SERVER_TOKEN): server.vmInstanceId,
                            (EcsSystemTags.SLB_BACKEND_NIC_TOKEN)   : server.vmNicId,
                            (EcsSystemTags.SLB_BACKEND_TYPE_TOKEN)  : server.type,
                            (EcsSystemTags.SLB_BACKEND_WEIGHT_TOKEN): server.weight
                    ]
            )
            CreateSystemTagAction.Result result = action.call()
            result.throwExceptionIfError()
            if (result.value.inventory != null) {
                affectedTags.offer(result.value.inventory.uuid)
            }
        }
    }

    void handleError(ErrorCode errorCode) {
        String details = errorCode.details
        String code
        String message
        if (details.indexOf("conflict loadBalancerPort") != -1) {
            code = "ListenerAlreadyExists"
            message = "There is already a listener bound to the port on the specified load balancer."
        } else {
            code = ECSErrorCode.InvalidParameter
            message = details
        }
        throw new APIAdapterSpecifiedErrorException(code, message)
    }

    class DoAddNicToListener implements Runnable {
        List listeners
        List nicUuids
        List weightTags
        Queue errorOut
        Queue successAct

        DoAddNicToListener(List listeners, List nicUuids, List weightTags, Queue errorOut, Queue successAct) {
            this.listeners = listeners
            this.nicUuids = nicUuids
            this.weightTags = weightTags
            this.errorOut = errorOut
            this.successAct = successAct
        }

        @Override
        void run() {
            listeners.forEach { listener ->
                AddVmNicToLoadBalancerAction action = new AddVmNicToLoadBalancerAction(
                        sessionId: sessionId,
                        listenerUuid: listener.uuid,
                        vmNicUuids: nicUuids,
                        systemTags: weightTags
                )
                AddVmNicToLoadBalancerAction.Result result = action.call()
                if (result.error == null) {
                    successAct.offer(action)
                } else {
                    errorOut.offer(result)
                    logger.error("failed to add vm nics to listener [uuid: ${listener.uuid}], details: ${result.error.details}")
                    rollback()
                }
            }
        }
    }

    void handleServersAndListener(List listeners) {
        //add server to listener
        List nicUuids = []
        List weightTags = []
        servers.forEach { server ->
            if (server.vmNicId != null) {
                nicUuids.add(server.vmNicId)
                String tag = LoadBalancerSystemTags.BALANCER_WEIGHT.instantiateTag(
                        [
                                (LoadBalancerSystemTags.BALANCER_NIC_TOKEN)   : server.vmNicId,
                                (LoadBalancerSystemTags.BALANCER_WEIGHT_TOKEN): server.weight
                        ]
                )
                weightTags.add(tag)
            }
        }

        DoAddNicToListener addNicToListener = new DoAddNicToListener(listeners, nicUuids, weightTags, errorOut, successAct)
        worker = new Thread(addNicToListener)
        worker.start()
    }

    void rollback() {
        if (!affectedTags.isEmpty()) {
            DeleteTagAction deleteTagAction = new DeleteTagAction(
                    sessionId: sessionId
            )

            affectedTags.forEach { String tagUuid ->
                deleteTagAction.uuid = tagUuid
                deleteTagAction.call()
            }
        }

        if (!successAct.isEmpty()) {
            successAct.forEach { action ->
                def rollbackAction = new RemoveVmNicFromLoadBalancerAction(
                        sessionId: sessionId,
                        listenerUuid: action.listenerUuid,
                        vmNicUuids: action.vmNicUuids
                )
                rollbackAction.call()
            }
        }
    }

    void completeServerInfo(List<SLBBackendServer> backendServers) {
        List<String> vmUuids = new ArrayList<>()
        List<String> nicUuids = new ArrayList<>()

        for (SLBBackendServer server : backendServers) {
            if (server.type == ECS_SLB_BACKEND_SERVER_TYPE_ECS) {
                vmUuids.add(server.serverId)
            } else if (server.type == ECS_SLB_BACKEND_SERVER_TYPE_ENI) {
                nicUuids.add(server.serverId)
            } else {
                logger.error("Not a supported server type:$server.type".toString())
                throw new APIAdapterSpecifiedErrorException(INVALID_PARAMETER_CODE, TYPE_NOT_SUPPORT_MESSAGE)
            }
        }

        Map<String, VmInstanceInventory> instances
        Map<String, VmNicInventory> nics
        if (vmUuids.size() != 0) {
            QueryVmInstanceAction action = new QueryVmInstanceAction(
                    sessionId: sessionId,
                    conditions: [
                            "uuid?=${StringUtils.join(vmUuids, ',')}".toString()
                    ]
            )
            QueryVmInstanceAction.Result result = action.call()
            if (result.error == null) {
                instances = result.value.inventories.stream().collect(Collectors.toMap({ vm -> vm.uuid }, { vm -> vm }))
            }
        }

        if (nicUuids.size() != 0) {
            QueryVmNicAction action = new QueryVmNicAction(
                    sessionId: sessionId,
                    conditions: [
                            "uuid?=${StringUtils.join(nicUuids, ',')}".toString()
                    ]
            )
            QueryVmNicAction.Result result = action.call()
            if (result.error == null) {
                nics = result.value.inventories.stream().collect(Collectors.toMap({ nic -> nic.uuid }, { nic -> nic }))
            }
        }

        for (SLBBackendServer server : backendServers) {
            if (server.type == ECS_SLB_BACKEND_SERVER_TYPE_ECS) {
                server.vmInstanceId = server.serverId
                VmInstanceInventory instance = instances[server.vmInstanceId]
                List vmNics = instance?.vmNics
                if (vmNics != null) {
                    VmNicInventory nicInventory = vmNics.stream()
                            .filter { nic -> nic.l3NetworkUuid == instance.defaultL3NetworkUuid }
                            .findFirst()
                            .orElse(null) as VmNicInventory
                    server.vmNicId = nicInventory?.uuid
                }
                if (server.vmNicId == null) {
                    throw new APIAdapterSpecifiedErrorException(INVALID_PARAMETER_CODE, NIC_NOT_FOUND_MESSAGE)
                }
            } else {
                server.vmNicId = server.serverId
                server.vmInstanceId = nics[server.vmNicId]?.vmInstanceUuid
                if (server.vmInstanceId == null) {
                    throw new APIAdapterSpecifiedErrorException(INVALID_PARAMETER_CODE, INSTANCE_NOT_FOUND_MESSAGE)
                }
            }
        }
    }
}
