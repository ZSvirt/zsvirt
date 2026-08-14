package org.zstack.test.integration.storage.primary.ceph


import org.zstack.core.db.DatabaseFacade
import org.zstack.sdk.CephBackupStorageInventory
import org.zstack.sdk.CephPrimaryStorageInventory
import org.zstack.sdk.ClusterInventory
import org.zstack.storage.ceph.CephCapacityVO
import org.zstack.test.integration.storage.CephEnv
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
/**
 * Created by Administrator on 2017-03-20.
 */

class DeleteCephCapacityVOCase extends SubCase{
    EnvSpec env


    @Override
    void setup() {
        useSpring(StorageTest.springSpec)
    }

    @Override
    void environment() {
        env = CephEnv.CephStorageOneVmEnv()
    }

    @Override
    void test() {
        env.create {
            testCephDeleteAllStorageWillDeleteCapacity()
        }
    }

    @Override
    void clean() {
        env.delete()
    }

    void testCephDeleteAllStorageWillDeleteCapacity(){
        ClusterInventory cluInv = env.inventoryByName("test-cluster") as ClusterInventory
        CephPrimaryStorageInventory psInv = env.inventoryByName("ceph-pri") as CephPrimaryStorageInventory
        CephBackupStorageInventory bkInv = env.inventoryByName("ceph-bk") as CephBackupStorageInventory
        DatabaseFacade dbf = bean(DatabaseFacade.class)
        assert dbf.findByUuid(bkInv.getFsid(),CephCapacityVO.class)

        deleteBackupStorage {
            uuid = bkInv.uuid
            sessionId = currentEnvSpec.session.uuid
        }

        assert dbf.findByUuid(bkInv.getFsid(),CephCapacityVO.class)

        detachPrimaryStorageFromCluster {
            primaryStorageUuid = psInv.uuid
            clusterUuid = cluInv.uuid
        }
        deletePrimaryStorage {
            uuid = psInv.uuid
            sessionId = currentEnvSpec.session.uuid
        }

        deleteBackupStorage {
            uuid = bkInv.uuid
            sessionId = currentEnvSpec.session.uuid
        }

        assert !dbf.isExist(bkInv.getFsid(), CephCapacityVO.class)
    }
}
