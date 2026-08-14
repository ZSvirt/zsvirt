package org.zstack.test.integration.storage.volume

import org.zstack.core.db.Q
import org.zstack.header.volume.VolumeType
import org.zstack.header.volume.VolumeVO
import org.zstack.header.volume.VolumeVO_
import org.zstack.sdk.DiskOfferingInventory
import org.zstack.sdk.HostInventory
import org.zstack.sdk.ImageInventory
import org.zstack.sdk.PrimaryStorageInventory
import org.zstack.sdk.VolumeInventory
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

class CreateDataVolumeWithCephFromTemplateCase extends SubCase {
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
        env = VolumeEnv.cephStorageCephEnv()
    }

    @Override
    void test() {
        env.create {
            testCreateDataVolumeWithCephFromTemplate()
            testCreateDataVolumeWithCeph()
        }
    }

    void testCreateDataVolumeWithCeph() {
        DiskOfferingInventory diskOffering = env.inventoryByName("diskOffering")
        PrimaryStorageInventory ceph = env.inventoryByName("ceph-ps")
        VolumeInventory dataVolume = createDataVolume {
            name = 'test-volume-template-ceph-3'
            diskOfferingUuid = diskOffering.uuid
            primaryStorageUuid = ceph.uuid
            systemTags = ["capability::virtio-scsi", "ephemeral::shareable"]
        } as VolumeInventory

        assert Q.New(VolumeVO.class).eq(VolumeVO_.type, VolumeType.Data).eq(VolumeVO_.name, dataVolume.name).count() == 1
    }

    void testCreateDataVolumeWithCephFromTemplate() {
        HostInventory host = env.inventoryByName("kvm")
        PrimaryStorageInventory ceph = env.inventoryByName("ceph-ps")
        ImageInventory image = env.inventoryByName("image-data-volume")

        VolumeInventory dataVolume = createDataVolumeFromVolumeTemplate {
            name = 'test-volume-template-local-2'
            imageUuid = image.uuid
            primaryStorageUuid = ceph.uuid
            hostUuid = host.uuid
            systemTags = ["capability::virtio-scsi"]
        } as VolumeInventory

        assert Q.New(VolumeVO.class).eq(VolumeVO_.type, VolumeType.Data).eq(VolumeVO_.name, dataVolume.name).count() == 1
    }
}
