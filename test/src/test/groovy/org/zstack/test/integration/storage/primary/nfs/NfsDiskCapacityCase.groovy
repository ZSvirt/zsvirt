package org.zstack.test.integration.storage.primary.nfs

import org.zstack.core.Platform
import org.zstack.header.image.ImageConstant
import org.zstack.header.storage.primary.PrimaryStorageOverProvisioningManager
import org.zstack.sdk.BackupStorageInventory
import org.zstack.sdk.DiskOfferingInventory
import org.zstack.sdk.GetPrimaryStorageCapacityResult
import org.zstack.sdk.ImageInventory
import org.zstack.sdk.InstanceOfferingInventory
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.PrimaryStorageInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.sdk.VolumeInventory
import org.zstack.storage.backup.sftp.SftpBackupStorageConstant
import org.zstack.test.integration.storage.Env
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit

/**
 * Created by AlanJager on 2017/3/25.
 */
class NfsDiskCapacityCase extends SubCase {
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
        env = Env.nfsOneVmEnv()
    }

    @Override
    void test() {
        env.create {
            testAvailableCapacityDuringDataVolumeLifecycle()
        }
    }

    void testAvailableCapacityDuringDataVolumeLifecycle() {
        PrimaryStorageOverProvisioningManager psRatioMgr = bean(PrimaryStorageOverProvisioningManager.class)
        PrimaryStorageInventory primaryStorageInventory = env.inventoryByName("nfs")
        BackupStorageInventory backupStorageInventory = env.inventoryByName("sftp")
        L3NetworkInventory l3NetworkInventory = env.inventoryByName("l3")
        InstanceOfferingInventory instanceOfferingInventory = env.inventoryByName("instanceOffering")
        DiskOfferingInventory diskOfferingInventory = env.inventoryByName("diskOffering")

        psRatioMgr.setGlobalRatio(1)

        env.afterSimulator(SftpBackupStorageConstant.DOWNLOAD_IMAGE_PATH) { rsp ->
            rsp.size = SizeUnit.GIGABYTE.toByte(10)
            rsp.actualSize = SizeUnit.GIGABYTE.toByte(1)
            return rsp
        }

        ImageInventory imageInventory = addImage {
            name = "test-image"
            resourceUuid = Platform.getUuid()
            backupStorageUuids = [backupStorageInventory.uuid]
            format = ImageConstant.QCOW2_FORMAT_STRING
            url = "http://image.qcow2"
        }

        assert imageInventory.size == SizeUnit.GIGABYTE.toByte(10)
        assert imageInventory.actualSize == SizeUnit.GIGABYTE.toByte(1)

        VmInstanceInventory vmInstanceInventory = createVmInstance {
            name = "test-vm"
            instanceOfferingUuid = instanceOfferingInventory.uuid
            imageUuid = imageInventory.uuid
            l3NetworkUuids = [l3NetworkInventory.uuid]
        }

        GetPrimaryStorageCapacityResult beforePrimaryStorageCapacityResult = getPrimaryStorageCapacity {
            primaryStorageUuids = [primaryStorageInventory.uuid]
        }

        GetPrimaryStorageCapacityResult getPrimaryStorageCapacityResult = getPrimaryStorageCapacity {
            primaryStorageUuids = [primaryStorageInventory.uuid]
        }

        psRatioMgr.setGlobalRatio(2.5)

        GetPrimaryStorageCapacityResult ratio251 = getPrimaryStorageCapacity {
            primaryStorageUuids = [primaryStorageInventory.uuid]
        }
        assert ratio251.availableCapacity == getPrimaryStorageCapacityResult.availableCapacity

        VolumeInventory volumeInventory1 = createDataVolume {
            name = "data-volume"
            diskOfferingUuid = diskOfferingInventory.uuid
        }
        attachDataVolumeToVm {
            volumeUuid = volumeInventory1.uuid
            vmInstanceUuid = vmInstanceInventory.uuid
        }

        getPrimaryStorageCapacityResult = getPrimaryStorageCapacity {
            primaryStorageUuids = [primaryStorageInventory.uuid]
        }
        psRatioMgr.setGlobalRatio(1.5)
        GetPrimaryStorageCapacityResult ratio15 = getPrimaryStorageCapacity {
            primaryStorageUuids = [primaryStorageInventory.uuid]
        }
        assert getPrimaryStorageCapacityResult.availableCapacity == ratio15.availableCapacity


        VolumeInventory volumeInventory2 = createDataVolume {
            name = "data-volume"
            diskOfferingUuid = diskOfferingInventory.uuid
        }
        attachDataVolumeToVm {
            volumeUuid = volumeInventory2.uuid
            vmInstanceUuid = vmInstanceInventory.uuid
        }
        deleteDataVolume {
            uuid = volumeInventory1.uuid
        }
        expungeDataVolume {
            uuid = volumeInventory1.uuid
        }

        retryInSecs {
            psRatioMgr.setGlobalRatio(1.5)
            getPrimaryStorageCapacityResult = getPrimaryStorageCapacity {
                primaryStorageUuids = [primaryStorageInventory.uuid]
            }
            psRatioMgr.setGlobalRatio(2.5)
            GetPrimaryStorageCapacityResult ratio252 = getPrimaryStorageCapacity {
                primaryStorageUuids = [primaryStorageInventory.uuid]
            }
            assert getPrimaryStorageCapacityResult.availableCapacity == ratio252.availableCapacity
        }

        deleteDataVolume {
            uuid = volumeInventory2.uuid
        }
        expungeDataVolume {
            uuid = volumeInventory2.uuid
        }

        retryInSecs {
            psRatioMgr.setGlobalRatio(2.5)
            getPrimaryStorageCapacityResult = getPrimaryStorageCapacity {
                primaryStorageUuids = [primaryStorageInventory.uuid]
            }
            psRatioMgr.setGlobalRatio(1.0)
            GetPrimaryStorageCapacityResult ratio1 = getPrimaryStorageCapacity {
                primaryStorageUuids = [primaryStorageInventory.uuid]
            }
            assert getPrimaryStorageCapacityResult.availableCapacity == ratio1.availableCapacity
        }



        GetPrimaryStorageCapacityResult getPrimaryStorageCapacityResult1 = getPrimaryStorageCapacity {
            primaryStorageUuids = [primaryStorageInventory.uuid]
        }

        assert beforePrimaryStorageCapacityResult.totalCapacity == getPrimaryStorageCapacityResult1.totalPhysicalCapacity
        assert beforePrimaryStorageCapacityResult.availableCapacity == getPrimaryStorageCapacityResult1.availableCapacity
    }
}
