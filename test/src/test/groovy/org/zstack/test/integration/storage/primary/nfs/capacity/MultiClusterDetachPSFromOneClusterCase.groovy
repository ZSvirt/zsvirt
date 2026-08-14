package org.zstack.test.integration.storage.primary.nfs.capacity

import org.zstack.sdk.ClusterInventory
import org.zstack.sdk.PrimaryStorageInventory
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

/**
 * Created by lining on 2017/4/8.
 */
class MultiClusterDetachPSFromOneClusterCase extends SubCase{

    EnvSpec env

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void setup() {
        useSpring(StorageTest.springSpec)
    }

    @Override
    void environment() {
        env =  env {
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

                    attachPrimaryStorage("nfs")
                }

                cluster {
                    name = "cluster1"
                    hypervisorType = "KVM"

                    kvm {
                        name = "kvm1"
                        managementIp = "127.0.0.2"
                        username = "root"
                        password = "password"
                    }

                    attachPrimaryStorage("nfs")
                }

                nfsPrimaryStorage {
                    name = "nfs"
                    url = "localhost:/nfs"
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

    void testPSCapacityAfterDetachPS(){

        ClusterInventory cluster = env.inventoryByName("cluster")
        PrimaryStorageInventory ps = env.inventoryByName("nfs")

        // detach ps
        detachPrimaryStorageFromCluster {
            primaryStorageUuid = ps.uuid
            clusterUuid = cluster.uuid
        }

        // check PrimaryStorageCapacityVO capacity = 0
        boolean retryResult = retryInSecs(2) {
            ps = queryPrimaryStorage {
                conditions=["uuid=${ps.uuid}".toString()]
            }[0]
            return  0 == ps.availableCapacity
        }
        assert false == retryResult
        assert 0 != ps.availableCapacity
        assert 0 != ps.availablePhysicalCapacity
        assert 0 != ps.totalCapacity
        assert 0 != ps.totalPhysicalCapacity
    }
}