package org.zstack.test.integration.kvm.vm

import org.springframework.http.HttpEntity
import org.zstack.compute.vm.VmSystemTags
import org.zstack.header.vm.VmInstanceState
import org.zstack.header.vm.VmInstanceVO
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMConstant
import org.zstack.sdk.SystemTagInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.kvm.Env
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.gson.JSONObjectUtil

/**
 * @author: kefeng.wang
 * @date: 2018-09-28 16:18
 */
class CreateVmWithMachineTypeCase extends SubCase {
    EnvSpec env
    VmInstanceInventory vm

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
            vm = env.inventoryByName("vm") as VmInstanceInventory
            createVmWithMachineType("pc")
            createVmWithMachineType("q35")
            testTagValidator()
        }
    }

    void createVmWithMachineType(String machineTypeValue) {
        assert null != vm

        env.afterSimulator(KVMConstant.KVM_START_VM_PATH) { rsp, HttpEntity<String> e ->
            def cmd = JSONObjectUtil.toObject(e.body, KVMAgentCommands.StartVmCmd.class)
            assert cmd.machineType == machineTypeValue
            if (machineTypeValue == "q35") {
                assert cmd.pciePortNums != null
            }
            return rsp
        }

        String vmUuid = createVmInstance {
            name = "vm"
            instanceOfferingUuid = vm.instanceOfferingUuid
            l3NetworkUuids = [vm.defaultL3NetworkUuid]
            imageUuid = vm.imageUuid
            sessionId = currentEnvSpec.session.uuid
            systemTags = ["${VmSystemTags.MACHINE_TYPE_TOKEN}::${machineTypeValue}".toString()]
        }.uuid

        def tags = querySystemTag {
            conditions = [
                    "resourceUuid=${vmUuid}",
                    "tag=${VmSystemTags.MACHINE_TYPE_TOKEN}::${machineTypeValue}".toString()
            ]
        } as List<SystemTagInventory>
        assert tags.size() == 1

        VmInstanceVO vo = dbFindByUuid(vmUuid, VmInstanceVO.class)
        assert vo.state == VmInstanceState.Running
    }

    void testTagValidator() {
        expect(AssertionError.class) {
            createVmInstance {
                name = "vm"
                instanceOfferingUuid = vm.instanceOfferingUuid
                l3NetworkUuids = [vm.defaultL3NetworkUuid]
                imageUuid = vm.imageUuid
                sessionId = currentEnvSpec.session.uuid
                systemTags = ["vmMachineType::xx"]
            }
        }

        expect(AssertionError.class) {
            createSystemTag {
                tag = "vmMachineType::xx"
                resourceUuid = vm.uuid
                resourceType = VmInstanceVO.class.simpleName
            }
        }
    }

    @Override
    void clean() {
        env.delete()
    }
}
