package org.zstack.test.integration.platforms.ha.config

import org.springframework.http.HttpEntity
import org.zstack.compute.host.HostGlobalConfig
import org.zstack.ha.header.HaErrors
import org.zstack.ha.header.HaGlobalConfig
import org.zstack.ha.header.SelfFencerStrategy
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

import static org.zstack.core.Platform.operr

class HaGlobalConfigNegativeCase extends SubCase {
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
            testInvalidCheckSuccessRatio()
            testEnableHaWhileHostDisconnected()
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

    void testInvalidCheckSuccessRatio() {
        logger.info("Test 001: ha check success ratio outside 0..1, expect HA.2002")
        expectApiFailure({
            updateGlobalConfig {
                delegate.category = HaGlobalConfig.CATEGORY
                delegate.name = HaGlobalConfig.CHECK_SUCCESS_RATIO.name
                delegate.value = "-1"
            }
        }) {
            assert delegate.code == HaErrors.INVALID_CHECK_SUCCESS_RATIO.toString()
        }

        logger.info("Test 002: ha check success ratio 2, expect HA.2002")
        expectApiFailure({
            updateGlobalConfig {
                delegate.category = HaGlobalConfig.CATEGORY
                delegate.name = HaGlobalConfig.CHECK_SUCCESS_RATIO.name
                delegate.value = "2"
            }
        }) {
            assert delegate.code == HaErrors.INVALID_CHECK_SUCCESS_RATIO.toString()
        }

        assert queryGlobalConfig {
            delegate.conditions = ["category=${HaGlobalConfig.CATEGORY}", "name=${HaGlobalConfig.CHECK_SUCCESS_RATIO.name}"]
        }[0].value != "-1"
        assert queryGlobalConfig {
            delegate.conditions = ["category=${HaGlobalConfig.CATEGORY}", "name=${HaGlobalConfig.CHECK_SUCCESS_RATIO.name}"]
        }[0].value != "2"
    }

    void testEnableHaWhileHostDisconnected() {
        updateGlobalConfig {
            delegate.category = HaGlobalConfig.CATEGORY
            delegate.name = HaGlobalConfig.ALL.name
            delegate.value = "false"
        }

        def beforeEnable = queryGlobalConfig {
            delegate.conditions = ["category=${HaGlobalConfig.CATEGORY}", "name=${HaGlobalConfig.ALL.name}"]
        }[0].value
        def beforeStrategy = queryGlobalConfig {
            delegate.conditions = ["category=${HaGlobalConfig.CATEGORY}", "name=${HaGlobalConfig.SELF_FENCER_STRATEGY.name}"]
        }[0].value

        logger.info("Test 101: disconnect a host that still has an Unknown VM, enabling HA fails")
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
        retryInSecs {
            assert dbFindByUuid(vm.uuid, VmInstanceVO.class).state == VmInstanceState.Unknown
        }

        expectApiFailure({
            updateGlobalConfig {
                delegate.category = HaGlobalConfig.CATEGORY
                delegate.name = HaGlobalConfig.ALL.name
                delegate.value = "true"
            }
        }) {
            assert delegate.code == HaErrors.DISCONNECTED_HOST_HAS_UNKNOWN_VM.toString()
            assert delegate.details.contains(vm.hostUuid)
            assert delegate.details.count(vm.hostUuid) == 1
        }

        logger.info("Test 102: the same host still blocks a fencer strategy update")
        expectApiFailure({
            updateGlobalConfig {
                delegate.category = HaGlobalConfig.CATEGORY
                delegate.name = HaGlobalConfig.SELF_FENCER_STRATEGY.name
                delegate.value = SelfFencerStrategy.Permissive.toString()
            }
        }) {
            assert delegate.code == HaErrors.DISCONNECTED_HOST_HAS_UNKNOWN_VM.toString()
            assert delegate.details.contains(vm.hostUuid)
        }

        assert queryGlobalConfig {
            delegate.conditions = ["category=${HaGlobalConfig.CATEGORY}", "name=${HaGlobalConfig.ALL.name}"]
        }[0].value == beforeEnable
        assert queryGlobalConfig {
            delegate.conditions = ["category=${HaGlobalConfig.CATEGORY}", "name=${HaGlobalConfig.SELF_FENCER_STRATEGY.name}"]
        }[0].value == beforeStrategy
    }

    @Override
    void clean() {
        env.delete()
    }
}
