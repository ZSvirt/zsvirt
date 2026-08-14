package org.zstack.test.integration.kvm.vm

import org.springframework.http.HttpEntity
import org.zstack.kvm.KVMSystemTags
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

class CreateVmWithPredefinedPciBridgeCase extends SubCase {
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
            createVmWithPredefinedPciBridge()
            createQ35VmWithNoPredefinedPciBridge()
            testCreateTag()
        }
    }

    void testCreateTag() {
        expect(AssertionError.class) {
            createSystemTag {
                resourceUuid = vm.uuid
                tag = "vm::pci::bridge::num::32"
                resourceType = VmInstanceVO.class.simpleName
            }
        }

        expect(AssertionError.class) {
            createSystemTag {
                resourceUuid = vm.uuid
                tag = "vm::pci::bridge::num::-1"
                resourceType = VmInstanceVO.class.simpleName
            }
        }

        createSystemTag {
            resourceUuid = vm.uuid
            tag = "vm::pci::bridge::num::22"
            resourceType = VmInstanceVO.class.simpleName
        }
    }

    void createVmWithPredefinedPciBridge() {
        def targetValue = 3
        env.afterSimulator(KVMConstant.KVM_START_VM_PATH) { rsp, HttpEntity<String> e ->
            def cmd = JSONObjectUtil.toObject(e.body, KVMAgentCommands.StartVmCmd.class)
            assert cmd.predefinedPciBridgeNum == targetValue
            return rsp
        }

        String vmUuid = createVmInstance {
            name = "vm"
            instanceOfferingUuid = vm.instanceOfferingUuid
            l3NetworkUuids = [vm.defaultL3NetworkUuid]
            imageUuid = vm.imageUuid
            sessionId = currentEnvSpec.session.uuid
            systemTags = ["vm::pci::bridge::num::${targetValue}".toString()]
        }.uuid

        def tags = querySystemTag {
            conditions = [
                    "resourceUuid=${vmUuid}",
                    "tag=vm::pci::bridge::num::${targetValue}".toString()
            ]
        } as List<SystemTagInventory>
        assert tags.size() == 1

        VmInstanceVO vo = dbFindByUuid(vmUuid, VmInstanceVO.class)
        assert vo.state == VmInstanceState.Running
    }

    void createQ35VmWithNoPredefinedPciBridge() {
        def targetValue = 1
        env.afterSimulator(KVMConstant.KVM_START_VM_PATH) { rsp, HttpEntity<String> e ->
            def cmd = JSONObjectUtil.toObject(e.body, KVMAgentCommands.StartVmCmd.class)
            assert cmd.predefinedPciBridgeNum == targetValue
            return rsp
        }

        String vmUuid = createVmInstance {
            name = "vm"
            instanceOfferingUuid = vm.instanceOfferingUuid
            l3NetworkUuids = [vm.defaultL3NetworkUuid]
            imageUuid = vm.imageUuid
            sessionId = currentEnvSpec.session.uuid
            systemTags = ["vmMachineType::q35"]
        }.uuid

        def tags = querySystemTag {
            conditions = [
                    "resourceUuid=${vmUuid}", "tag=vmMachineType::q35"
            ]
        } as List<SystemTagInventory>
        assert tags.size() == 1

        VmInstanceVO vo = dbFindByUuid(vmUuid, VmInstanceVO.class)
        assert vo.state == VmInstanceState.Running
    }

    @Override
    void clean() {
        env.delete()
    }
}

