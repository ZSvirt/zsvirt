package org.zstack.test.integration.storage.primary.nfs.capacity

import org.zstack.sdk.ClusterInventory
import org.zstack.sdk.PrimaryStorageInventory
import org.zstack.test.integration.storage.Env
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

/**
 * Created by lining on 2017/4/8.
 */
class DetachPSFromClusterCase extends SubCase{

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
        env = Env.nfsOneVmEnv()
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
}