package org.zstack.testlib

import org.springframework.http.HttpEntity
import org.zstack.core.db.Q
import org.zstack.header.Constants
import org.zstack.header.host.HostVO
import org.zstack.header.host.HostVO_
import org.zstack.kvm.KVMConstant
import org.zstack.kvm.KVMAgentCommands
import org.zstack.utils.gson.JSONObjectUtil

/**
 * Created by xing5 on 2017/2/15.
 */
class L2NoVlanNetworkSpec extends L2NetworkSpec implements Simulator{
    L2NoVlanNetworkSpec(EnvSpec envSpec) {
        super(envSpec)
    }

    SpecID create(String uuid, String sessionId) {
        inventory = createL2NoVlanNetwork {
            delegate.name = name
            delegate.description = description
            delegate.resourceUuid = uuid
            delegate.sessionId = sessionId
            delegate.physicalInterface = physicalInterface
            delegate.userTags = userTags
            delegate.systemTags = systemTags
            delegate.zoneUuid = (parent as ZoneSpec).inventory.uuid
            delegate.vSwitchType = vSwitchType
        }

        postCreate {
            inventory = queryL2Network {
                conditions=["uuid=${inventory.uuid}".toString()]
            }[0]
        }

        return id(name, inventory.uuid)
    }


    @Override
    void registerSimulators(EnvSpec env) {
        env.simulator(KVMConstant.KVM_DELETE_L2NOVLAN_NETWORK_PATH) { HttpEntity<String> entity, EnvSpec spec ->
            return  new KVMAgentCommands.DeleteBridgeResponse()
        }
        env.simulator(KVMConstant.KVM_DELETE_OVSDPDK_NETWORK_PATH) { HttpEntity<String> entity, EnvSpec spec ->
            return new KVMAgentCommands.DeleteBridgeResponse()
        }
    }
}
