package org.zstack.test.integration.storage.snapshot

import org.springframework.http.HttpEntity
import org.zstack.core.db.Q
import org.zstack.header.storage.snapshot.VolumeSnapshotTreeVO
import org.zstack.header.storage.snapshot.VolumeSnapshotTreeVO_
import org.zstack.header.storage.snapshot.VolumeSnapshotVO
import org.zstack.kvm.KVMConstant
import org.zstack.sdk.VmInstanceInventory
import org.zstack.sdk.VolumeSnapshotInventory
import org.zstack.storage.primary.local.LocalStorageKvmBackend
import org.zstack.storage.snapshot.VolumeSnapshotGlobalConfig
import org.zstack.test.integration.ldap.Env
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
/**
 * Created by ads6 on 2018/1/10.
 */
class SnapshotMaxIncrementalCase extends SubCase {

    EnvSpec env
    VmInstanceInventory vm
    VolumeSnapshotInventory root
    VolumeSnapshotInventory rootLeaf
    VolumeSnapshotInventory root1
    VolumeSnapshotInventory root1Leaf

    final int MaxIncrementalSnapshot = 2

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
            VolumeSnapshotGlobalConfig.MAX_INCREMENTAL_SNAPSHOT_NUM.updateValue(MaxIncrementalSnapshot)
            testCreateSnapshotMaxIncremental()
            testDeleteLatestSnapshot()
            testDeleteRootSnapshot()
            testCreateDeleteMultiSnapshot()
            testCreateRevertSnapshot()
        }
    }

    void testCreateSnapshotMaxIncremental(){
        root = createSnapshot(vm.getRootVolumeUuid())

        rootLeaf = createSnapshot(vm.getRootVolumeUuid())
        assert rootLeaf.getParentUuid() == root.getUuid()
        VolumeSnapshotVO rootLeafVO = dbFindByUuid(rootLeaf.getUuid(), VolumeSnapshotVO.class)
        assert !rootLeafVO.isFullSnapshot()
        assert rootLeafVO.getDistance() == 2

        root1 = createSnapshot(vm.getRootVolumeUuid())
        assert root1.getParentUuid() == null
        VolumeSnapshotVO root1VO = dbFindByUuid(root1.getUuid(), VolumeSnapshotVO.class)
        assert root1VO.isFullSnapshot()
        assert root1VO.getDistance() == SnapshotTestConstant.ROOT_SNAPSHOT_DISNTANCE

        root1Leaf = createSnapshot(vm.getRootVolumeUuid())
        assert root1Leaf.getParentUuid() == root1.getUuid()
        VolumeSnapshotVO root1LeafVO = dbFindByUuid(root1Leaf.getUuid(), VolumeSnapshotVO.class)
        assert !root1LeafVO.isFullSnapshot()
        assert root1LeafVO.getDistance() == SnapshotTestConstant.ROOT_SNAPSHOT_DISNTANCE + 1
        assert root1LeafVO.isLatest()
        assert Q.New(VolumeSnapshotTreeVO.class).count() == 2
    }

    void testDeleteLatestSnapshot(){
        boolean deletePathInvoked = false
        env.afterSimulator(LocalStorageKvmBackend.DELETE_BITS_PATH) { rsp, HttpEntity<String> e ->
            deletePathInvoked = true
            return rsp
        }

        boolean mergePathInvoked = false
        env.afterSimulator(KVMConstant.KVM_MERGE_SNAPSHOT_PATH) { rsp, HttpEntity<String> e ->
            mergePathInvoked = true
            return rsp
        }

        deleteVolumeSnapshot { uuid = root1Leaf.uuid }
        VolumeSnapshotVO root1VO = dbFindByUuid(root1.getUuid(), VolumeSnapshotVO.class)
        assert root1VO.isLatest()
        assert Q.New(VolumeSnapshotVO.class).count() == 3

        assert deletePathInvoked && mergePathInvoked
    }

    void testDeleteRootSnapshot() {
        int deletePathInvokedCount = 0
        env.afterSimulator(LocalStorageKvmBackend.DELETE_BITS_PATH) { rsp, HttpEntity<String> e ->
            deletePathInvokedCount++
            return rsp
        }

        root1Leaf = createSnapshot(vm.getRootVolumeUuid())

        deleteVolumeSnapshot { uuid = root1.uuid }

        assert deletePathInvokedCount == 2
        assert Q.New(VolumeSnapshotTreeVO.class).count() == 1
        assert !Q.New(VolumeSnapshotTreeVO.class).eq(VolumeSnapshotTreeVO_.current,true).isExists()

        root1 = createSnapshot(vm.getRootVolumeUuid())
        assert Q.New(VolumeSnapshotTreeVO.class).count() == 2
        assert Q.New(VolumeSnapshotVO.class).count() == 3

        assert dbFindByUuid(root1.getUuid(), VolumeSnapshotVO.class).isLatest()
        assert dbFindByUuid(root1.getTreeUuid(), VolumeSnapshotTreeVO.class).isCurrent()
    }

    void testCreateDeleteMultiSnapshot() {
        def mergePathInvokedCount = 0
        env.afterSimulator(KVMConstant.KVM_MERGE_SNAPSHOT_PATH) { rsp, HttpEntity<String> e ->
            mergePathInvokedCount++
            return rsp
        }

        deleteVolumeSnapshot { uuid = root1.uuid }
        deleteVolumeSnapshot { uuid = root.uuid }
        assert Q.New(VolumeSnapshotVO.class).count() == 0


        List<String> uuids = new ArrayList<>()

        for (int i=0; i<100; i++){
            VolumeSnapshotInventory snapshot = createSnapshot(vm.getRootVolumeUuid())
            uuids.add(snapshot.uuid)
        }

        assert Q.New(VolumeSnapshotVO.class).count() == 100
        int expectedTreeCount = ((100 + 1) + MaxIncrementalSnapshot) / (MaxIncrementalSnapshot + 1)
        assert Q.New(VolumeSnapshotTreeVO.class).count() == expectedTreeCount

        def threads = []
        for (String suuid: uuids){
            def targetUuid = suuid
            def thread = Thread.start {
                deleteVolumeSnapshot {
                    uuid = targetUuid
                }
            }

            threads.add(thread)
        }

        threads.each { it -> it.join() }

        assert Q.New(VolumeSnapshotVO.class).count() == 0
        assert Q.New(VolumeSnapshotTreeVO.class).count() == 0
        assert mergePathInvokedCount > 1
    }

    void testCreateRevertSnapshot() {
        root = createSnapshot(vm.getRootVolumeUuid())
        rootLeaf = createSnapshot(vm.getRootVolumeUuid())
        root1 = createSnapshot(vm.getRootVolumeUuid())

        stopVmInstance { uuid = vm.uuid }

        revertVolumeFromSnapshot { uuid = rootLeaf.uuid }
        assert dbFindByUuid(rootLeaf.getUuid(), VolumeSnapshotVO.class).isLatest()
        assert dbFindByUuid(rootLeaf.getTreeUuid(), VolumeSnapshotTreeVO.class).isCurrent()

        VolumeSnapshotInventory root2 = createSnapshot(vm.getRootVolumeUuid())
        assert Q.New(VolumeSnapshotTreeVO.class).count() == 3
        assert dbFindByUuid(root2.getUuid(), VolumeSnapshotVO.class).isLatest()
        assert dbFindByUuid(root2.getTreeUuid(), VolumeSnapshotTreeVO.class).isCurrent()

        revertVolumeFromSnapshot { uuid = rootLeaf.uuid }
        assert dbFindByUuid(rootLeaf.getUuid(), VolumeSnapshotVO.class).isLatest()
        assert dbFindByUuid(rootLeaf.getTreeUuid(), VolumeSnapshotTreeVO.class).isCurrent()

        deleteVolumeSnapshot { uuid = rootLeaf.uuid }

        VolumeSnapshotInventory leaf1 = createSnapshot(vm.getRootVolumeUuid())
        assert dbFindByUuid(leaf1.getUuid(), VolumeSnapshotVO.class).isLatest()
        assert dbFindByUuid(leaf1.getTreeUuid(), VolumeSnapshotTreeVO.class).isCurrent()
    }

    private VolumeSnapshotInventory createSnapshot(String uuid){
        VolumeSnapshotInventory inv = createVolumeSnapshot {
            volumeUuid = uuid
            name = "test-snapshot"
        } as VolumeSnapshotInventory
        return inv
    }


}
