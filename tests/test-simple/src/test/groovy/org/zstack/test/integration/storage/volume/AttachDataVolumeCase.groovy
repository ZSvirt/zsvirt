package org.zstack.test.integration.storage.volume

import org.zstack.core.componentloader.PluginRegistry
import org.zstack.core.db.Q
import org.zstack.header.image.ImageConstant
import org.zstack.header.volume.VolumeFormat
import org.zstack.header.volume.VolumeVO
import org.zstack.header.volume.VolumeVO_
import org.zstack.kvm.KVMAttachVolumeExtensionPoint
import org.zstack.kvm.KVMConvertVolumeExtensionPoint
import org.zstack.sdk.DiskOfferingInventory
import org.zstack.sdk.PriceInventory
import org.zstack.sdk.PrimaryStorageInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.sdk.VolumeInventory
import org.zstack.test.integration.storage.CephEnv
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

/**
 * Created by MaJin on 2017/10/29.
 */
class AttachDataVolumeCase extends SubCase{
    EnvSpec env
    VolumeInventory dataVolume, dataVolume2

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
            PluginRegistry pluginRegistry = bean(PluginRegistry.class)
            def convertExts = pluginRegistry.getExtensionList(KVMConvertVolumeExtensionPoint.class)
            def attachExts = pluginRegistry.getExtensionList(KVMAttachVolumeExtensionPoint.class)
            assert convertExts.containsAll(attachExts) :
                    "when implement convert volume before attach volume, ConvertVolumeExtensionPoint is needed too"
            testCreateDataVolume()
            testAttachDataVolume()
        }
    }

    @Override
    void clean() {
        env.delete()
    }

    void testCreateDataVolume(){
        def diskOffering = env.inventoryByName("diskOffering") as DiskOfferingInventory
        def ps = env.inventoryByName("ceph-pri") as PrimaryStorageInventory
        dataVolume = createDataVolume{
            name = "test"
            diskOfferingUuid = diskOffering.uuid
            primaryStorageUuid = ps.uuid
        } as VolumeInventory

        assert dataVolume.format == ImageConstant.RAW_FORMAT_STRING

        dataVolume2 = createDataVolume{
            name = "test2"
            diskOfferingUuid = diskOffering.uuid
            primaryStorageUuid = ps.uuid
        } as VolumeInventory
    }

    void testAttachDataVolume(){
        def vm = env.inventoryByName("test-vm") as VmInstanceInventory
        attachDataVolumeToVm {
            volumeUuid = dataVolume.uuid
            vmInstanceUuid = vm.uuid
        }
        assert Q.New(VolumeVO.class)
                .eq(VolumeVO_.uuid, dataVolume.uuid)
                .select(VolumeVO_.format)
                .findValue() == ImageConstant.RAW_FORMAT_STRING

        attachDataVolumeToVm {
            volumeUuid = dataVolume2.uuid
            vmInstanceUuid = vm.uuid
        }
    }

}
