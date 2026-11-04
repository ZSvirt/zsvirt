package org.zstack.test.integration.kvm.vm

import org.springframework.http.HttpEntity
import org.zstack.core.cloudbus.CloudBus
import org.zstack.core.db.SQL
import org.zstack.header.vm.*
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMConstant
import org.zstack.sdk.StopVmInstanceAction
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.kvm.Env
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.*

class GracefulStopPausedRecoveryCase extends SubCase {
    EnvSpec env
    String pausedOnHost
    List<String> stopTypes = []

    @Override
    void setup() { useSpring(KvmTest.springSpec) }

    @Override
    void environment() { env = Env.oneVmTwoHostNfsEnv() }

    void failGraceAsPaused(VmInstanceInventory vm) {
        pausedOnHost = vm.uuid
        def result = new StopVmInstanceAction(uuid: vm.uuid, type: "grace", sessionId: adminSession()).call()
        assert result.error != null
        assert result.error.details.contains("injected graceful timeout")
        assert stopTypes.last() == "grace"
        assert dbFindByUuid(vm.uuid, VmInstanceVO.class).state == VmInstanceState.Paused
        assert queryGCJob { conditions = ["context~=%${vm.uuid}%"] }.isEmpty()
    }

    @Override
    void test() {
        env.create {
            VmInstanceInventory vm = env.inventoryByName("vm")
            env.afterSimulator(KVMConstant.KVM_STOP_VM_PATH) { rsp, HttpEntity<String> e ->
                def cmd = json(e.body, KVMAgentCommands.StopVmCmd.class)
                stopTypes.add(cmd.type)
                if (cmd.uuid == pausedOnHost && cmd.type == "grace") {
                    rsp.success = false
                    rsp.error = "injected graceful timeout while guest is paused"
                }
                return rsp
            }
            env.afterSimulator(KVMConstant.KVM_VM_CHECK_STATE) { rsp ->
                if (pausedOnHost != null) {
                    rsp.states[pausedOnHost] = "Paused"
                }
                return rsp
            }

            // The stop failure callback must finish and reconcile actual Paused.
            failGraceAsPaused(vm)

            // Exercise periodic reconciliation independently of the failure callback.
            SQL.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, vm.uuid)
                    .set(VmInstanceVO_.state, VmInstanceState.Stopping).update()
            VmStateChangedOnHostMsg sync = new VmStateChangedOnHostMsg()
            sync.vmInstanceUuid = vm.uuid
            sync.hostUuid = vm.hostUuid
            sync.stateOnHost = "Paused"
            sync.fromSync = true
            sync.vmStateAtTracingMoment = "Stopping"
            CloudBus bus = bean(CloudBus.class)
            bus.makeTargetServiceIdByResourceUuid(sync, VmInstanceConstant.SERVICE_ID, vm.uuid)
            assert bus.call(sync).success
            assert dbFindByUuid(vm.uuid, VmInstanceVO.class).state == VmInstanceState.Paused
            assert VmAbnormalLifeCycleStruct.getVmAbnormalLifeCycleOperation(
                    vm.hostUuid, VmInstanceState.Stopping, VmInstanceState.Paused,
                    vm.hostUuid, "another-host") == null

            pausedOnHost = null
            resumeVmInstance { uuid = vm.uuid }
            assert dbFindByUuid(vm.uuid, VmInstanceVO.class).state == VmInstanceState.Running

            // A second failure must still allow an explicit hard stop.
            failGraceAsPaused(vm)
            pausedOnHost = null
            stopVmInstance { uuid = vm.uuid; type = "cold" }
            assert stopTypes == ["grace", "grace", "cold"]
            assert dbFindByUuid(vm.uuid, VmInstanceVO.class).state == VmInstanceState.Stopped
        }
    }

    @Override
    void clean() { pausedOnHost = null; env.delete() }
}
