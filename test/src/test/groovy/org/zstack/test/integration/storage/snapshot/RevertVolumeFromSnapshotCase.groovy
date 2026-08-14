package org.zstack.test.integration.storage.snapshot

import org.zstack.core.db.Q
import org.zstack.core.trash.StorageTrash
import org.zstack.core.trash.TrashType
import org.zstack.header.core.trash.InstallPathRecycleInventory
import org.zstack.header.storage.snapshot.VolumeSnapshotTreeVO
import org.zstack.header.storage.snapshot.VolumeSnapshotVO
import org.zstack.header.storage.snapshot.VolumeSnapshotVO_
import org.zstack.sdk.VmInstanceInventory
import org.zstack.sdk.VolumeInventory
import org.zstack.sdk.VolumeSnapshotInventory
import org.zstack.storage.primary.local.LocalStorageResourceRefVO
import org.zstack.storage.primary.local.LocalStorageResourceRefVO_
import org.zstack.test.integration.ldap.Env
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
/**
 * Created by ads6 on 2018/1/2.
 */
class RevertVolumeFromSnapshotCase extends SubCase{
    def DOC = """
STEP:
1. Create 3 snapshot on root 
2. revert first snapshot on volume
3. create 2 more snapshot 
"""

    EnvSpec env
    VmInstanceInventory vm
    VolumeInventory root

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
        env = Env.localStorageOneVmEnv()
    }

    @Override
    void test() {
        env.create {
            vm = env.inventoryByName("vm") as VmInstanceInventory
            testRevertVolumeFromSnapshot()
            testCleanUpTrash()
        }
    }

    void testRevertVolumeFromSnapshot(){
        def s1 = createSnapshot(vm.rootVolumeUuid) as VolumeSnapshotInventory
        def s2 = createSnapshot(vm.rootVolumeUuid) as VolumeSnapshotInventory
        def s3 = createSnapshot(vm.rootVolumeUuid) as VolumeSnapshotInventory

        stopVmInstance {
            uuid = vm.uuid
        }

        root = queryVolume {
            conditions = ["uuid=${vm.rootVolumeUuid}"]
        }[0] as VolumeInventory

        def installPath = root.installPath
        def size = root.size

        revertVolumeFromSnapshot {
            uuid = s1.uuid
        }

        assert Q.New(VolumeSnapshotVO.class).select(VolumeSnapshotVO_.latest).eq(VolumeSnapshotVO_.uuid, s1.uuid).findValue()
        assert Q.New(VolumeSnapshotVO.class).count() == 3

        def s4 =  createSnapshot(vm.rootVolumeUuid) as VolumeSnapshotInventory
        def s5 = createSnapshot(vm.rootVolumeUuid) as VolumeSnapshotInventory

        VolumeSnapshotVO s1vo = Q.New(VolumeSnapshotVO.class).eq(VolumeSnapshotVO_.uuid, s4.uuid).find()
        VolumeSnapshotVO s2vo = Q.New(VolumeSnapshotVO.class).eq(VolumeSnapshotVO_.uuid, s5.uuid).find()
        assert s1vo.distance == 2
        assert s2vo.distance == 3
        assert s2vo.parentUuid == s1vo.uuid
        assert s1vo.parentUuid == s1.uuid
        assert !s1vo.latest
        assert s2vo.latest
        assert Q.New(VolumeSnapshotVO.class).eq(VolumeSnapshotVO_.latest, true).count() == 1
        assert Q.New(VolumeSnapshotTreeVO.class).count() == 1

        def trash = bean(StorageTrash.class)
        def trashs = trash.getTrashList(root.primaryStorageUuid) as List<InstallPathRecycleInventory>

        assert trashs.size() > 0

        def trashed = false
        trashs.each { t ->
            if (t.resourceUuid == vm.rootVolumeUuid) {
                assert t.installPath == installPath
                assert t.size == size + 1
                assert t.trashType == TrashType.RevertVolume.toString()
                trashed = true
            }
        }
        assert trashed
    }

    void testCleanUpTrash() {
        def trash = bean(StorageTrash.class)
        def trashs = trash.getTrashList(root.primaryStorageUuid) as List<InstallPathRecycleInventory>
        assert trashs.size() > 0

        cleanUpTrashOnPrimaryStorage {
            uuid = root.primaryStorageUuid
        }

        trashs = trash.getTrashList(root.primaryStorageUuid) as List<InstallPathRecycleInventory>
        assert trashs.size() == 0
        assert Q.New(LocalStorageResourceRefVO.class).eq(LocalStorageResourceRefVO_.resourceUuid, root.uuid).exists
    }

    private VolumeSnapshotInventory createSnapshot(String uuid){
        VolumeSnapshotInventory inv = createVolumeSnapshot {
            volumeUuid = uuid
            name = "test-snapshot"
        } as VolumeSnapshotInventory
        return inv
    }
}
