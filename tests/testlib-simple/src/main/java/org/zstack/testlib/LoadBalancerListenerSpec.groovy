package org.zstack.testlib

import org.zstack.sdk.LoadBalancerListenerInventory
import org.zstack.sdk.VmNicInventory

/**
 * Created by xing5 on 2017/2/20.
 */
class LoadBalancerListenerSpec extends Spec implements HasSession {
    @SpecParam(required = true)
    String name = "lb-listener"
    @SpecParam
    String description
    @SpecParam(required = true)
    String protocol
    @SpecParam(required = true)
    Integer loadBalancerPort
    @SpecParam(required = true)
    Integer instancePort
    private List<Closure> vmNics = []

    LoadBalancerListenerInventory inventory

    LoadBalancerListenerSpec(EnvSpec envSpec) {
        super(envSpec)
    }

    SpecID create(String uuid, String sessionId) {
        inventory = createLoadBalancerListener {
            delegate.resourceUuid = uuid
            delegate.name = name
            delegate.description = description
            delegate.protocol = protocol
            delegate.loadBalancerPort = loadBalancerPort
            delegate.instancePort = instancePort
            delegate.userTags = userTags
            delegate.systemTags = systemTags
            delegate.sessionId = sessionId
            delegate.loadBalancerUuid = (parent as LoadBalancerSpec).inventory.uuid
        }

        if (!vmNics.isEmpty()) {
            addVmNicToLoadBalancer {
                delegate.listenerUuid = inventory.uuid
                delegate.vmNicUuids = vmNics.collect { it() }
                delegate.sessionId = sessionId
            }
        }

        return id(name, inventory.uuid)
    }

    void useVmNic(String vmName, String l3NetworkName) {
        assert vmName != null: "vmName must be set when calling lb.listener.useVmNic()"
        assert l3NetworkName != null: "l3NetworkName must be set when calling lb.listener.useVmNic()"

        preCreate {
            addDependency(vmName, VmSpec.class)
            addDependency(l3NetworkName, L3NetworkSpec.class)
        }

        vmNics.add {
            VmSpec vm = findSpec(vmName, VmSpec.class)
            L3NetworkSpec l3 = findSpec(l3NetworkName, L3NetworkSpec.class)

            VmNicInventory nic = vm.inventory.vmNics.find { it.l3NetworkUuid == l3.inventory.uuid }
            assert nic!= null: "vm[$name] doesn't have nic on the l3 network[$l3NetworkName], check your environment()"

            return nic.uuid
        }
    }

    @Override
    void delete(String sessionId) {
        if (inventory != null) {
            deleteLoadBalancerListener {
                delegate.uuid = inventory.uuid
                delegate.sessionId = sessionId
            }

            inventory = null
        }
    }
}
