package org.zstack.test.integration.storage.volume

import org.zstack.core.db.Q
import org.zstack.header.storage.snapshot.VolumeSnapshotTreeVO
import org.zstack.header.storage.snapshot.VolumeSnapshotTreeVO_
import org.zstack.header.volume.VolumeVO
import org.zstack.sdk.ImageInventory
import org.zstack.sdk.InstanceOfferingInventory
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.storage.snapshot.VolumeSnapshotGlobalConfig
import org.zstack.storage.volume.VolumeSystemTags
import org.zstack.test.integration.storage.Env
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
/**
 * Created by MaJin on 2017-10-19.
 */
class PerVolumeSnapshotMaxNumCase extends SubCase{
    EnvSpec env
    VmInstanceInventory vm1, vm2

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
            vm1 = env.inventoryByName("vm") as VmInstanceInventory
            testSnapshotGlobalConfig()
            testVolumeSnapshotMaxNumSystemTag()
            testNoSystemTagCreateSnapshot()
            testPerVolumeSystemTagCreateSnapshot()
            testAddVolumeSystemTagAfterCreateSnapshot()
            testVolumeSnapshotTree()
        }
    }

    @Override
    void clean() {
        env.delete()
    }

    void testSnapshotGlobalConfig(){
        updateGlobalConfig {
            category = VolumeSnapshotGlobalConfig.CATEGORY
            name = VolumeSnapshotGlobalConfig.MAX_INCREMENTAL_SNAPSHOT_NUM.name
            value = "2"
            sessionId = adminSession()
        }
        assert VolumeSnapshotGlobalConfig.MAX_INCREMENTAL_SNAPSHOT_NUM.value() == "2"
    }

    void testVolumeSnapshotMaxNumSystemTag(){
        createSystemTag {
            resourceType = VolumeVO.getSimpleName()
            resourceUuid = vm1.rootVolumeUuid
            tag = "volumeMaxIncrementalSnapshotNum::1"
        }
        assert VolumeSystemTags.VOLUME_MAX_INCREMENTAL_SNAPSHOT_NUM.getTokenByResourceUuid(vm1.rootVolumeUuid,
                VolumeSystemTags.VOLUME_MAX_INCREMENTAL_SNAPSHOT_NUM_TOKEN) == "1"
    }

    void testNoSystemTagCreateSnapshot(){
        def instanceOfferingInv = env.inventoryByName("instanceOffering") as InstanceOfferingInventory
        def imageInv = env.inventoryByName("image1") as ImageInventory
        def l3Inv = env.inventoryByName("l3") as L3NetworkInventory

        vm2 = createVmInstance {
            name = "vm2"
            instanceOfferingUuid = instanceOfferingInv.uuid
            imageUuid = imageInv.uuid
            l3NetworkUuids = [l3Inv.uuid]
        } as VmInstanceInventory

        // MaxNum for vm2 is not set, global config is 2
        assert Q.New(VolumeSnapshotTreeVO.class).count() == 0

        createSnapshots(vm2.rootVolumeUuid, 2)

        assert Q.New(VolumeSnapshotTreeVO.class).count() == 1

        createSnapshots(vm2.rootVolumeUuid, 1)

        assert Q.New(VolumeSnapshotTreeVO.class).count() == 2
    }

    void testPerVolumeSystemTagCreateSnapshot(){
        // MaxNum for vm1 is 1, global config is 2
        int nowCount = Q.New(VolumeSnapshotTreeVO.class).count()

        createSnapshots(vm1.rootVolumeUuid, 2)

        assert Q.New(VolumeSnapshotTreeVO.class).count() == nowCount + 2
    }

    void testAddVolumeSystemTagAfterCreateSnapshot(){
        int nowCount = Q.New(VolumeSnapshotTreeVO.class).count()

        createSnapshots(vm2.rootVolumeUuid, 1)

        assert Q.New(VolumeSnapshotTreeVO.class).count() == nowCount

        createSystemTag {
            resourceType = VolumeVO.getSimpleName()
            resourceUuid = vm2.rootVolumeUuid
            tag = "volumeMaxIncrementalSnapshotNum::1"
        }

        createSnapshots(vm2.rootVolumeUuid, 1)

        assert Q.New(VolumeSnapshotTreeVO.class).count() == nowCount + 1
    }

    void testVolumeSnapshotTree() {
        def instanceOfferingInv = env.inventoryByName("instanceOffering") as InstanceOfferingInventory
        def imageInv = env.inventoryByName("image1") as ImageInventory
        def l3Inv = env.inventoryByName("l3") as L3NetworkInventory
        def vm3 = createVmInstance {
            name = "vm3"
            instanceOfferingUuid = instanceOfferingInv.uuid
            imageUuid = imageInv.uuid
            l3NetworkUuids = [l3Inv.uuid]
        } as VmInstanceInventory

        assert getVolumeTreeCount(vm3.rootVolumeUuid) == 0

        createSystemTag {
            resourceType = VolumeVO.getSimpleName()
            resourceUuid = vm3.rootVolumeUuid
            tag = "volumeMaxIncrementalSnapshotNum::1"
        }

        createSnapshots(vm3.rootVolumeUuid, 1)
        assert getVolumeTreeCount(vm3.rootVolumeUuid) == 1

        createSnapshots(vm3.rootVolumeUuid, 1)
        assert getVolumeTreeCount(vm3.rootVolumeUuid) == 2

        createSnapshots(vm3.rootVolumeUuid, 1)
        assert getVolumeTreeCount(vm3.rootVolumeUuid) == 2

        createSnapshots(vm3.rootVolumeUuid, 1)
        assert getVolumeTreeCount(vm3.rootVolumeUuid) == 3
    }

    private static Long getVolumeTreeCount(String volUuid) {
        return Q.New(VolumeSnapshotTreeVO.class)
                .eq(VolumeSnapshotTreeVO_.volumeUuid, volUuid)
                .count()
    }

    private void createSnapshots(String volUuid, int count){
        for (int i = 0; i < count; i++) {
            createVolumeSnapshot {
                volumeUuid = volUuid
                name = "testVmSnapshot"
            }
        }
    }
}
