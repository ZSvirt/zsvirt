package org.zstack.test.integration.kvm.nic

import org.zstack.compute.vm.VmSystemTags
import org.zstack.header.image.ImagePlatform
import org.zstack.header.tag.SystemTagVO
import org.zstack.header.tag.SystemTagVO_
import org.zstack.header.vm.VmNicConstant
import org.zstack.sdk.VmInstanceInventory
import org.zstack.header.vm.VmNicVO
import org.zstack.header.vm.VmNicVO_
import org.zstack.header.vm.VmInstanceState
import org.zstack.test.integration.kvm.Env
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.core.db.Q

class ChangeWindowsVmNicDriverCase extends SubCase {
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
            testChangeWindowsVmNicDriver()
        }
    }

    void testChangeWindowsVmNicDriver() {
        VmInstanceInventory vm = env.inventoryByName("vm") as VmInstanceInventory
        assert VmSystemTags.VIRTIO.hasTag(vm.uuid)
        assert vm.vmNics[0].driverType == VmNicConstant.NIC_DRIVER_TYPE_VIRTIO

        updateVmInstance {
            platform = ImagePlatform.Windows.toString()
            uuid = vm.uuid
        }
        deleteTag {
            uuid = Q.New(SystemTagVO.class)
                    .eq(SystemTagVO_.resourceUuid, vm.uuid)
                    .eq(SystemTagVO_.tag, "driver::virtio")
                    .select(SystemTagVO_.uuid)
                    .findValue().toString()
        }
        assert !VmSystemTags.VIRTIO.hasTag(vm.uuid)
        assert Q.New(VmNicVO.class).eq(VmNicVO_.vmInstanceUuid, vm.uuid).select(VmNicVO_.driverType).findValue() == VmNicConstant.NIC_DRIVER_TYPE_E1000

        if (vm.state.equals(VmInstanceState.Running.toString())) {
            expect(AssertionError.class) {
                createSystemTag {
                    resourceType = "VmInstanceVO"
                    resourceUuid = vm.uuid
                    tag = "driver::virtio"
                }
            }

            stopVmInstance {
                uuid = vm.uuid
            }
        }

        createSystemTag {
            resourceType = "VmInstanceVO"
            resourceUuid = vm.uuid
            tag = "driver::virtio"
        }

        startVmInstance {
            uuid = vm.uuid
        }

        assert VmSystemTags.VIRTIO.hasTag(vm.uuid)
        assert Q.New(VmNicVO.class).eq(VmNicVO_.vmInstanceUuid, vm.uuid).select(VmNicVO_.driverType).findValue() == VmNicConstant.NIC_DRIVER_TYPE_VIRTIO
    }
}