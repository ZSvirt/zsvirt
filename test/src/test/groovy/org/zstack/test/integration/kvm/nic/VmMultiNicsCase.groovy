package org.zstack.test.integration.kvm.nic

import org.springframework.http.HttpEntity
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMConstant
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.kvm.Env
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.gson.JSONObjectUtil

/**
 * Created by MaJin on 2017/12/19.
 */
class VmMultiNicsCase extends SubCase{
    EnvSpec env
    VmInstanceInventory vm

    @Override
    void setup() {
        useSpring(KvmTest.springSpec)
    }

    @Override
    void environment() {
        env = Env.oneVmTwoL3NetworkLocalEnv()
    }

    @Override
    void test() {
        env.create {
            vm = env.inventoryByName("vm") as VmInstanceInventory
            testStartVmWithFixedOrderNics()
        }

    }

    @Override
    void clean() {
        env.delete()
    }

    void testStartVmWithFixedOrderNics(){
        env.simulator(KVMConstant.KVM_START_VM_PATH){ HttpEntity<String> e ->
            KVMAgentCommands.StartVmCmd cmd = JSONObjectUtil.toObject(e.body, KVMAgentCommands.StartVmCmd.class)
            assert cmd.nics.get(0).deviceId == 0
            assert cmd.nics.get(1).deviceId == 1
        }

        rebootVmInstance {
            uuid = vm.uuid
        }
    }
}
