package org.zstack.test.integration.storage.primary.local.capacity

import org.zstack.compute.vm.VmGlobalConfig
import org.zstack.core.db.Q
import org.zstack.header.image.ImageConstant
import org.zstack.header.vm.VmInstanceDeletionPolicyManager.VmInstanceDeletionPolicy
import org.zstack.header.vm.VmInstanceState
import org.zstack.header.vm.VmInstanceVO
import org.zstack.sdk.BackupStorageInventory
import org.zstack.sdk.GetPrimaryStorageCapacityResult
import org.zstack.sdk.HostDiskCapacity
import org.zstack.sdk.HostInventory
import org.zstack.sdk.ImageInventory
import org.zstack.sdk.InstanceOfferingInventory
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.PrimaryStorageInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.sdk.VolumeInventory
import org.zstack.storage.primary.local.LocalStorageHostRefVO
import org.zstack.storage.primary.local.LocalStorageHostRefVO_
import org.zstack.storage.primary.local.LocalStorageKvmBackend
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit
/**
 * Created by SyZhao on 2017/4/21.
 */
class LocalStorageExpungeVmByImageReconnectCapacityCase extends SubCase {
    EnvSpec env
    PrimaryStorageInventory ps
    BackupStorageInventory bs
    ImageInventory image
    VmInstanceInventory vm
    L3NetworkInventory l3
    InstanceOfferingInventory offering
    HostInventory host

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
        env = LocalStorageEnv.localStorageOneVmEnvForCapacity()
    }

    @Override
    void test() {
        env.create {
            prepare()
            testExpungeVmByImageReconnectCheckCapacity()
        }
    }

    void prepare() {
        LocalStorageEnv.simulator(env)

        bs = env.inventoryByName("sftp") as BackupStorageInventory
        ps = env.inventoryByName("local") as PrimaryStorageInventory
        l3 = env.inventoryByName("l3") as L3NetworkInventory
        offering = env.inventoryByName("instanceOffering") as InstanceOfferingInventory
        host = env.inventoryByName("kvm") as HostInventory

        image = addImage {
            name = "image1"
            url = "http://zstack.org/download/test.qcow2"
            backupStorageUuids = [bs.uuid]
            format = ImageConstant.QCOW2_FORMAT_STRING
        } as ImageInventory

        vm = createVmInstance {
            name = "test-vm"
            imageUuid = image.uuid
            l3NetworkUuids = [l3.uuid]
            instanceOfferingUuid = offering.uuid
            hostUuid = host.uuid
        } as VmInstanceInventory

        reconnectHost {
            uuid = vm.hostUuid
        }
        reconnectPrimaryStorage {
            uuid = ps.uuid
        }
    }

    void testExpungeVmByImageReconnectCheckCapacity() {
        VmGlobalConfig.VM_DELETION_POLICY.updateValue(VmInstanceDeletionPolicy.Delay.toString())
        assert vm.rootVolumeUuid

        def volume = queryVolume {
            conditions=["uuid=${vm.rootVolumeUuid}"]
        }[0] as VolumeInventory

        assert volume.size > 0

        LocalStorageHostRefVO beforeRefVO = Q.New(LocalStorageHostRefVO.class)
                .eq(LocalStorageHostRefVO_.hostUuid, vm.hostUuid).find()

        def beforeCapacityResult = getPrimaryStorageCapacity {
            primaryStorageUuids = [ps.uuid]
        } as GetPrimaryStorageCapacityResult

        def hostCapacity = getLocalStorageHostDiskCapacity {
            primaryStorageUuid = ps.uuid
            hostUuid = host.uuid
        }[0] as HostDiskCapacity

        env.simulator(LocalStorageKvmBackend.CREATE_VOLUME_FROM_CACHE_PATH) {
            def rsp = new LocalStorageKvmBackend.CreateVolumeFromCacheRsp()
            rsp.totalCapacity = hostCapacity.totalCapacity
            rsp.availableCapacity = hostCapacity.availableCapacity - SizeUnit.GIGABYTE.toByte(20)
            return rsp
        }

        destroyVmInstance {
            uuid = vm.uuid
        }

        VmInstanceVO vmvo = dbFindByUuid(vm.uuid, VmInstanceVO.class)
        assert vmvo.state == VmInstanceState.Destroyed

        reconnectHost {
            uuid = vm.hostUuid
        }
        reconnectPrimaryStorage {
            uuid = ps.uuid
        }

        def afterCapacityResult = getPrimaryStorageCapacity {
            primaryStorageUuids = [ps.uuid]
        } as GetPrimaryStorageCapacityResult

        LocalStorageHostRefVO afterRefVO = Q.New(LocalStorageHostRefVO.class)
                .eq(LocalStorageHostRefVO_.hostUuid, vm.hostUuid).find()

        assert beforeCapacityResult.availablePhysicalCapacity == afterCapacityResult.availablePhysicalCapacity
        assert beforeCapacityResult.availableCapacity == afterCapacityResult.availableCapacity
        assert beforeRefVO.availablePhysicalCapacity == afterRefVO.availablePhysicalCapacity

        expungeVmInstance {
            uuid = vm.uuid
        }

        afterCapacityResult = getPrimaryStorageCapacity {
            primaryStorageUuids = [ps.uuid]
        } as GetPrimaryStorageCapacityResult

        afterRefVO = Q.New(LocalStorageHostRefVO.class)
                .eq(LocalStorageHostRefVO_.hostUuid, vm.hostUuid).find()

        assert beforeRefVO.availablePhysicalCapacity == afterRefVO.availablePhysicalCapacity
        assert beforeRefVO.availableCapacity == afterRefVO.availableCapacity - SizeUnit.GIGABYTE.toByte(2)
        assert beforeCapacityResult.availablePhysicalCapacity == afterCapacityResult.availablePhysicalCapacity
        assert beforeCapacityResult.availableCapacity == afterCapacityResult.availableCapacity - SizeUnit.GIGABYTE.toByte(2)
    }
}
