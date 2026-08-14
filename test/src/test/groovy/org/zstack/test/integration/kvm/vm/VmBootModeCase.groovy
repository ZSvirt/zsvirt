package org.zstack.test.integration.kvm.vm

import org.zstack.compute.vm.VmSystemTags
import org.zstack.core.db.Q
import org.zstack.header.tag.SystemTagVO
import org.zstack.header.tag.SystemTagVO_
import org.zstack.header.vm.devices.VmInstanceResourceMetadataVO_
import org.zstack.header.vm.devices.VmInstanceResourceMetadataVO
import org.zstack.sdk.ImageInventory
import org.zstack.sdk.InstanceOfferingInventory
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.kvm.Env
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

class VmBootModeCase extends SubCase{
    EnvSpec env

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void setup() {
        useSpring(KvmTest.springSpec)
    }

    @Override
    void environment() {
        env = Env.oneVmBasicEnv()
    }

    @Override
    void test() {
        env.create {
            testCreateVmWithBootMode()
            testSetVmBootMode()
        }
    }

    void testSetVmBootMode() {
        def vm = env.inventoryByName("vm") as VmInstanceInventory

        assert Q.New(VmInstanceResourceMetadataVO.class)
                .eq(VmInstanceResourceMetadataVO_.vmInstanceUuid, vm.uuid)
                .count() != 0

        assert !VmSystemTags.BOOT_MODE.hasTag(vm.uuid)

        setVmBootMode {
            uuid = vm.uuid
            bootMode = "Legacy"
        }

        assert VmSystemTags.BOOT_MODE.hasTag(vm.uuid)
        assert Q.New(VmInstanceResourceMetadataVO.class)
                .eq(VmInstanceResourceMetadataVO_.vmInstanceUuid, vm.uuid)
                .count() == 0

        setVmBootMode {
            uuid = vm.uuid
            bootMode = "UEFI"
        }

        assert VmSystemTags.BOOT_MODE.hasTag(vm.uuid)
        assert Q.New(SystemTagVO.class)
                .eq(SystemTagVO_.resourceUuid, vm.uuid)
                .like(SystemTagVO_.tag, "%bootMode%").count() == 1

        deleteVmBootMode {
            uuid = vm.uuid
        }

        assert !VmSystemTags.BOOT_MODE.hasTag(vm.uuid)
    }

    void testCreateVmWithBootMode() {
        def instanceOffering = env.inventoryByName("instanceOffering") as InstanceOfferingInventory
        def image = env.inventoryByName("image1") as ImageInventory
        def l3 = env.inventoryByName("l3") as L3NetworkInventory

        createVmInstance {
            name = "test-1"
            instanceOfferingUuid = instanceOffering.uuid
            imageUuid = image.uuid
            l3NetworkUuids = [l3.uuid]
            systemTags = ["bootMode::Legacy"]
        }

        expect(AssertionError.class){
            createVmInstance {
                name = "test-1"
                instanceOfferingUuid = instanceOffering.uuid
                imageUuid = image.uuid
                l3NetworkUuids = [l3.uuid]
                systemTags = ["bootMode::testhost"]
            }
        }

        List<VmInstanceInventory> vms =  queryVmInstance {
            conditions = ["type=UserVm"]
        }
        assert vms.size() == 2
    }
}
