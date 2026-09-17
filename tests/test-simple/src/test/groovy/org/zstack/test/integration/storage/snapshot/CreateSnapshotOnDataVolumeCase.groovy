package org.zstack.test.integration.storage.snapshot

import org.springframework.http.HttpEntity
import org.zstack.core.db.Q
import org.zstack.header.storage.primary.PrimaryStorageCapacityVO
import org.zstack.header.storage.primary.PrimaryStorageCapacityVO_
import org.zstack.header.storage.snapshot.VolumeSnapshotTreeVO
import org.zstack.header.storage.snapshot.VolumeSnapshotTreeVO_
import org.zstack.header.storage.snapshot.VolumeSnapshotVO
import org.zstack.header.storage.snapshot.VolumeSnapshotVO_
import org.zstack.header.volume.VolumeVO
import org.zstack.header.volume.VolumeVO_
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMConstant
import org.zstack.sdk.DiskOfferingInventory
import org.zstack.sdk.KVMHostInventory
import org.zstack.sdk.PrimaryStorageInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.sdk.VolumeInventory
import org.zstack.sdk.VolumeSnapshotInventory
import org.zstack.storage.primary.local.LocalStorageHostRefVO
import org.zstack.storage.primary.local.LocalStorageHostRefVO_
import org.zstack.test.integration.storage.Env
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit
import org.zstack.utils.gson.JSONObjectUtil

/**
 * Created by ads6 on 2018/1/2.
 */
class CreateSnapshotOnDataVolumeCase extends SubCase{

    EnvSpec env
    VmInstanceInventory vm
    String dataVolUuid
    boolean dataVolAttached

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
        env = Env.localStorageOneVmWithOneDataVolumeEnv()
    }

    @Override
    void test() {
        env.create {
            vm = env.inventoryByName("vm") as VmInstanceInventory
            dataVolUuid = vm.allVolumes.find{ it.uuid != vm.rootVolumeUuid }.uuid
            dataVolAttached = true
            testCreateSnapshotOnDataVolume()
            dataVolAttached = false
            testCreateSnapshotOnDataVolume()
            testCreateVolumeSnapshotAndCheckPSCapacity()
        }
    }

    void testCreateSnapshotOnDataVolume(){
        if (!dataVolAttached){ detachDataVolumeFromVm { uuid = dataVolUuid }}

        def snapshotNum = 5

        List<VolumeSnapshotInventory> snapshotInvList = new ArrayList<>()
        for (int i=0; i<snapshotNum; i++){
            VolumeSnapshotInventory snapshotInv = createSnapshot(dataVolUuid)
            snapshotInvList.add(snapshotInv)
        }

        assert env.verifySimulator(KVMConstant.KVM_TAKE_VOLUME_SNAPSHOT_PATH, snapshotNum)
        assert Q.New(VolumeSnapshotVO.class).count() == snapshotNum

        firstSnapshot(snapshotInvList[0])

        for (int i=1; i<snapshotNum-1; i++){
            deltaSnapshot(snapshotInvList[i], i+1)
        }

        lastSnapshot(snapshotInvList[snapshotNum-1], snapshotNum)

        assert Q.New(VolumeSnapshotTreeVO.class).eq(VolumeSnapshotTreeVO_.uuid, snapshotInvList[0].getTreeUuid()).isExists()
        assert Q.New(VolumeSnapshotTreeVO.class).count() == 1


        deleteVolumeSnapshot {
            uuid = snapshotInvList[3].uuid
        }

        retryInSecs{
            assert dbFindByUuid(snapshotInvList[3].uuid, VolumeSnapshotVO.class) == null
            assert dbFindByUuid(snapshotInvList[4].uuid, VolumeSnapshotVO.class) == null
            assert Q.New(VolumeSnapshotVO.class).count() == 3
            assert Q.New(VolumeSnapshotTreeVO.class).count() == 1
        }

        deleteVolumeSnapshot {
            uuid = snapshotInvList[0].uuid
        }

        retryInSecs {
            assert Q.New(VolumeSnapshotVO.class).count() == 0
        }

        if (!dataVolAttached) {
            attachDataVolumeToVm {
                vmInstanceUuid = vm.uuid
                volumeUuid = dataVolUuid
            }
        }

        env.resetAllSimulatorSize()
    }


    void firstSnapshot(VolumeSnapshotInventory snapshotInv){
        VolumeVO volumeVO = Q.New(VolumeVO.class).eq(VolumeVO_.uuid, vm.getRootVolumeUuid()).find()
        VolumeSnapshotVO snapshotVO =Q.New(VolumeSnapshotVO.class).eq(VolumeSnapshotVO_.uuid, snapshotInv.getUuid()).find()
        assert snapshotVO.isFullSnapshot() == false
        assert snapshotVO.isLatest() == false
        assert snapshotVO.getParentUuid() == null
        assert snapshotVO.getDistance() == 1
        assert snapshotVO.getPrimaryStorageUuid() == volumeVO.getPrimaryStorageUuid()
        assert snapshotVO.getPrimaryStorageInstallPath() != null

    }

    void lastSnapshot(VolumeSnapshotInventory snapshotInv, int distance) {
        VolumeVO volumeVO = Q.New(VolumeVO.class).eq(VolumeVO_.uuid, vm.getRootVolumeUuid()).find()
        VolumeSnapshotVO snapshotVO = Q.New(VolumeSnapshotVO.class).eq(VolumeSnapshotVO_.uuid, snapshotInv.getUuid()).find()
        assert snapshotVO.isFullSnapshot() == false
        assert snapshotVO.isLatest()
        assert snapshotVO.getParentUuid()  != null
        assert snapshotVO.getDistance() == distance
        assert snapshotVO.getPrimaryStorageUuid() == volumeVO.getPrimaryStorageUuid()
        assert snapshotVO.getPrimaryStorageInstallPath() != null
    }


    void deltaSnapshot(VolumeSnapshotInventory snapshotInv, int distance){
        VolumeVO volumeVO = Q.New(VolumeVO.class).eq(VolumeVO_.uuid, vm.getRootVolumeUuid()).find()
        VolumeSnapshotVO snapshotVO =Q.New(VolumeSnapshotVO.class).eq(VolumeSnapshotVO_.uuid, snapshotInv.getUuid()).find()
        assert snapshotVO.isFullSnapshot() == false
        assert snapshotVO.isLatest() == false
        assert snapshotVO.getParentUuid() != null
        assert snapshotVO.getDistance() == distance
        assert snapshotVO.getPrimaryStorageUuid() == volumeVO.getPrimaryStorageUuid()
        assert snapshotVO.getPrimaryStorageInstallPath() != null

    }

    private VolumeSnapshotInventory createSnapshot(String uuid){
        VolumeSnapshotInventory inv = createVolumeSnapshot {
            volumeUuid = uuid
            name = "test-snapshot"
        } as VolumeSnapshotInventory
        return inv
    }

    void testCreateVolumeSnapshotAndCheckPSCapacity() {
        def local = env.inventoryByName("local") as PrimaryStorageInventory
        def kvm = env.inventoryByName("kvm") as KVMHostInventory

        long volumeSize = SizeUnit.TERABYTE.toByte(50)
        DiskOfferingInventory diskOffering = createDiskOffering {
            name = "data"
            diskSize = volumeSize
        } as DiskOfferingInventory

        VolumeInventory dataVolume = createDataVolume {
            name = "50T"
            diskOfferingUuid = diskOffering.uuid
            primaryStorageUuid = local.uuid
            systemTags = Arrays.asList("localStorage::hostUuid::" + kvm.uuid)
        } as VolumeInventory

        def beforeCreateVolumePSAvailableCapacity = Q.New(PrimaryStorageCapacityVO.class)
                .select(PrimaryStorageCapacityVO_.availableCapacity)
                .eq(PrimaryStorageCapacityVO_.uuid, local.uuid)
                .findValue()
        def beforeCreateVolumeHostAvailableCapacity = Q.New(LocalStorageHostRefVO.class)
                .eq(LocalStorageHostRefVO_.hostUuid, kvm.uuid)
                .select(LocalStorageHostRefVO_.availableCapacity)
                .findValue()

        env.simulator(KVMConstant.KVM_TAKE_VOLUME_SNAPSHOT_PATH) { HttpEntity<String> e, EnvSpec espec ->
            KVMAgentCommands.TakeSnapshotCmd cmd = JSONObjectUtil.toObject(e.getBody(), KVMAgentCommands.TakeSnapshotCmd.class)
            def rsp = new KVMAgentCommands.TakeSnapshotResponse()
            rsp.newVolumeInstallPath = cmd.installPath
            rsp.snapshotInstallPath = cmd.volumeInstallPath
            rsp.size = SizeUnit.TERABYTE.toByte(100)
            return rsp
        }

        createVolumeSnapshot {
            name = "vol-snapshot"
            volumeUuid = dataVolume.uuid
        }

        def afterCreateVolumePSAvailableCapacity = Q.New(PrimaryStorageCapacityVO.class)
                .select(PrimaryStorageCapacityVO_.availableCapacity)
                .eq(PrimaryStorageCapacityVO_.uuid, local.uuid)
                .findValue()
        def afterCreateVolumeHostAvailableCapacity = Q.New(LocalStorageHostRefVO.class)
                .eq(LocalStorageHostRefVO_.hostUuid, kvm.uuid)
                .select(LocalStorageHostRefVO_.availableCapacity)
                .findValue()

        retryInSecs {
            assert afterCreateVolumePSAvailableCapacity == beforeCreateVolumePSAvailableCapacity - beforeCreateVolumeHostAvailableCapacity
            assert afterCreateVolumeHostAvailableCapacity == 0
        }
    }
}
