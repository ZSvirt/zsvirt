package org.zstack.test.integration.platforms.ha.dead

import org.springframework.http.HttpEntity
import org.zstack.compute.host.HostGlobalConfig
import org.zstack.ha.header.HaGlobalConfig
import org.zstack.ha.header.VmHaLevel
import org.zstack.header.host.HostStatus
import org.zstack.header.host.HostVO
import org.zstack.header.vm.VmInstanceState
import org.zstack.header.vm.VmInstanceVO
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMConstant
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.ZStackTest
import org.zstack.test.integration.platforms.ha.HaTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

class HaOnDownHostCase extends SubCase {
    EnvSpec env

    VmInstanceInventory vm

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
            testWhenHostPingFail()
        }
    }

    void prepare() {
        vm = env.inventoryByName("vm") as VmInstanceInventory

        setVmInstanceHaLevel {
            delegate.uuid = vm.uuid
            delegate.level = VmHaLevel.NeverStop.toString()
        }

        updateGlobalConfig {
            delegate.category = HostGlobalConfig.PING_HOST_INTERVAL.category
            delegate.name = HostGlobalConfig.PING_HOST_INTERVAL.name
            delegate.value = 1
        }
        updateGlobalConfig {
            delegate.category = HaGlobalConfig.HOST_CHECK_INTERVAL.category
            delegate.name = HaGlobalConfig.HOST_CHECK_INTERVAL.name
            delegate.value = 1
        }
    }

    /**
     * from cloud: HaCase.testDeleteVmHaLevelAndMakeHostDownVmWontStart
     */
    void testWhenHostPingFail() {
        def vm000 = queryVmInstance {
            delegate.conditions = ["uuid=${vm.uuid}"]
        } as List<VmInstanceInventory>
        assert vm000.size() == 1
        assert vm000[0].state == VmInstanceState.Running.toString()
        def hostUuidForPingFailure = vm000[0].hostUuid
        logger.info("VM is on " + hostUuidForPingFailure)

        logger.info("""Test 001: mock host down (ping fail), host -> Disconnected, vm -> Unknown
                stage 1:         host Disconnected -> VM Unknown
                stage 2(dead):   VM Unknown -> Stopped      (not in current cases)
                stage 3(reboot): VM Stopped -> Running      (not in current cases)""")

        KVMAgentCommands.PingCmd pingCmd = null
        env.afterSimulator(KVMConstant.KVM_PING_PATH) { KVMAgentCommands.AgentResponse rsp, HttpEntity<String> e ->
            pingCmd = json(e.body, KVMAgentCommands.PingCmd)
            rsp.success = pingCmd.hostUuid != hostUuidForPingFailure
            return rsp
        }

        retryInSecs {
            assert dbFindByUuid(hostUuidForPingFailure, HostVO).status == HostStatus.Disconnected
        }

        retryInSecs {
            def vo = dbFindByUuid(vm.uuid, VmInstanceVO)
            assert vo.state == VmInstanceState.Unknown
        }

        logger.info("Test 002: mock host up (ping success)")
        env.cleanSimulatorAndMessageHandlers()

        def vo = dbFindByUuid(vm.uuid, VmInstanceVO)
        reconnectHost {
            uuid = vo.hostUuid
        }

        retryInSecs {
            assert dbFindByUuid(vo.hostUuid, HostVO.class).status == HostStatus.Connected
        }

        retryInSecs {
            vo = dbFindByUuid(vm.uuid, VmInstanceVO.class)
            assert vo.state == VmInstanceState.Running
        }
    }

    @Override
    void clean() {
        env.delete()
    }
}
