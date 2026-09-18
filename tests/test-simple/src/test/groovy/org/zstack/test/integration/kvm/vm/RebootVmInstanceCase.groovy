package org.zstack.test.integration.kvm.vm

import org.zstack.core.cloudbus.CloudBus
import org.zstack.core.db.SQL
import org.zstack.header.vm.StartVmOnHypervisorMsg
import org.zstack.header.vm.StartVmOnHypervisorReply
import org.zstack.header.vm.VmInstanceState
import org.zstack.header.vm.VmInstanceVO
import org.zstack.header.vm.VmInstanceVO_
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMConstant
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.kvm.Env
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

import static org.zstack.core.Platform.operr

/**
 * Created by lining on 2020-04-10.
 */
class RebootVmInstanceCase extends SubCase{
    EnvSpec env

    @Override
    void setup() {
        useSpring(KvmTest.springSpec)
    }

    @Override
    void environment() {
        env = Env.oneVmTwoHostNfsEnv()
    }

    @Override
    void test() {
        env.create {
            testRebootVmFail()
        }
    }

    void testRebootVmFail() {
        VmInstanceInventory vm = env.inventoryByName("vm") as VmInstanceInventory
        
        env.message(StartVmOnHypervisorMsg.class) { StartVmOnHypervisorMsg msg, CloudBus bus ->
            SQL.New(VmInstanceVO.class)
                    .eq(VmInstanceVO_.uuid, vm.uuid)
                    .set(VmInstanceVO_.state, VmInstanceState.Unknown)
                    .update()

            StartVmOnHypervisorReply reply = new StartVmOnHypervisorReply()
            reply.setError(operr("start fail on purpose"))
            bus.reply(msg, reply)
        }

        expect(AssertionError.class) {
            rebootVmInstance {
                uuid = vm.uuid
            }
        }

        VmInstanceVO vo = dbFindByUuid(vm.uuid, VmInstanceVO.class)
        assert vo.state == VmInstanceState.Unknown
    }

    @Override
    void clean() {
        env.delete()
    }
}
