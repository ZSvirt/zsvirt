package org.zstack.test.integration.storage.snapshot

import org.springframework.http.HttpEntity
import org.zstack.core.db.Q
import org.zstack.header.storage.primary.PrimaryStorageStateEvent
import org.zstack.header.storage.snapshot.VolumeSnapshotVO
import org.zstack.header.storage.snapshot.VolumeSnapshotVO_
import org.zstack.sdk.VmInstanceInventory
import org.zstack.sdk.VolumeSnapshotInventory
import org.zstack.storage.ceph.primary.CephPrimaryStorageBase
import org.zstack.test.integration.storage.CephEnv
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.gson.JSONObjectUtil

/**
 * Created by kayo on 2018/2/26.
 */
class VolumeSnapshotDeletionCase extends SubCase {
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
        env = CephEnv.CephStorageOneVmEnv()
    }

    @Override
    void test() {
        env.create {
            testMaintainPrimaryStorageDeletion()
        }
    }

    void testMaintainPrimaryStorageDeletion() {
        VmInstanceInventory vm = env.inventoryByName("test-vm") as VmInstanceInventory
        VolumeSnapshotInventory snapshot = createVolumeSnapshot {
            name = "test"
            volumeUuid = vm.getRootVolumeUuid()
        } as VolumeSnapshotInventory

        def psUuid = snapshot.primaryStorageUuid

        changePrimaryStorageState {
            uuid = psUuid
            stateEvent = PrimaryStorageStateEvent.maintain.toString()
        }

        def cmd = null
        env.afterSimulator(CephPrimaryStorageBase.DELETE_SNAPSHOT_PATH) { rsp, HttpEntity<String> e ->
            cmd = JSONObjectUtil.toObject(e.body, CephPrimaryStorageBase.DeleteSnapshotCmd.class)
            return rsp
        }

        detachPrimaryStorageFromCluster {
            primaryStorageUuid = psUuid
            clusterUuid = vm.getClusterUuid()
        }

        deletePrimaryStorage {
            uuid = psUuid
        }

        assert !Q.New(VolumeSnapshotVO.class).eq(VolumeSnapshotVO_.volumeUuid, vm.rootVolumeUuid).isExists()
        assert cmd == null
    }
}
