package org.zstack.test.integration.platforms.ha.config

import org.springframework.http.HttpEntity
import org.zstack.compute.host.HostGlobalConfig
import org.zstack.ha.header.HaGlobalConfig
import org.zstack.ha.header.SelfFencerStrategy
import org.zstack.header.host.HostStatus
import org.zstack.header.host.HostVO
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMConstant
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.ZStackTest
import org.zstack.test.integration.platforms.ha.HaTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

import static org.zstack.core.Platform.operr

class HaGlobalConfigCase extends SubCase {
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
            testGlobalConfig()
            testHostDisconnectedAndChangeHaGlobalConfig()
        }
    }

    void prepare() {
        vm = env.inventoryByName("vm") as VmInstanceInventory

        updateGlobalConfig {
            delegate.category = HostGlobalConfig.PING_HOST_INTERVAL.category
            delegate.name = HostGlobalConfig.PING_HOST_INTERVAL.name
            delegate.value = 1
        }
    }

    void testGlobalConfig() {
        logger.info("Test 001: update ha check success ratio to 0.5, expect success")
        updateGlobalConfig {
            delegate.category = HaGlobalConfig.CATEGORY
            delegate.name = HaGlobalConfig.CHECK_SUCCESS_RATIO.name
            delegate.value = "0.5"
        }

        def configs = queryGlobalConfig {
            delegate.conditions = [
                    "category=${HaGlobalConfig.CATEGORY}",
                    "name=${HaGlobalConfig.CHECK_SUCCESS_RATIO.name}"
            ]
        }
        assert configs.size() == 1
        assert configs[0].value == "0.5"
    }

    void testHostDisconnectedAndChangeHaGlobalConfig() {
        logger.info("Test 101: stop every VM, disconnect the host, enabling HA and setting Force succeed")
        def vms = queryVmInstance {}
        vms.each { VmInstanceInventory inventory ->
            stopVmInstance {
                delegate.uuid = inventory.uuid
            }
        }

        env.afterSimulator(KVMConstant.KVM_PING_PATH) { KVMAgentCommands.AgentResponse rsp, HttpEntity<String> e ->
            def pingCmd = json(e.body, KVMAgentCommands.PingCmd.class)
            rsp.success = pingCmd.hostUuid != vm.hostUuid
            if (!rsp.success) {
                rsp.error = operr("on purpose").toString()
            }
            return rsp
        }
        retryInSecs {
            assert dbFindByUuid(vm.hostUuid, HostVO.class).status == HostStatus.Disconnected
        }

        updateGlobalConfig {
            delegate.category = HaGlobalConfig.CATEGORY
            delegate.name = HaGlobalConfig.ALL.name
            delegate.value = "true"
        }
        updateGlobalConfig {
            delegate.category = HaGlobalConfig.CATEGORY
            delegate.name = HaGlobalConfig.SELF_FENCER_STRATEGY.name
            delegate.value = SelfFencerStrategy.Force.toString()
        }
        assert queryGlobalConfig {
            delegate.conditions = ["category=${HaGlobalConfig.CATEGORY}", "name=${HaGlobalConfig.ALL.name}"]
        }[0].value == "true"
        assert queryGlobalConfig {
            delegate.conditions = ["category=${HaGlobalConfig.CATEGORY}", "name=${HaGlobalConfig.SELF_FENCER_STRATEGY.name}"]
        }[0].value == SelfFencerStrategy.Force.toString()

        logger.info("Test 102: reconnect the host, disable HA and set fencer strategy to Permissive")
        env.cleanSimulatorAndMessageHandlers()
        reconnectHost {
            delegate.uuid = vm.hostUuid
        }
        updateGlobalConfig {
            delegate.category = HaGlobalConfig.CATEGORY
            delegate.name = HaGlobalConfig.ALL.name
            delegate.value = "false"
        }
        updateGlobalConfig {
            delegate.category = HaGlobalConfig.CATEGORY
            delegate.name = HaGlobalConfig.SELF_FENCER_STRATEGY.name
            delegate.value = SelfFencerStrategy.Permissive.toString()
        }
        assert queryGlobalConfig {
            delegate.conditions = ["category=${HaGlobalConfig.CATEGORY}", "name=${HaGlobalConfig.ALL.name}"]
        }[0].value == "false"
        assert queryGlobalConfig {
            delegate.conditions = ["category=${HaGlobalConfig.CATEGORY}", "name=${HaGlobalConfig.SELF_FENCER_STRATEGY.name}"]
        }[0].value == SelfFencerStrategy.Permissive.toString()
    }

    @Override
    void clean() {
        env.delete()
    }
}
