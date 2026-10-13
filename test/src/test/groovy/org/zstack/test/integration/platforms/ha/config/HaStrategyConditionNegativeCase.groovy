package org.zstack.test.integration.platforms.ha.config

import org.springframework.http.HttpEntity
import org.zstack.compute.host.HostGlobalConfig
import org.zstack.ha.header.HaErrors
import org.zstack.ha.header.HaGlobalConfig
import org.zstack.ha.header.HaStrategyState
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

class HaStrategyConditionNegativeCase extends SubCase {
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
            testUpdateWhileHostDisconnected()
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

    void testUpdateWhileHostDisconnected() {
        updateGlobalConfig {
            delegate.category = HaGlobalConfig.CATEGORY
            delegate.name = HaGlobalConfig.ALL.name
            delegate.value = "true"
        }
        def conditions = zqlQuery("query HaStrategyCondition")
        assert conditions.size() >= 1
        def before = conditions[0].state

        logger.info("Test 001: a disconnected host with an Unknown VM blocks enabling a HA strategy condition")
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
            updateHaStrategyCondition {
                delegate.uuid = conditions[0].uuid
                delegate.state = HaStrategyState.Enable.toString()
            }
        }) {
            assert delegate.code == HaErrors.DISCONNECTED_HOST_HAS_UNKNOWN_VM.toString()
            assert delegate.details.contains(vm.hostUuid)
        }

        def after = zqlQuery("query HaStrategyCondition where uuid = '${conditions[0].uuid}'")
        assert after.size() == 1
        assert after[0].state == before
    }

    @Override
    void clean() {
        env.delete()
    }
}
