package org.zstack.test.integration.storage.snapshot

import org.springframework.http.HttpEntity
import org.zstack.core.db.Q
import org.zstack.header.identity.AccessLevel
import org.zstack.header.identity.AccountResourceRefVO
import org.zstack.header.identity.AccountResourceRefVO_
import org.zstack.header.storage.snapshot.VolumeSnapshotTreeVO
import org.zstack.header.storage.snapshot.VolumeSnapshotTreeVO_
import org.zstack.header.storage.snapshot.VolumeSnapshotVO
import org.zstack.header.storage.snapshot.VolumeSnapshotVO_
import org.zstack.header.volume.VolumeVO
import org.zstack.header.volume.VolumeVO_
import org.zstack.sdk.VmInstanceInventory
import org.zstack.sdk.VolumeSnapshotInventory
import org.zstack.storage.snapshot.VolumeSnapshot
import org.zstack.test.integration.ldap.Env
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.kvm.KVMConstant
import org.zstack.storage.primary.local.LocalStorageKvmBackend
import org.zstack.testlib.HttpError
import org.zstack.simulator.kvm.VolumeSnapshotKvmSimulator
import org.zstack.core.db.DatabaseFacade

/**
 * Created by ads6 on 2018/1/2.
 */
class CreateSnapshotFailureCase extends SubCase{

    EnvSpec env
    VmInstanceInventory vm
    VolumeSnapshotInventory root
    VolumeSnapshotInventory Leaf1
    VolumeSnapshotInventory Leaf2


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
            testCreateSnapshotFailure()
        }
    }

    void testCreateSnapshotFailure() {
        root = createSnapshot(vm.getRootVolumeUuid())
        Leaf1 = createSnapshot(vm.getRootVolumeUuid())
        Leaf2 = createSnapshot(vm.getRootVolumeUuid())

        env.simulator(KVMConstant.KVM_TAKE_VOLUME_SNAPSHOT_PATH) { rsp, HttpEntity<String> e ->
            rsp.success = false
            return rsp
        }

        expect(AssertionError) {
            createSnapshot(vm.getRootVolumeUuid())
        }

        VolumeSnapshotVO leaf2vo = dbFindByUuid(Leaf2.getUuid(), VolumeSnapshotVO.class)

        assert Q.New(VolumeSnapshotVO.class).count() == 3
        assert leaf2vo.isLatest()

        ArrayList<String> uuids = Q.New(VolumeSnapshotVO.class).select(VolumeSnapshotVO_.uuid).listValues()
        assert !Q.New(AccountResourceRefVO.class)
                .eq(AccountResourceRefVO_.resourceType, VolumeSnapshotVO.class.simpleName)
                .eq(AccountResourceRefVO_.type, AccessLevel.Own)
                .notIn(AccountResourceRefVO_.resourceUuid, uuids)
                .isExists()

        deleteVolumeSnapshot {
            uuid = root.getUuid()
        }
    }


    private VolumeSnapshotInventory createSnapshot(String uuid){
        VolumeSnapshotInventory inv = createVolumeSnapshot {
            volumeUuid = uuid
            name = "test-snapshot"
        } as VolumeSnapshotInventory
        return inv
    }
}
