package org.zstack.test.integration.storage.primary.smp.capacity

import org.zstack.sdk.ClusterInventory
import org.zstack.sdk.PrimaryStorageInventory
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.*

/**
 * Created by lining on 2017/4/10.
 */
class DetachPSFromClusterCase extends SubCase{
    EnvSpec env

    @Override
    void setup() {
        useSpring(StorageTest.springSpec)
    }

    @Override
    void environment() {
        env = env{
            zone {
                name = "zone"
                description = "test"

                cluster {
                    name = "cluster"
                    hypervisorType = "KVM"

                    kvm {
                        name = "kvm"
                        managementIp = "localhost"
                        username = "root"
                        password = "password"
                    }

                    attachPrimaryStorage("smp")
                }

                smpPrimaryStorage {
                    name = "smp"
                    url = "/test"
                }

            }
        }

    }

    @Override
    void test() {
        env.create {
            testPSCapacityAfterDetachPS()
        }
    }

    private void testPSCapacityAfterDetachPS() {
        ClusterInventory cluster = env.inventoryByName("cluster")
        PrimaryStorageInventory ps = env.inventoryByName("smp")

        // check ps capacity > 0
        assert 0 < ps.availableCapacity
        assert 0 < ps.availablePhysicalCapacity
        assert 0 < ps.totalCapacity
        assert 0 < ps.totalPhysicalCapacity

        // detach ps
        detachPrimaryStorageFromCluster {
            primaryStorageUuid = ps.uuid
            clusterUuid = cluster.uuid
        }

        // check PrimaryStorageCapacityVO capacity = 0
        retryInSecs(2) {
            ps = queryPrimaryStorage {
                conditions=["uuid=${ps.uuid}".toString()]
            }[0]
            assert 0 == ps.availableCapacity
            assert 0 == ps.availablePhysicalCapacity
            assert 0 == ps.totalCapacity
            assert 0 == ps.totalPhysicalCapacity
        }
    }

    @Override
    void clean() {
        env.delete()
    }
}
