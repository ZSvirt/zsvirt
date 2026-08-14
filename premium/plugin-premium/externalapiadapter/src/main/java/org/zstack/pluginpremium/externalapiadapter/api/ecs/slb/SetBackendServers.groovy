package org.zstack.pluginpremium.externalapiadapter.api.ecs.slb

import org.zstack.network.service.lb.LoadBalancerSystemTags
import org.zstack.pluginpremium.externalapiadapter.EcsSystemTags
import org.zstack.pluginpremium.externalapiadapter.datatypes.SLBBackendServer
import org.zstack.sdk.ChangeLoadBalancerListenerAction
import org.zstack.sdk.SystemTagInventory
import org.zstack.sdk.UpdateSystemTagAction

import java.util.stream.Collectors

import static org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants.ECS_SLB_BACKEND_SERVER_TYPE_ECS
import static org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants.ECS_SLB_BACKEND_SERVER_TYPE_ENI

/**
 * Created by Qi Le on 2020/6/11
 */
class SetBackendServers extends AddBackendServers {
    @Override
    void handleServersAndListener(List listeners) {
        //change the weight via API call
        List tags = servers.stream().map({ server ->
            LoadBalancerSystemTags.BALANCER_WEIGHT.instantiateTag(
                    [
                            (LoadBalancerSystemTags.BALANCER_WEIGHT_TOKEN): server.weight,
                            (LoadBalancerSystemTags.BALANCER_NIC_TOKEN)   : server.vmNicId
                    ]
            )
        }).collect(Collectors.toList())

        listeners.forEach { listener ->
            ChangeLoadBalancerListenerAction action = new ChangeLoadBalancerListenerAction(
                    sessionId: sessionId,
                    uuid: listener.uuid,
                    systemTags: tags
            )
            ChangeLoadBalancerListenerAction.Result result = action.call()
            result.throwExceptionIfError()
        }
    }

    @Override
    void handleServersTag(String slbUuid) {
        //servers: all backend server need to be set
        //tags: all backend servers tags
        //find out tags need to be set and update them
        List tags = getAllTags().value.inventories
        Map targetServers = servers.stream().collect(Collectors.toMap({ server ->
            if (server.type == ECS_SLB_BACKEND_SERVER_TYPE_ECS) {
                return server.vmInstanceId
            }
            if (server.type == ECS_SLB_BACKEND_SERVER_TYPE_ENI) {
                return server.vmNicId
            }
        }, { server -> server }))

        tags.stream().forEach({ SystemTagInventory tag ->
            Map tokens = EcsSystemTags.SLB_BACKEND_SERVER_EXT.getTokensByTag(tag.tag)
            SLBBackendServer server
            if (tokens[EcsSystemTags.SLB_BACKEND_TYPE_TOKEN] == ECS_SLB_BACKEND_SERVER_TYPE_ECS) {
                server = targetServers[tokens[EcsSystemTags.SLB_BACKEND_SERVER_TOKEN]]
            }
            if (tokens[EcsSystemTags.SLB_BACKEND_TYPE_TOKEN] == ECS_SLB_BACKEND_SERVER_TYPE_ENI) {
                server = targetServers[tokens[EcsSystemTags.SLB_BACKEND_NIC_TOKEN]]
            }
            if (server != null) {
                UpdateSystemTagAction updateTagAction = new UpdateSystemTagAction(
                        sessionId: sessionId,
                        uuid: tag.uuid,
                        tag: EcsSystemTags.SLB_BACKEND_SERVER_EXT.instantiateTag(
                                [
                                        (EcsSystemTags.SLB_BACKEND_NIC_TOKEN)   : server.vmNicId,
                                        (EcsSystemTags.SLB_BACKEND_SERVER_TOKEN): server.vmInstanceId,
                                        (EcsSystemTags.SLB_BACKEND_TYPE_TOKEN)  : server.type,
                                        (EcsSystemTags.SLB_BACKEND_WEIGHT_TOKEN): server.weight
                                ]
                        )
                )
                UpdateSystemTagAction.Result updateTagResult = updateTagAction.call()
                updateTagResult.throwExceptionIfError()
            }
        })
    }

    @Override
    void filterBackendServers() {
        Set existedServers = queryBackendServers().stream().map({ server ->
            if (server.type == ECS_SLB_BACKEND_SERVER_TYPE_ECS) {
                return server.vmInstanceId
            }
            if (server.type == ECS_SLB_BACKEND_SERVER_TYPE_ENI) {
                return server.vmNicId
            }
        }).collect(Collectors.toSet())

        servers = servers.stream().filter({ server ->
            if (server.type == ECS_SLB_BACKEND_SERVER_TYPE_ECS) {
                return existedServers.contains(server.vmInstanceId)
            }
            if (server.type == ECS_SLB_BACKEND_SERVER_TYPE_ENI) {
                return existedServers.contains(server.vmNicId)
            }
        }).collect(Collectors.toList())
    }
}
