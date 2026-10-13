package org.zstack.test.integration.platforms.ha.dead

import org.springframework.http.HttpEntity
import org.zstack.compute.host.HostGlobalConfig
import org.zstack.ha.entity.HaCommands
import org.zstack.ha.header.HaConstants
import org.zstack.ha.header.HaGlobalConfig
import org.zstack.ha.header.SelfFencerStrategy
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

/**
 * Force strategy does not move the VM to another host in this case.
 * That path used VmHaController to mark the host dead; the current host-down
 * flow only reaches Unknown, same as HaOnDownHostCase.
 */
class PermissiveSelfFencerCase extends SubCase {
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
            testSelfFencerStrategyIsPushedToTheHost()
            testVmStaysOnHostWithPermissiveStrategy()
        }
    }

    void prepare() {
        vm = env.inventoryByName("vm") as VmInstanceInventory
        setVmInstanceHaLevel {
            delegate.uuid = vm.uuid
            delegate.level = VmHaLevel.NeverStop.toString()
        }
        def records = zqlQuery("query VmHa where uuid = '${vm.uuid}'")
        assert records.size() == 1
        assert records[0].haLevel == VmHaLevel.NeverStop.toString()

        updateGlobalConfig {
            delegate.category = HostGlobalConfig.PING_HOST_INTERVAL.category
            delegate.name = HostGlobalConfig.PING_HOST_INTERVAL.name
            delegate.value = 1
        }
    }

    void testSelfFencerStrategyIsPushedToTheHost() {
        HaCommands.SetupSelfFencerCmd cmd = null
        env.afterSimulator(HaConstants.SETUP_SELF_FENCER_PATH) { rsp, HttpEntity<String> e ->
            cmd = json(e.body, HaCommands.SetupSelfFencerCmd.class)
            return rsp
        }

        logger.info("Test 001: Permissive is pushed to the NFS self-fencer")
        updateGlobalConfig {
            delegate.category = HaGlobalConfig.CATEGORY
            delegate.name = HaGlobalConfig.SELF_FENCER_STRATEGY.name
            delegate.value = SelfFencerStrategy.Permissive.toString()
        }
        retryInSecs {
            assert cmd != null
            assert cmd.strategy == SelfFencerStrategy.Permissive.toString()
        }
        assert queryGlobalConfig {
            delegate.conditions = ["category=${HaGlobalConfig.CATEGORY}", "name=${HaGlobalConfig.SELF_FENCER_STRATEGY.name}"]
        }[0].value == SelfFencerStrategy.Permissive.toString()

        logger.info("Test 002: Force is pushed to the NFS self-fencer")
        cmd = null
        updateGlobalConfig {
            delegate.category = HaGlobalConfig.CATEGORY
            delegate.name = HaGlobalConfig.SELF_FENCER_STRATEGY.name
            delegate.value = SelfFencerStrategy.Force.toString()
        }
        retryInSecs {
            assert cmd != null
            assert cmd.strategy == SelfFencerStrategy.Force.toString()
        }
        assert queryGlobalConfig {
            delegate.conditions = ["category=${HaGlobalConfig.CATEGORY}", "name=${HaGlobalConfig.SELF_FENCER_STRATEGY.name}"]
        }[0].value == SelfFencerStrategy.Force.toString()

        logger.info("Test 003: switch back to Permissive before the host-down check")
        cmd = null
        updateGlobalConfig {
            delegate.category = HaGlobalConfig.CATEGORY
            delegate.name = HaGlobalConfig.SELF_FENCER_STRATEGY.name
            delegate.value = SelfFencerStrategy.Permissive.toString()
        }
        retryInSecs {
            assert cmd != null
            assert cmd.strategy == SelfFencerStrategy.Permissive.toString()
        }
    }

    void testVmStaysOnHostWithPermissiveStrategy() {
        def running = queryVmInstance {
            delegate.conditions = ["uuid=${vm.uuid}"]
        } as List<VmInstanceInventory>
        assert running.size() == 1
        assert running[0].state == VmInstanceState.Running.toString()
        String hostUuid = running[0].hostUuid

        logger.info("Test 101: ping failure under Permissive, expect the VM stays Unknown on the same host")
        env.afterSimulator(KVMConstant.KVM_PING_PATH) { KVMAgentCommands.AgentResponse rsp, HttpEntity<String> e ->
            def pingCmd = json(e.body, KVMAgentCommands.PingCmd.class)
            rsp.success = pingCmd.hostUuid != hostUuid
            return rsp
        }

        retryInSecs {
            assert dbFindByUuid(hostUuid, HostVO.class).status == HostStatus.Disconnected
        }
        retryInSecs {
            def unknown = queryVmInstance {
                delegate.conditions = ["uuid=${vm.uuid}"]
            } as List<VmInstanceInventory>
            assert unknown.size() == 1
            assert unknown[0].state == VmInstanceState.Unknown.toString()
            assert unknown[0].hostUuid == hostUuid
        }
        VmInstanceVO vo = dbFindByUuid(vm.uuid, VmInstanceVO.class)
        assert vo.state == VmInstanceState.Unknown
        assert vo.hostUuid == hostUuid

        logger.info("Test 102: host reconnects, expect the VM Running on the same host")
        env.cleanSimulatorAndMessageHandlers()
        reconnectHost {
            delegate.uuid = hostUuid
        }
        retryInSecs {
            assert dbFindByUuid(hostUuid, HostVO.class).status == HostStatus.Connected
        }
        retryInSecs {
            def back = queryVmInstance {
                delegate.conditions = ["uuid=${vm.uuid}"]
            } as List<VmInstanceInventory>
            assert back.size() == 1
            assert back[0].state == VmInstanceState.Running.toString()
            assert back[0].hostUuid == hostUuid
        }
    }

    @Override
    void clean() {
        env.delete()
    }
}
