package org.zstack.test.integration.core.branchcascade

import org.zstack.core.db.Q
import org.zstack.header.vm.VmNicVO
import org.zstack.header.vm.VmNicVO_
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.kvm.Env
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

class AsyncBranchCascadeExtensionPointCase extends SubCase {
    EnvSpec env

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void setup() {
        useSpring(BranchCascadeTest.springSpec)
    }

    @Override
    void environment() {
        env = Env.oneVmBasicEnv()
    }

    @Override
    void test() {
        env.create {
            BypassVmDetachNicBranchCascadeExtension extension = bean(BypassVmDetachNicBranchCascadeExtension.class)
            VmInstanceInventory vm = env.inventoryByName("vm")
            L3NetworkInventory l3 = env.inventoryByName("l3")

            deleteL3Network { uuid = l3.uuid }

            assert extension.success
            long count = Q.New(VmNicVO.class).eq(VmNicVO_.vmInstanceUuid, vm.uuid).count()
            assert count == vm.vmNics.size()
        }
    }
}
