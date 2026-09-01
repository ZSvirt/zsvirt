package org.zstack.test.integration.identity.resource

import org.zstack.header.cluster.ClusterVO
import org.zstack.header.zone.ZoneVO
import org.zstack.sdk.ClusterInventory
import org.zstack.sdk.ResourceInventory
import org.zstack.sdk.ZoneInventory
import org.zstack.test.integration.identity.IdentityTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

/**
 * Created by xing5 on 2017/5/1.
 */
class ResourceCase extends SubCase {
    EnvSpec envSpec

    @Override
    void clean() {
        envSpec.delete()
    }

    @Override
    void setup() {
        useSpring(IdentityTest.springSpec)
    }

    @Override
    void environment() {
        envSpec = env {
            zone {
                name = "zone"

                cluster {
                    name = "cluster"
                }
            }
        }
    }

    void testUpdateResourceName() {
        ZoneInventory zone = envSpec.inventoryByName("zone")
        ClusterInventory cluster = envSpec.inventoryByName("cluster")

        updateZone {
            name = "thisZone"
            uuid = zone.uuid
        }

        updateCluster {
            name = "thisCluster"
            uuid = cluster.uuid
        }

        List<ResourceInventory> invs = getResourceNames {
            uuids = [zone.uuid, cluster.uuid]
        }

        ResourceInventory zinv = invs.find { it.uuid == zone.uuid }
        assert zinv.resourceName == "thisZone"
        ResourceInventory cinv = invs.find { it.uuid == cluster.uuid }
        assert cinv.resourceName == "thisCluster"
    }

    void testQueryResource() {
        ZoneInventory zone = envSpec.inventoryByName("zone")
        ClusterInventory cluster = envSpec.inventoryByName("cluster")

        List<ResourceInventory> invs = getResourceNames {
            uuids=[zone.uuid,cluster.uuid]
        }

        ResourceInventory z = invs.find { it.uuid == zone.uuid }
        assert z.resourceName == zone.name
        assert z.resourceType == ZoneVO.class.simpleName

        ResourceInventory c = invs.find { it.uuid == cluster.uuid }
        assert c.resourceName == cluster.name
        assert c.resourceType == ClusterVO.class.simpleName
    }

    @Override
    void test() {
        envSpec.create {
            testQueryResource()
            testUpdateResourceName()
        }
    }
}
