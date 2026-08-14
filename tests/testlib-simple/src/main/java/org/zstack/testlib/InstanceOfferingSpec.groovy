package org.zstack.testlib

import org.zstack.sdk.InstanceOfferingInventory

/**
 * Created by xing5 on 2017/2/15.
 */
class InstanceOfferingSpec extends Spec implements HasSession {
    @SpecParam(required = true)
    String name
    @SpecParam
    String description
    @SpecParam
    Long memory
    @SpecParam
    Long cpu
    @SpecParam
    String allocatorStrategy

    public InstanceOfferingInventory inventory

    InstanceOfferingSpec(EnvSpec envSpec) {
        super(envSpec)
    }

    SpecID create(String uuid, String sessionId) {
        inventory = createInstanceOffering {
            delegate.resourceUuid = uuid
            delegate.name = name
            delegate.memorySize = memory
            delegate.cpuNum = cpu
            delegate.allocatorStrategy = allocatorStrategy
            delegate.userTags = userTags
            delegate.systemTags = systemTags
            delegate.sessionId = sessionId
        }

        postCreate {
            inventory = queryInstanceOffering {
                conditions=["uuid=${inventory.uuid}".toString()]
            }[0]
        }

        return id(name, inventory.uuid)
    }

    @Override
    void delete(String sessionId) {
        if (inventory != null) {
            deleteInstanceOffering {
                delegate.uuid = inventory.uuid
                delegate.sessionId = sessionId
            }

            inventory = null
        }
    }
}
