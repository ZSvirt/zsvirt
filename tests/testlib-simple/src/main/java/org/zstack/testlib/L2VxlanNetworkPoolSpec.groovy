package org.zstack.testlib

import org.springframework.http.HttpEntity
import org.zstack.core.db.Q
import org.zstack.header.Constants
import org.zstack.header.host.HostVO
import org.zstack.header.host.HostVO_
import org.zstack.network.l2.vxlan.vxlanNetworkPool.VxlanKvmAgentCommands
import org.zstack.network.l2.vxlan.vxlanNetworkPool.VxlanNetworkPoolConstant
import org.zstack.sdk.L2NetworkInventory
import org.zstack.utils.gson.JSONObjectUtil

/**
 * Created by weiwang on 15/03/2017.
 */
class L2VxlanNetworkPoolSpec extends L2NetworkSpec implements Simulator {
    L2VxlanNetworkPoolSpec(EnvSpec envSpec) {
        super(envSpec)
    }

    SpecID create(String uuid, String sessionId) {
        inventory = createL2VxlanNetworkPool {
            delegate.name = name
            delegate.description = description
            delegate.physicalInterface = physicalInterface
            delegate.resourceUuid = uuid
            delegate.userTags = userTags
            delegate.systemTags = systemTags
            delegate.sessionId = sessionId
            delegate.zoneUuid = (parent as ZoneSpec).inventory.uuid
        } as L2NetworkInventory

        postCreate {
            inventory = queryL2VxlanNetworkPool {
                conditions=["uuid=${inventory.uuid}".toString()]
            }[0]
        }

        return id(name, inventory.uuid)
    }

    @Override
    void registerSimulators(EnvSpec env) {
        env.simulator(VxlanNetworkPoolConstant.VXLAN_KVM_CHECK_L2VXLAN_NETWORK_PATH) { HttpEntity<String> entity, EnvSpec spec ->
            def rsp = new VxlanKvmAgentCommands.CheckVxlanCidrResponse()
            def cmd = JSONObjectUtil.toObject(entity.body, VxlanKvmAgentCommands.CheckVxlanCidrCmd.class)
            def hostUuid = entity.getHeaders().getFirst(Constants.AGENT_HTTP_HEADER_RESOURCE_UUID)

            rsp.success = true

            if (cmd.vtepip != null) {
                rsp.vtepIp = cmd.vtepip
            } else {
                rsp.vtepIp = Q.New(HostVO.class).select(HostVO_.managementIp).eq(HostVO_.uuid, hostUuid).findValue()
            }

            return rsp
        }

        env.simulator(VxlanNetworkPoolConstant.VXLAN_KVM_REALIZE_L2VXLAN_NETWORK_PATH) { HttpEntity<String> entity, EnvSpec spec ->
            return new VxlanKvmAgentCommands.CreateVxlanBridgeResponse()
        }

        env.simulator(VxlanNetworkPoolConstant.VXLAN_KVM_POPULATE_FDB_L2VXLAN_NETWORK_PATH) { HttpEntity<String> entity, EnvSpec spec ->
            return new VxlanKvmAgentCommands.PopulateVxlanFdbResponse()
        }

        env.simulator(VxlanNetworkPoolConstant.VXLAN_KVM_POPULATE_FDB_L2VXLAN_NETWORKS_PATH) { HttpEntity<String> entity, EnvSpec spec ->
            return new VxlanKvmAgentCommands.PopulateVxlanNetworksFdbCmd()
        }
        env.simulator(VxlanNetworkPoolConstant.VXLAN_KVM_REALIZE_L2VXLAN_NETWORKS_PATH) { HttpEntity<String> entity, EnvSpec spec ->
            return new VxlanKvmAgentCommands.CreateVxlanBridgesCmd()
        }
        env.simulator(VxlanNetworkPoolConstant.VXLAN_KVM_DELETE_L2VXLAN_NETWORK_PATH) { HttpEntity<String> entity, EnvSpec spec ->
            return new VxlanKvmAgentCommands.DeleteVxlanBridgeCmd()
        }

    }
}
