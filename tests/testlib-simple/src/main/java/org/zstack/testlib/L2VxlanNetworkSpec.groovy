package org.zstack.testlib

import org.zstack.sdk.L2NetworkInventory

/**
 * Created by weiwang on 15/03/2017.
 */
class L2VxlanNetworkSpec extends L2NetworkSpec {
    @SpecParam(required = true)
    Integer vni
    @SpecParam(required = true)
    String poolUuid;

    L2VxlanNetworkSpec(EnvSpec envSpec) {
        super(envSpec)
    }

    SpecID create(String uuid, String sessionId) {
        inventory = createL2VxlanNetwork {
            delegate.name = name
            delegate.description = description
            delegate.physicalInterface = physicalInterface
            delegate.poolUuid = (parent as L2VxlanNetworkPoolSpec).inventory.uuid
            delegate.vni = vni
            delegate.resourceUuid = uuid
            delegate.userTags = userTags
            delegate.systemTags = systemTags
            delegate.sessionId = sessionId
            delegate.zoneUuid = (parent as ZoneSpec).inventory.uuid
        } as L2NetworkInventory

        postCreate {
            inventory = queryL2VxlanNetwork {
                conditions=["uuid=${inventory.uuid}".toString()]
            }[0]
        }

        return id(name, inventory.uuid)
    }
}
