package org.zstack.test.integration.storage.primary.local

import org.zstack.sdk.ClusterInventory
import org.zstack.sdk.GetPrimaryStorageCapacityResult
import org.zstack.header.host.HostVO
import org.zstack.sdk.HostInventory
import org.zstack.sdk.PrimaryStorageInventory
import org.zstack.test.integration.storage.Env
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

/**
 * Created by Quarkonics on 2017/4/20.
 */
class LocalStorageTwoPrimaryStorageCapacityCase extends SubCase {
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
        env = Env.localStorageOneVmEnvForTwoPrimaryStorage()
    }

    @Override
    void test() {
        env.create {
            testCalculationOfLSCapacityWhenDetachAndAttachLSPStoCluster1()
        }
    }

    void testCalculationOfLSCapacityWhenDetachAndAttachLSPStoCluster1() {
        PrimaryStorageInventory ps = env.inventoryByName("local")
        PrimaryStorageInventory ps2 = env.inventoryByName("local2")
        ClusterInventory cluster = env.inventoryByName("cluster")

        GetPrimaryStorageCapacityResult beforeCapacityResult1 = getPrimaryStorageCapacity {
            primaryStorageUuids = [ps.uuid]
        }

        GetPrimaryStorageCapacityResult beforeCapacityResult2 = getPrimaryStorageCapacity {
            primaryStorageUuids = [ps2.uuid]
        }

        HostInventory host = addKVMHost {
            name = "test"
            managementIp = "127.0.0.2"
            username = "root"
            password = "password"
            clusterUuid = cluster.uuid
        }
        assert dbFindByUuid(host.uuid, HostVO.class) != null

        GetPrimaryStorageCapacityResult capacityResult1 = getPrimaryStorageCapacity {
            primaryStorageUuids = [ps.uuid]
        }

        GetPrimaryStorageCapacityResult capacityResult2 = getPrimaryStorageCapacity {
            primaryStorageUuids = [ps2.uuid]
        }

        assert beforeCapacityResult1.availableCapacity < capacityResult1.availableCapacity
        assert beforeCapacityResult2.availableCapacity < capacityResult2.availableCapacity

        deleteHost {
            uuid = host.uuid
        }

        GetPrimaryStorageCapacityResult afterCapacityResult1 = getPrimaryStorageCapacity {
            primaryStorageUuids = [ps.uuid]
        }

        GetPrimaryStorageCapacityResult afterCapacityResult2 = getPrimaryStorageCapacity {
            primaryStorageUuids = [ps2.uuid]
        }

        retryInSecs(3){
            assert beforeCapacityResult1.availableCapacity == afterCapacityResult1.availableCapacity
            assert beforeCapacityResult2.availableCapacity == afterCapacityResult2.availableCapacity
        }
    }
}
