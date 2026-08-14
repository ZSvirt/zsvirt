package org.zstack.test.integration.kvm.vm.migrate

import org.zstack.core.cloudbus.CloudBus
import org.zstack.header.allocator.DesignatedAllocateHostMsg
import org.zstack.header.message.AbstractBeforeDeliveryMessageInterceptor
import org.zstack.header.message.Message
import org.zstack.sdk.HostInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
/**
 * Created by mingjian.deng on 2017/12/26.
 */
class GetMigrateAfterVmNicDeletedCase extends SubCase {
    EnvSpec env
    VmInstanceInventory vm

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
        env = VmMigrateEnv.oneVmThreeHostsNfsStorage()
    }

    @Override
    void test() {
        env.create {
            prepare()
            testGetVmMigrateHosts()
            testMigrate()
        }
    }

    void prepare() {
        vm = env.inventoryByName("vm") as VmInstanceInventory
    }

    void testGetVmMigrateHosts() {
        def hosts = getVmMigrationCandidateHosts {
            vmInstanceUuid = vm.uuid
        } as List<HostInventory>

        assert hosts.size() == 2

        vm = detachL3NetworkFromVm {
            vmNicUuid = vm.vmNics.get(0).uuid
        } as VmInstanceInventory

        assert vm.vmNics.size() == 0
        hosts = getVmMigrationCandidateHosts {
            vmInstanceUuid = vm.uuid
        } as List<HostInventory>

        assert hosts.size() == 2
    }

    void testMigrate() {
        def host = env.inventoryByName("kvm") as HostInventory

        CloudBus bus = bean(CloudBus.class)
        boolean allowNoL3 = false
        int count = 0
        bus.installBeforeDeliveryMessageInterceptor(new AbstractBeforeDeliveryMessageInterceptor() {
            @Override
            void beforeDeliveryMessage(Message msg) {
                DesignatedAllocateHostMsg dmsg = (DesignatedAllocateHostMsg) msg
                count += 1
                allowNoL3 = dmsg.isAllowNoL3Networks()
            }
        }, DesignatedAllocateHostMsg.class)

        vm = migrateVm {
            vmInstanceUuid = vm.uuid
        } as VmInstanceInventory

        assert vm.hostUuid != host.uuid
        assert count == 1
        assert allowNoL3
    }
}
