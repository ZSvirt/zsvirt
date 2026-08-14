package org.zstack.test.integration.storage.primary.local.capacity

import org.springframework.http.HttpEntity
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
import org.zstack.storage.primary.local.LocalStorageKvmBackend
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit
/**
 * Created by SyZhao on 2017/4/17.
 */
class LocalStorageCreateVmByImageCapacityCase extends SubCase {
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
            testCreateVmByImageCheckCapacity()
        }
    }

    void testCreateVmByImageCheckCapacity() {
        LocalStorageEnv.simulator(env)

        def ps = env.inventoryByName("local") as PrimaryStorageInventory
        def diskOffering = env.inventoryByName("diskOffering") as DiskOfferingInventory
        def instanceOffering = env.inventoryByName("instanceOffering") as InstanceOfferingInventory
        def l3 = env.inventoryByName("l3") as L3NetworkInventory
        def bs = env.inventoryByName("sftp") as BackupStorageInventory
        def host = env.inventoryByName("kvm") as HostInventory

        ImageInventory sizedImage = addImage {
            name = "sized-image"
            url = "http://my-site/foo.qcow2"
            backupStorageUuids = [bs.uuid]
            format = ImageConstant.QCOW2_FORMAT_STRING
        }

        def hostCapacity = getLocalStorageHostDiskCapacity {
            primaryStorageUuid = ps.uuid
            hostUuid = host.uuid
        }[0] as HostDiskCapacity

        GetPrimaryStorageCapacityResult beforeCapacityResult = getPrimaryStorageCapacity {
            primaryStorageUuids = [ps.uuid]
        }

        env.simulator(LocalStorageKvmBackend.CREATE_VOLUME_FROM_CACHE_PATH) {
            def rsp = new LocalStorageKvmBackend.CreateVolumeFromCacheRsp()
            rsp.totalCapacity = hostCapacity.totalCapacity
            rsp.availableCapacity = hostCapacity.availableCapacity - SizeUnit.GIGABYTE.toByte(20) - SizeUnit.GIGABYTE.toByte(1)
            return rsp
        }

        def vm = createVmInstance {
            name = "crt-vm"
            instanceOfferingUuid = instanceOffering.uuid
            imageUuid = sizedImage.uuid
            l3NetworkUuids = [l3.uuid]
            hostUuid = host.uuid
        }

        GetPrimaryStorageCapacityResult capacityResult = getPrimaryStorageCapacity {
            primaryStorageUuids = [ps.uuid]
        }
        // ImageCache(1G) + QCOW2(2G)
        assert beforeCapacityResult.availableCapacity == capacityResult.availableCapacity +
                SizeUnit.GIGABYTE.toByte(1) + SizeUnit.GIGABYTE.toByte(2)
        assert beforeCapacityResult.availablePhysicalCapacity == capacityResult.availablePhysicalCapacity + SizeUnit.GIGABYTE.toByte(20) + SizeUnit.GIGABYTE.toByte(1)
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
                SizeUnit.GIGABYTE.toByte(1) + SizeUnit.GIGABYTE.toByte(2)
        assert beforeCapacityResult.availablePhysicalCapacity == afterCapacityResult.availablePhysicalCapacity + SizeUnit.GIGABYTE.toByte(20) + SizeUnit.GIGABYTE.toByte(1)
        assert beforeCapacityResult.totalCapacity == afterCapacityResult.totalCapacity
        assert beforeCapacityResult.totalPhysicalCapacity == afterCapacityResult.totalPhysicalCapacity
    }
}
