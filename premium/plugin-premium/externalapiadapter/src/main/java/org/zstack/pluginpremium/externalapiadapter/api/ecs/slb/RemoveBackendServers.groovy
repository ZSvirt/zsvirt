package org.zstack.pluginpremium.externalapiadapter.api.ecs.slb

import org.zstack.pluginpremium.externalapiadapter.EcsSystemTags
import org.zstack.sdk.*

import java.util.stream.Collectors

/**
 * Created by Qi Le on 2019/10/17
 */
class RemoveBackendServers extends AddBackendServers {
    @Override
    void handleServersTag(String slbUuid) {
        //remove tags
        Set instanceUuids = servers.stream().map {server ->
            server.vmInstanceId
        }.collect(Collectors.toSet())
        QuerySystemTagAction.Result result = getAllTags()
        List<SystemTagInventory> allTags = result.value.inventories as List<SystemTagInventory>
        List<SystemTagInventory> tags = new ArrayList<>()
        for (SystemTagInventory tag : allTags) {
            if (EcsSystemTags.SLB_BACKEND_SERVER_EXT.isMatch(tag.getTag()) &&
                    instanceUuids.contains(EcsSystemTags.SLB_BACKEND_SERVER_EXT.getTokenByTag(tag.getTag(),
                            EcsSystemTags.SLB_BACKEND_SERVER_TOKEN))) {
                tags.add(tag)
                continue
            }
            if (EcsSystemTags.SLB_BACKEND_SERVER.isMatch(tag.getTag()) &&
                    instanceUuids.contains(EcsSystemTags.SLB_BACKEND_SERVER.getTokenByTag(tag.getTag(),
                            EcsSystemTags.SLB_BACKEND_SERVER_TOKEN))) {
                tags.add(tag)
            }
        }
        tags.parallelStream().forEach { SystemTagInventory tag ->
            DeleteTagAction action = new DeleteTagAction(
                    sessionId: sessionId,
                    uuid: tag.uuid
            )
            action.call()
        }
    }

    @Override
    void handleServersAndListener(List listeners) {
        //remove server from listener
        List nicUuids = new ArrayList<>()
        servers.forEach { server ->
            if (server.vmNicId != null) {
                nicUuids.add(server.vmNicId)
            }
        }
        listeners.forEach { listener ->
            RemoveVmNicFromLoadBalancerAction action = new RemoveVmNicFromLoadBalancerAction(
                    sessionId: sessionId,
                    listenerUuid: listener.uuid,
                    vmNicUuids: nicUuids
            )
            action.call(new Completion<RemoveVmNicFromLoadBalancerAction.Result>() {
                @Override
                void complete(RemoveVmNicFromLoadBalancerAction.Result ret) {
                    //just submit task, do nothing here
                }
            })
        }
    }
}
