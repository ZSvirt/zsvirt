package org.zstack.testlib

import org.zstack.sdk.DiskOfferingInventory

/**
 * Created by xing5 on 2017/2/16.
 */
class DiskOfferingSpec extends Spec implements HasSession {
    @SpecParam(required = true)
    String name
    @SpecParam
    String description
    @SpecParam(required = true)
    Long diskSize
    @SpecParam
    String allocatorStrategy

    DiskOfferingInventory inventory

    DiskOfferingSpec(EnvSpec envSpec) {
        super(envSpec)
    }

    SpecID create(String uuid, String sessionId) {
        inventory = createDiskOffering {
            delegate.name = name
            delegate.description = description
            delegate.diskSize = diskSize
            delegate.systemTags = systemTags
            delegate.allocationStrategy = allocatorStrategy
            delegate.sessionId = sessionId
        }

        postCreate {
            inventory = queryDiskOffering {
                conditions=["uuid=${inventory.uuid}".toString()]
            }[0]
        }

        return id(name, inventory.uuid)
    }

    @Override
    void delete(String sessionId) {
        if (inventory != null) {
            deleteDiskOffering {
                delegate.uuid = inventory.uuid
                delegate.sessionId = sessionId
            }

            inventory = null
        }
    }
}
