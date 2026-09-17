package org.zstack.test.integration.storage.backup.ceph

import org.zstack.header.image.ImageConstant
import org.zstack.header.image.ImageState
import org.zstack.header.image.ImageStatus
import org.zstack.sdk.BackupStorageInventory
import org.zstack.sdk.ImageInventory
import org.zstack.sdk.PrimaryStorageInventory
import org.zstack.sdk.VolumeInventory
import org.zstack.test.integration.storage.CephEnv
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
/**
 * Created by mingjian.deng on 2018/1/3.
 */
class CephCreateDataVolumetemplateCase extends SubCase {
    EnvSpec env
    VolumeInventory volume
    PrimaryStorageInventory ps
    BackupStorageInventory bs

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
        env = CephEnv.CephStorageOneVmWithDataVolumeEnv()
    }

    @Override
    void test() {
        env.create {
            prepare()
            testCreateImageForDataVolume()
        }
    }

    void prepare() {
        volume = env.inventoryByName("volume") as VolumeInventory
        ps = env.inventoryByName("ceph-pri") as PrimaryStorageInventory
        bs = env.inventoryByName("ceph-bk") as BackupStorageInventory
    }

    void testCreateImageForDataVolume() {
        String name = "ceph-to-ceph"
        def ceph_image = createDataVolumeTemplateFromVolume {
            delegate.name = name
            delegate.description = "description"
            delegate.volumeUuid = volume.uuid
        } as ImageInventory

        assert ceph_image.name == "ceph-to-ceph"
        assert ceph_image.description == "description"
        assert ceph_image.backupStorageRefs.size() == 1
        assert ceph_image.backupStorageRefs.get(0).backupStorageUuid == bs.uuid
        assert ceph_image.state == ImageState.Enabled.toString()
        assert ceph_image.status == ImageStatus.Ready.toString()

        assert ceph_image.type == ImageConstant.ZSTACK_IMAGE_TYPE
        assert ceph_image.mediaType == ImageConstant.ImageMediaType.DataVolumeTemplate.name()
        assert ceph_image.guestOsType == null
        assert ceph_image.format == ImageConstant.RAW_FORMAT_STRING
        assert ceph_image.platform == null
        assert ceph_image.md5Sum == null
    }
}
