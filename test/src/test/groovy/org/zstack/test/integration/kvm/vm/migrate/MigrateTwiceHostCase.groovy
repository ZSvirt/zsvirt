package org.zstack.test.integration.kvm.vm.migrate

import org.zstack.header.vm.VmInstanceVO
import org.zstack.sdk.HostInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
/**
 * Created by camile on 2017-11-14.
 */
class MigrateTwiceHostCase extends SubCase{
    EnvSpec env

    @Override
    void setup() {
        useSpring(KvmTest.springSpec)
    }

    @Override
    void environment() {
        env = VmMigrateEnv.oneVmThreeHostsLocalStorage()
    }

    @Override
    void test() {
        env.create {
            //lastHostUuid must be the host where the last VM stopped, see more ZSTAC-7930
            testLastHostUuid()
        }
    }

    void testLastHostUuid(){
        def vm = env.inventoryByName("vm") as VmInstanceInventory
        def host = env.inventoryByName("kvm") as HostInventory
        def host1 = env.inventoryByName("kvm1") as HostInventory
        def host2 = env.inventoryByName("kvm2") as HostInventory

        expect([AssertionError.class]) {
            localStorageMigrateVolume {
                volumeUuid = vm.rootVolumeUuid
                destHostUuid = host1.uuid
            }
        }

        stopVmInstance {
            uuid = vm.uuid
        }

        localStorageMigrateVolume {
            volumeUuid = vm.rootVolumeUuid
            destHostUuid = host1.uuid
        }

        localStorageMigrateVolume {
            volumeUuid = vm.rootVolumeUuid
            destHostUuid = host2.uuid
        }

        VmInstanceVO vmvo = dbFindByUuid(vm.uuid, VmInstanceVO.class)
        assert vmvo.hostUuid == null
        assert vmvo.lastHostUuid == host.uuid

        startVmInstance {
            uuid = vm.uuid
        }

        vmvo = dbFindByUuid(vm.uuid, VmInstanceVO.class)
        assert vmvo.hostUuid == host2.uuid
        assert vmvo.lastHostUuid == host.uuid

        stopVmInstance {
            uuid = vm.uuid
        }

    }

    @Override
    void clean() {
        env.delete()
    }
}
