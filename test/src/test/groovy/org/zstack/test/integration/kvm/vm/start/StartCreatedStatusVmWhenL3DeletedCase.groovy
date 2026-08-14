package org.zstack.test.integration.kvm.vm.start

import org.zstack.header.errorcode.SysErrors
import org.zstack.header.vm.VmCreationStrategy
import org.zstack.sdk.StartVmInstanceAction
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.kvm.Env
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.testlib.Test

/**
 * Created by lining on 2017/2/22.
 */
class StartCreatedStatusVmWhenL3DeletedCase extends SubCase {
    EnvSpec env

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
            runTest()
        }
    }

    void runTest(){
        VmInstanceInventory vm = env.inventoryByName("vm")

        VmInstanceInventory newVm = createVmInstance {
            name = "newVm"
            imageUuid = vm.imageUuid
            instanceOfferingUuid = vm.instanceOfferingUuid
            l3NetworkUuids = [vm.defaultL3NetworkUuid]
            strategy =  VmCreationStrategy.JustCreate.name()
        }

        deleteL3Network {
            uuid = newVm.defaultL3NetworkUuid
        }

        StartVmInstanceAction startVmInstanceAction = new StartVmInstanceAction(
                uuid: newVm.uuid,
                sessionId: Test.currentEnvSpec.session.uuid
        )
        StartVmInstanceAction.Result result = startVmInstanceAction.call()
        assert null != result.error
        assert result.error.code == "VM.1001"
        assert result.error.causes.size() > 0
        assert result.error.causes[0]["code"] == SysErrors.OPERATION_ERROR.toString()
    }

    @Override
    void clean() {
        env.delete()
    }
}