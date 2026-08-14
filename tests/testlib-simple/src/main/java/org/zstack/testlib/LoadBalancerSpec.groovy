package org.zstack.testlib

import org.zstack.sdk.LoadBalancerInventory
import org.zstack.sdk.VipInventory

/**
 * Created by xing5 on 2017/2/20.
 */
class LoadBalancerSpec extends Spec implements HasSession {
    @SpecParam(required = true)
    String name
    @SpecParam
    String description
    private Closure vip

    LoadBalancerInventory inventory

    LoadBalancerSpec(EnvSpec envSpec) {
        super(envSpec)
    }

    SpecID create(String uuid, String sessionId) {
        inventory = createLoadBalancer {
            delegate.resourceUuid = uuid
            delegate.name = name
            delegate.description = description
            delegate.vipUuid = vip(sessionId)
            delegate.userTags = userTags
            delegate.systemTags = systemTags
            delegate.sessionId = sessionId
        }

        postCreate {
            inventory = queryLoadBalancer {
                conditions=["uuid=${inventory.uuid}".toString()]
            }[0]
        }

        return id(name, inventory.uuid)
    }

    @SpecMethod
    void useVip(String vipL3NetworkName) {
        preCreate {
            addDependency(vipL3NetworkName, L3NetworkSpec.class)
        }

        vip = { String sessionId ->
            def l3 = findSpec(vipL3NetworkName, L3NetworkSpec.class) as L3NetworkSpec

            VipInventory inv = createVip {
                delegate.name = "vip-on-$vipL3NetworkName"
                delegate.l3NetworkUuid = l3.inventory.uuid
                delegate.requiredIp = requiredIp == null ? null : requiredIp
                delegate.sessionId = sessionId
            } as VipInventory

            return inv.uuid
        }
    }

    LoadBalancerListenerSpec listener(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = LoadBalancerListenerSpec.class) Closure c) {
        def spec = new LoadBalancerListenerSpec(envSpec)
        c.delegate = spec
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c()
        addChild(spec)
        return spec
    }

    @Override
    void delete(String sessionId) {
        if (inventory != null) {
            deleteLoadBalancer {
                delegate.uuid = inventory.uuid
                delegate.sessionId = sessionId
            }

            inventory = null
        }
    }
}
