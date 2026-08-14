package org.zstack.testlib

import org.zstack.sdk.IpRangeInventory

/**
 * Created by shixin on 2017/2/15.
 */
class Ipv6RangeSpec extends Spec implements HasSession {
    @SpecParam
    String name = "ipv6-range"
    @SpecParam
    String description
    @SpecParam(required = true)
    String networkCidr
    @SpecParam(required = true)
    String addressMode
    @SpecParam
    List<String> systemTags

    IpRangeInventory inventory

    Ipv6RangeSpec(EnvSpec envSpec) {
        super(envSpec)
    }

    SpecID create(String uuid, String sessionId) {
        inventory = addIpv6RangeByNetworkCidr {
            delegate.resourceUuid = uuid
            delegate.name = name
            delegate.description = description
            delegate.networkCidr = networkCidr
            delegate.addressMode = addressMode
            delegate.sessionId = sessionId
            delegate.l3NetworkUuid = (parent as L3NetworkSpec).inventory.uuid
            delegate.systemTags = systemTags
        }

        postCreate {
            inventory = queryIpRange {
                conditions=["uuid=${inventory.uuid}".toString()]
            }[0]
        }

        return id(name, inventory.uuid)
    }

    @Override
    void delete(String sessionId) {
        if (inventory != null) {
            deleteIpRange {
                delegate.uuid = inventory.uuid
                delegate.sessionId = sessionId
            }

            inventory = null
        }
    }
}
