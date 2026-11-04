package org.zstack.test.integration.platforms.ha.config

import org.springframework.http.HttpEntity
import org.zstack.compute.host.HostGlobalConfig
import org.zstack.core.db.SQL
import org.zstack.ha.HaScheduler
import org.zstack.ha.header.HaGlobalConfig
import org.zstack.ha.header.HaConstants
import org.zstack.ha.header.VmHaLevel
import org.zstack.header.errorcode.SysErrors
import org.zstack.header.host.HostErrors
import org.zstack.header.host.HostStatus
import org.zstack.header.host.HostVO
import org.zstack.header.vm.VmInstanceState
import org.zstack.header.vm.VmInstanceVO
import org.zstack.header.vm.VmInstanceVO_
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMConstant
import org.zstack.sdk.ImageInventory
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.ZStackTest
import org.zstack.test.integration.platforms.ha.HaTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class StopHaCase extends SubCase {
    EnvSpec env
    VmInstanceInventory vm
    VmInstanceInventory vm2

    @Override
    void setup() {
        useSpring(ZStackTest.springSpec)
    }

    @Override
    void environment() {
        env = HaTest.normal()
    }

    @Override
    void test() {
        env.create {
            prepare()
            testAddNeverStopToStopVm()
            testColdStopHaVm()
            updateConfig()
            testRecoverHAWhenFailToStopVm()
            testRecoverHAWhenFailedToStopVmOnHypervisor()
        }
    }

    void prepare() {
        vm = env.inventoryByName("vm") as VmInstanceInventory
        def l3 = env.inventoryByName("l3") as L3NetworkInventory
        def image = env.inventoryByName("image1") as ImageInventory
        vm2 = createVmInstance {
            delegate.name = "vm-2"
            delegate.cpuNum = 4
            delegate.memorySize = gb(8)
            delegate.l3NetworkUuids = [l3.uuid]
            delegate.imageUuid = image.uuid
            delegate.hostUuid = vm.hostUuid
            delegate.platform = "Linux"
            delegate.guestOsType = "Linux"
            delegate.architecture = "x86_64"
            delegate.diskAOs = [
                [
                    "boot"        : true,
                    "platform"    : "Linux",
                    "guestOsType" : "Linux",
                    "architecture": "x86_64"
                ]
            ]
        } as VmInstanceInventory
    }

    void testAddNeverStopToStopVm() {
        logger.info("Test 001: stop the VM, expect Stopped")
        stopVmInstance {
            delegate.uuid = vm.uuid
        }
        assert queryVmInstance {
            delegate.conditions = ["uuid=${vm.uuid}"]
        }[0].state == VmInstanceState.Stopped.toString()

        env.simulator(HaConstants.SETUP_SELF_FENCER_PATH) { HttpEntity<String> e ->
            return new KVMAgentCommands.AgentResponse()
        }

        logger.info("Test 002: set NeverStop on the stopped VM and start it, expect Running")
        setVmInstanceHaLevel {
            delegate.uuid = vm.uuid
            delegate.level = VmHaLevel.NeverStop.toString()
        }
        startVmInstance {
            delegate.uuid = vm.uuid
        }
        assert queryVmInstance {
            delegate.conditions = ["uuid=${vm.uuid}"]
        }[0].state == VmInstanceState.Running.toString()

        logger.info("Test 003: stop without stopHA, the NeverStop scan starts the VM again")
        stopVmInstance {
            delegate.uuid = vm.uuid
            delegate.stopHA = "false"
        }
        retryInSecs {
            assert threadFacadeForTest().triggerBlockedPeriodicTask(HaScheduler.TASK_NAME_STOPPED_VM_SCANNER) == null
            assert queryVmInstance {
                delegate.conditions = ["uuid=${vm.uuid}"]
            }[0].state == VmInstanceState.Running.toString()
        }

        logger.info("Test 004: stop with stopHA, expect NeverStop and inhibition")
        stopVmInstance {
            delegate.uuid = vm.uuid
        }
        assert queryVmInstance {
            delegate.conditions = ["uuid=${vm.uuid}"]
        }[0].state == VmInstanceState.Stopped.toString()
        def records = zqlQuery("query VmHa where uuid = '${vm.uuid}'")
        assert records.size() == 1
        assert records[0].haLevel == VmHaLevel.NeverStop.toString()
        assert records[0].inhibitionReason == "Before VM stopping on hypervisor"

        logger.info("Test 005: start the VM, expect NeverStop and inhibition cleared")
        startVmInstance {
            delegate.uuid = vm.uuid
        }
        assert queryVmInstance {
            delegate.conditions = ["uuid=${vm.uuid}"]
        }[0].state == VmInstanceState.Running.toString()
        records = zqlQuery("query VmHa where uuid = '${vm.uuid}'")
        assert records[0].haLevel == VmHaLevel.NeverStop.toString()
        assert records[0].inhibitionReason == null

        env.cleanAfterSimulatorHandlers()
    }

    void testColdStopHaVm() {
        logger.info("Test 101: stop, set NeverStop, and start, expect Running")
        stopVmInstance {
            delegate.uuid = vm.uuid
        }
        assert queryVmInstance {
            delegate.conditions = ["uuid=${vm.uuid}"]
        }[0].state == VmInstanceState.Stopped.toString()

        env.simulator(HaConstants.SETUP_SELF_FENCER_PATH) { HttpEntity<String> e ->
            return new KVMAgentCommands.AgentResponse()
        }

        setVmInstanceHaLevel {
            delegate.uuid = vm.uuid
            delegate.level = VmHaLevel.NeverStop.toString()
        }
        startVmInstance {
            delegate.uuid = vm.uuid
        }
        assert queryVmInstance {
            delegate.conditions = ["uuid=${vm.uuid}"]
        }[0].state == VmInstanceState.Running.toString()

        logger.info("Test 102: DB state is Stopped without a stop API, the NeverStop scan starts the VM")
        SQL.New(VmInstanceVO.class)
                .eq(VmInstanceVO_.uuid, vm.uuid)
                .set(VmInstanceVO_.state, VmInstanceState.Stopped)
                .update()
        assert threadFacadeForTest().triggerBlockedPeriodicTask(HaScheduler.TASK_NAME_STOPPED_VM_SCANNER) == null
        retryInSecs {
            assert queryVmInstance {
                delegate.conditions = ["uuid=${vm.uuid}"]
            }[0].state == VmInstanceState.Running.toString()
        }

        logger.info("Test 103: cold stop, expect NeverStop and inhibition")
        stopVmInstance {
            delegate.uuid = vm.uuid
            delegate.type = "cold"
        }
        retryInSecs {
            assert queryVmInstance {
                delegate.conditions = ["uuid=${vm.uuid}"]
            }[0].state == VmInstanceState.Stopped.toString()
        }
        def records = zqlQuery("query VmHa where uuid = '${vm.uuid}'")
        assert records[0].haLevel == VmHaLevel.NeverStop.toString()
        assert records[0].inhibitionReason == "Before VM stopping on hypervisor"

        logger.info("Test 104: start the VM, expect NeverStop and inhibition cleared")
        startVmInstance {
            delegate.uuid = vm.uuid
        }
        assert queryVmInstance {
            delegate.conditions = ["uuid=${vm.uuid}"]
        }[0].state == VmInstanceState.Running.toString()
        records = zqlQuery("query VmHa where uuid = '${vm.uuid}'")
        assert records[0].haLevel == VmHaLevel.NeverStop.toString()
        assert records[0].inhibitionReason == null

        env.cleanAfterSimulatorHandlers()
    }

    void updateConfig() {
        logger.info("Test 201: enable HA and shorten host check intervals")
        updateGlobalConfig {
            delegate.category = HaGlobalConfig.CATEGORY
            delegate.name = HaGlobalConfig.ALL.name
            delegate.value = "true"
        }
        updateGlobalConfig {
            delegate.category = HaGlobalConfig.CATEGORY
            delegate.name = HaGlobalConfig.HOST_CHECK_INTERVAL.name
            delegate.value = "1"
        }
        updateGlobalConfig {
            delegate.category = HaGlobalConfig.CATEGORY
            delegate.name = HaGlobalConfig.HOST_CHECK_MAX_ATTEMPTS.name
            delegate.value = "1"
        }
        updateGlobalConfig {
            delegate.category = HaGlobalConfig.CATEGORY
            delegate.name = HaGlobalConfig.HOST_CHECK_SUCCESS_INTERVAL.name
            delegate.value = "1"
        }
        updateGlobalConfig {
            delegate.category = HaGlobalConfig.CATEGORY
            delegate.name = HaGlobalConfig.HOST_CHECK_SUCCESS_TIMES.name
            delegate.value = "1"
        }
        updateGlobalConfig {
            delegate.category = HostGlobalConfig.CATEGORY
            delegate.name = HostGlobalConfig.AUTO_RECONNECT_ON_ERROR.name
            delegate.value = "false"
        }
        updateGlobalConfig {
            delegate.category = HostGlobalConfig.CATEGORY
            delegate.name = HostGlobalConfig.PING_HOST_INTERVAL.name
            delegate.value = "1"
        }
        updateGlobalConfig {
            delegate.category = HaGlobalConfig.CATEGORY
            delegate.name = HaGlobalConfig.NEVER_STOP_VM_FAILURE_RETRY_DELAY.name
            delegate.value = "1"
        }
    }

    void testRecoverHAWhenFailToStopVm() {
        logger.info("Test 301: stop while the host is reconnecting, expect failure and no inhibition")
        String hostUuid = vm2.hostUuid
        AtomicBoolean vmRecovered = new AtomicBoolean(false)
        env.afterSimulator(KVMConstant.KVM_CONNECT_PATH) { KVMAgentCommands.ConnectResponse rsp, HttpEntity<String> e ->
            rsp.success = true
            while (!vmRecovered.get()) {
                TimeUnit.MILLISECONDS.sleep(100)
            }
            return rsp
        }
        Thread.start {
            reconnectHost {
                delegate.uuid = hostUuid
            }
        }
        retryInSecs {
            assert dbFindByUuid(hostUuid, HostVO.class).status == HostStatus.Connecting
        }

        expectApiFailure({
            stopVmInstance {
                delegate.uuid = vm2.uuid
                delegate.stopHA = "true"
            }
        }) {
            assert delegate.code == SysErrors.OPERATION_ERROR.toString()
        }

        vmRecovered.set(true)
        def records = zqlQuery("query VmHa where uuid = '${vm2.uuid}'")
        assert records.size() == 1
        assert records[0].inhibitionReason == null

        retryInSecs {
            assert dbFindByUuid(hostUuid, HostVO.class).status == HostStatus.Connected
            assert queryVmInstance {
                delegate.conditions = ["uuid=${vm2.uuid}"]
            }[0].state == VmInstanceState.Running.toString()
        }

        env.cleanSimulatorAndMessageHandlers()
    }

    void testRecoverHAWhenFailedToStopVmOnHypervisor() {
        logger.info("Test 401: hypervisor refuses to stop the VM, expect failure and no inhibition")
        env.afterSimulator(KVMConstant.KVM_STOP_VM_PATH) { KVMAgentCommands.StopVmResponse rsp, HttpEntity<String> e ->
            rsp.success = false
            rsp.error = "on purpose"
            return rsp
        }
        env.afterSimulator(KVMConstant.KVM_VM_SYNC_PATH) { KVMAgentCommands.VmSyncResponse rsp, HttpEntity<String> e ->
            if (rsp.states != null && rsp.states.containsKey(vm2.uuid)) {
                rsp.states.put(vm2.uuid, KVMConstant.KvmVmState.Running.toString())
            }
            return rsp
        }
        env.afterSimulator(KVMConstant.KVM_VM_CHECK_STATE) { KVMAgentCommands.CheckVmStateRsp rsp, HttpEntity<String> e ->
            if (rsp.states != null && rsp.states.containsKey(vm2.uuid)) {
                rsp.states.put(vm2.uuid, KVMConstant.KvmVmState.Running.toString())
            }
            return rsp
        }

        expectApiFailure({
            stopVmInstance {
                delegate.uuid = vm2.uuid
                delegate.stopHA = "true"
            }
        }) {
            assert delegate.code == HostErrors.FAILED_TO_STOP_VM_ON_HYPERVISOR.toString()
        }

        def records = zqlQuery("query VmHa where uuid = '${vm2.uuid}'")
        assert records[0].inhibitionReason == null
        retryInSecs {
            assert queryVmInstance {
                delegate.conditions = ["uuid=${vm2.uuid}"]
            }[0].state == VmInstanceState.Running.toString()
        }
    }

    @Override
    void clean() {
        env.delete()
    }
}
