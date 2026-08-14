package org.zstack.test.integration.storage.primary.local.capacity

import org.springframework.http.HttpEntity
import org.zstack.core.db.Q
import org.zstack.header.image.ImageConstant
import org.zstack.sdk.BackupStorageInventory
import org.zstack.sdk.DiskOfferingInventory
import org.zstack.sdk.GetPrimaryStorageCapacityResult
import org.zstack.sdk.HostDiskCapacity
import org.zstack.sdk.HostInventory
import org.zstack.sdk.ImageInventory
import org.zstack.sdk.InstanceOfferingInventory
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.PrimaryStorageInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.storage.primary.local.LocalStorageHostRefVO
import org.zstack.storage.primary.local.LocalStorageHostRefVO_
import org.zstack.storage.primary.local.LocalStorageKvmBackend
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit
/**
 * Created by SyZhao on 2017/4/17.
 */
class LocalStorageCreateVmByIsoCapacityCase extends SubCase {
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
        env = LocalStorageEnv.localStorageOneVmEnvForCapacity()
    }

    @Override
    void test() {
        env.create {
            testCreateVmByIsoCheckCapacity()
        }
    }

    void testCreateVmByIsoCheckCapacity() {
        LocalStorageEnv.simulator(env)

        def ps = env.inventoryByName("local") as PrimaryStorageInventory
        def diskOffering = env.inventoryByName("diskOffering") as DiskOfferingInventory
        def instanceOffering = env.inventoryByName("instanceOffering") as InstanceOfferingInventory
        def l3 = env.inventoryByName("l3") as L3NetworkInventory
        def bs = env.inventoryByName("sftp") as BackupStorageInventory
        def host = env.inventoryByName("kvm") as HostInventory

        ImageInventory iso = addImage {
            name = "sized-image"
            url = "http://my-site/foo.iso"
            backupStorageUuids = [bs.uuid]
            format = ImageConstant.ISO_FORMAT_STRING
        }

        reconnectPrimaryStorage {
            uuid = ps.uuid
        }

        GetPrimaryStorageCapacityResult beforeCapacityResult = getPrimaryStorageCapacity {
            primaryStorageUuids = [ps.uuid]
        }

        def hostCapacity = getLocalStorageHostDiskCapacity {
            primaryStorageUuid = ps.uuid
            hostUuid = host.uuid
        }[0] as HostDiskCapacity

        env.simulator(LocalStorageKvmBackend.CREATE_EMPTY_VOLUME_PATH) {
            def rsp = new LocalStorageKvmBackend.CreateEmptyVolumeRsp()
            rsp.totalCapacity = hostCapacity.totalCapacity
            rsp.availableCapacity = hostCapacity.availableCapacity - SizeUnit.GIGABYTE.toByte(20) - SizeUnit.GIGABYTE.toByte(1)
            return rsp
        }

        def vm = createVmInstance {
            name = "crt-vm"
            instanceOfferingUuid = instanceOffering.uuid
            imageUuid = iso.uuid
            l3NetworkUuids = [l3.uuid]
            rootDiskOfferingUuid = diskOffering.uuid
            hostUuid = host.uuid
        } as VmInstanceInventory

        GetPrimaryStorageCapacityResult capacityResult = getPrimaryStorageCapacity {
            primaryStorageUuids = [ps.uuid]
        }

        def hostRef = Q.New(LocalStorageHostRefVO.class).eq(LocalStorageHostRefVO_.hostUuid, host.uuid).find() as LocalStorageHostRefVO
        assert hostRef.availableCapacity == hostCapacity.availableCapacity - SizeUnit.GIGABYTE.toByte(20) - SizeUnit.GIGABYTE.toByte(1)
        assert hostRef.availablePhysicalCapacity == hostCapacity.availablePhysicalCapacity - SizeUnit.GIGABYTE.toByte(20) - SizeUnit.GIGABYTE.toByte(1)
        assert vm.allVolumes[0].diskOfferingUuid == diskOffering.uuid
        // ImageCache(1G) + VolumeSize(20G)
        assert beforeCapacityResult.availableCapacity == capacityResult.availableCapacity +
                SizeUnit.GIGABYTE.toByte(1) + SizeUnit.GIGABYTE.toByte(20)
        assert beforeCapacityResult.availablePhysicalCapacity ==
                capacityResult.availablePhysicalCapacity + SizeUnit.GIGABYTE.toByte(20) + SizeUnit.GIGABYTE.toByte(1)
        assert beforeCapacityResult.totalCapacity == capacityResult.totalCapacity
        assert beforeCapacityResult.totalPhysicalCapacity == capacityResult.totalPhysicalCapacity

        env.afterSimulator(LocalStorageKvmBackend.INIT_PATH) { rsp, HttpEntity<String> e ->
            rsp.totalCapacity = hostCapacity.totalCapacity
            rsp.localStorageUsedCapacity = SizeUnit.GIGABYTE.toByte(20) + SizeUnit.GIGABYTE.toByte(1)
            rsp.availableCapacity = hostCapacity.availableCapacity - rsp.localStorageUsedCapacity
            return rsp
        }

        reconnectHost {
            uuid = host.uuid
        }
        GetPrimaryStorageCapacityResult afterCapacityResult = getPrimaryStorageCapacity {
            primaryStorageUuids = [ps.uuid]
        }
        assert beforeCapacityResult.availableCapacity == afterCapacityResult.availableCapacity +
                SizeUnit.GIGABYTE.toByte(1) + SizeUnit.GIGABYTE.toByte(20)
        assert beforeCapacityResult.availablePhysicalCapacity ==
                afterCapacityResult.availablePhysicalCapacity + SizeUnit.GIGABYTE.toByte(20) + SizeUnit.GIGABYTE.toByte(1)
        assert beforeCapacityResult.totalCapacity == afterCapacityResult.totalCapacity
        assert beforeCapacityResult.totalPhysicalCapacity == afterCapacityResult.totalPhysicalCapacity
    }
}
