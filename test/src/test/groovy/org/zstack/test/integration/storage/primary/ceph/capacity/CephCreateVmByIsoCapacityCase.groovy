package org.zstack.test.integration.storage.primary.ceph.capacity

import org.springframework.http.HttpEntity
import org.zstack.sdk.CephBackupStorageInventory
import org.zstack.sdk.CephPrimaryStoragePoolInventory
import org.zstack.sdk.ClusterInventory
import org.zstack.sdk.DiskOfferingInventory
import org.zstack.sdk.GetPrimaryStorageCapacityResult
import org.zstack.sdk.ImageInventory
import org.zstack.sdk.InstanceOfferingInventory
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.PrimaryStorageInventory
import org.zstack.storage.ceph.CephPoolCapacity
import org.zstack.storage.ceph.primary.CephPrimaryStoragePoolVO
import org.zstack.test.integration.storage.Env
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.CephPrimaryStorageSpec
import org.zstack.testlib.PrimaryStorageSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.gson.JSONObjectUtil
import org.zstack.test.integration.storage.CephEnv
import org.zstack.storage.ceph.primary.CephPrimaryStorageBase
import org.zstack.header.image.ImageConstant
import org.zstack.testlib.BackupStorageSpec
import org.zstack.utils.data.SizeUnit
import org.zstack.storage.ceph.backup.CephBackupStorageBase
import org.zstack.core.Platform

/**
 * Created by SyZhao on 2017/4/17.
 */
class CephCreateVmByIsoCapacityCase extends SubCase {
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
            testCreateVmByIsoCheckCapacity()
        }
    }

    void testCreateVmByIsoCheckCapacity() {
        PrimaryStorageInventory ps = env.inventoryByName("ceph-pri")
        ClusterInventory cluster = env.inventoryByName("test-cluster")
        ImageInventory image = env.inventoryByName("test-iso")
        DiskOfferingInventory diskOffering = env.inventoryByName("diskOffering")
        InstanceOfferingInventory instanceOffering = env.inventoryByName("instanceOffering")
        L3NetworkInventory l3 = env.inventoryByName("l3")
        CephBackupStorageInventory bs = queryCephBackupStorage {}[0]
        CephPrimaryStoragePoolInventory cephPrimaryStoragePool = queryCephPrimaryStoragePool {
            conditions = ["type=Data"]
        }[0]

        def image_virtual_size = SizeUnit.GIGABYTE.toByte(10)//10G
        def image_physical_size = SizeUnit.GIGABYTE.toByte(1)//1G

        reconnectPrimaryStorage {
            uuid = ps.uuid
        }

        def bsPoolAvailableCapacity = 1
        def bsPoolReplicatedSize = 2
        def bsPoolUsedCapacity = 3
        def psPoolAvailableCapacity = 4
        def psPoolReplicatedSize = 5
        def psPoolUsedCapacity = 6
        def download_image_path_invoked = false
        env.simulator(CephBackupStorageBase.DOWNLOAD_IMAGE_PATH) {
            def rsp = new CephBackupStorageBase.DownloadRsp()
            rsp.size = image_virtual_size
            rsp.actualSize = image_physical_size
            download_image_path_invoked = true

            CephPoolCapacity bsPoolCapacity = new CephPoolCapacity()
            bsPoolCapacity.availableCapacity = bsPoolAvailableCapacity
            bsPoolCapacity.name = bs.getPoolName()
            bsPoolCapacity.replicatedSize = bsPoolReplicatedSize
            bsPoolCapacity.usedCapacity = bsPoolUsedCapacity
            bsPoolCapacity.totalCapacity = bsPoolAvailableCapacity + bsPoolUsedCapacity
            bsPoolCapacity.relatedOsds = "osd.1"

            CephPoolCapacity otherPoolCapacity = new CephPoolCapacity()
            otherPoolCapacity.availableCapacity = 8
            otherPoolCapacity.name = "other-pool"
            otherPoolCapacity.replicatedSize = 9
            otherPoolCapacity.usedCapacity = 10
            otherPoolCapacity.relatedOsds = "osd.2"

            CephPoolCapacity psPoolCapacity = new CephPoolCapacity()
            psPoolCapacity.availableCapacity = psPoolAvailableCapacity
            psPoolCapacity.name = cephPrimaryStoragePool.poolName
            psPoolCapacity.replicatedSize = psPoolReplicatedSize
            psPoolCapacity.usedCapacity = psPoolUsedCapacity
            psPoolCapacity.totalCapacity = SizeUnit.GIGABYTE.toByte(20)
            psPoolCapacity.relatedOsds = "osd.3"

            rsp.setPoolCapacities(Arrays.asList(bsPoolCapacity, otherPoolCapacity, psPoolCapacity))
            rsp.setTotalCapacity(bs.totalCapacity)
            rsp.setAvailableCapacity(bs.availableCapacity)

            return rsp
        }

        ImageInventory sizedImage = addImage {
            name = "sized-image"
            url = "http://my-site/foo.iso"
            backupStorageUuids = [bs.uuid]
            format = ImageConstant.ISO_FORMAT_STRING
        }
        bs = queryCephBackupStorage {}[0]
        assert bs.poolAvailableCapacity == bsPoolAvailableCapacity
        assert bs.poolReplicatedSize == bsPoolReplicatedSize
        assert bs.poolUsedCapacity == bsPoolUsedCapacity

        cephPrimaryStoragePool = queryCephPrimaryStoragePool {
            conditions = ["type=Data"]
        }[0]
        //assert cephPrimaryStoragePool.availableCapacity == psPoolAvailableCapacity
        assert cephPrimaryStoragePool.replicatedSize == psPoolReplicatedSize
        assert cephPrimaryStoragePool.usedCapacity == psPoolUsedCapacity

        assert download_image_path_invoked

        GetPrimaryStorageCapacityResult beforeCapacityResult = getPrimaryStorageCapacity {
            primaryStorageUuids = [ps.uuid]
        }

        createVmInstance {
            name = "crt-vm"
            instanceOfferingUuid = instanceOffering.uuid
            imageUuid = sizedImage.uuid
            l3NetworkUuids = [l3.uuid]
            rootDiskOfferingUuid = diskOffering.uuid
        }

        GetPrimaryStorageCapacityResult capacityResult = getPrimaryStorageCapacity {
            primaryStorageUuids = [ps.uuid]
        }
        assert beforeCapacityResult.availableCapacity == capacityResult.availableCapacity + SizeUnit.GIGABYTE.toByte(20) + image_physical_size

        env.simulator(CephPrimaryStorageBase.INIT_PATH) { HttpEntity<String> e, EnvSpec spec ->
            def cmd = JSONObjectUtil.toObject(e.body, CephPrimaryStorageBase.InitCmd.class)
            CephPrimaryStorageSpec cspec = spec.specByUuid(cmd.uuid)
            assert cspec != null: "cannot find ceph primary storage[uuid:${cmd.uuid}], check your environment()"

            def rsp = new CephPrimaryStorageBase.InitRsp()
            rsp.fsid = cspec.fsid
            rsp.userKey = Platform.uuid
            rsp.totalCapacity = cspec.totalCapacity
            rsp.availableCapacity = cspec.availableCapacity - image_physical_size
            return rsp
        }

        reconnectPrimaryStorage {
            uuid = ps.uuid
        }
        GetPrimaryStorageCapacityResult afterCapacityResult = getPrimaryStorageCapacity {
            primaryStorageUuids = [ps.uuid]
        }
        assert beforeCapacityResult.availablePhysicalCapacity == afterCapacityResult.availablePhysicalCapacity + image_physical_size
    }
}
