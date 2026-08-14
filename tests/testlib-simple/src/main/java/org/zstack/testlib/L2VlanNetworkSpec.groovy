package org.zstack.testlib

import org.springframework.http.HttpEntity
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMConstant
import org.zstack.sdk.L2NetworkInventory

/**
 * Created by xing5 on 2017/2/15.
 */
class L2VlanNetworkSpec extends L2NetworkSpec implements Simulator{
    @SpecParam(required = true)
    Integer vlan

    L2VlanNetworkSpec(EnvSpec envSpec) {
        super(envSpec)
    }

    SpecID create(String uuid, String sessionId) {
        inventory = createL2VlanNetwork {
            delegate.name = name
            delegate.description = description
            delegate.physicalInterface = physicalInterface
            delegate.resourceUuid = uuid
            delegate.vlan = vlan
            delegate.userTags = userTags
            delegate.systemTags = systemTags
            delegate.sessionId = sessionId
            delegate.zoneUuid = (parent as ZoneSpec).inventory.uuid
            delegate.vSwitchType = vSwitchType
        } as L2NetworkInventory

        postCreate {
            inventory = queryL2VlanNetwork {
                conditions=["uuid=${inventory.uuid}".toString()]
            }[0]
        }

        return id(name, inventory.uuid)
    }


    @Override
    void registerSimulators(EnvSpec env) {
        env.simulator(KVMConstant.KVM_DELETE_L2VLAN_NETWORK_PATH) { HttpEntity<String> entity, EnvSpec spec ->
            return  new KVMAgentCommands.DeleteVlanBridgeResponse()
        }
        env.simulator(KVMConstant.KVM_DELETE_OVSDPDK_NETWORK_PATH) { HttpEntity<String> entity, EnvSpec spec ->
            return new KVMAgentCommands.DeleteBridgeResponse()
        }
    }
}
