package org.zstack.test.integration.platforms.ha.fencer

import org.springframework.web.util.UriComponentsBuilder
import org.zstack.core.Platform
import org.zstack.ha.HaScheduler
import org.zstack.ha.entity.HaCommands
import org.zstack.ha.header.HaConstants
import org.zstack.ha.header.HaGlobalConfig
import org.zstack.ha.header.VmHaLevel
import org.zstack.header.rest.RESTConstant
import org.zstack.header.rest.RESTFacade
import org.zstack.header.vm.VmInstanceState
import org.zstack.kvm.KVMAgentCommands
import org.zstack.sdk.HostInventory
import org.zstack.sdk.ImageInventory
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.sdk.VmSchedHistoryInventory
import org.zstack.test.integration.ZStackTest
import org.zstack.test.integration.platforms.ha.HaTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.gson.JSONObjectUtil

class KvmFencerReportCase extends SubCase {
    EnvSpec env
    L3NetworkInventory l3
    HostInventory kvm1
    ImageInventory image

    @Override
    void setup() {
        useSpring(ZStackTest.springSpec)
    }

    @Override
    void environment() {
        env = HaTest.cephOneCluster()
    }

    @Override
    void test() {
        env.create {
            prepare()
            testKvmFencerReport()
        }
    }

    void prepare() {
        l3 = env.inventoryByName("l3") as L3NetworkInventory
        kvm1 = env.inventoryByName("kvm1") as HostInventory
        image = env.inventoryByName("image1") as ImageInventory

        updateGlobalConfig {
            delegate.category = HaGlobalConfig.CATEGORY
            delegate.name = HaGlobalConfig.ALL.name
            delegate.value = "true"
        }
        assert queryGlobalConfig {
            delegate.conditions = ["category=${HaGlobalConfig.CATEGORY}", "name=${HaGlobalConfig.ALL.name}"]
        }[0].value == "true"
    }

    void testKvmFencerReport() {
        logger.info("Test 001: create a NeverStop VM, expect no fence record")
        VmInstanceInventory vm1 = createVmInstance {
            delegate.name = "vm1"
            delegate.clusterUuid = kvm1.clusterUuid
            delegate.hostUuid = kvm1.uuid
            delegate.imageUuid = image.uuid
            delegate.cpuNum = 1
            delegate.memorySize = gb(1)
            delegate.l3NetworkUuids = [l3.uuid]
            delegate.platform = "Linux"
            delegate.guestOsType = "Linux"
            delegate.architecture = "x86_64"
            delegate.diskAOs = [
                [
                    "boot"        : true,
                    "platform"    : "Linux",
                    "guestOsType" : "Linux",
                    "architecture": "x86_64",
                ],
            ]
        } as VmInstanceInventory
        assert vm1.state == VmInstanceState.Running.toString()
        assert vm1.hostUuid == kvm1.uuid

        setVmInstanceHaLevel {
            delegate.uuid = vm1.uuid
            delegate.level = VmHaLevel.NeverStop.toString()
        }
        def before = zqlQuery("query VmHa where uuid = '${vm1.uuid}'")
        assert before.size() == 1
        assert before[0].haLevel == VmHaLevel.NeverStop.toString()
        assert before[0].lastFencerName == null
        assert before[0].lastFenceHostUuid == null

        logger.info("Test 002: a host business NIC fence report is stored on VmHa")
        RESTFacade restFacade = bean(RESTFacade.class)
        UriComponentsBuilder ub = UriComponentsBuilder.fromHttpUrl(restFacade.getBaseUrl())
        ub.path(RESTConstant.COMMAND_CHANNEL_PATH)
        String url = ub.build().toUriString()
        def header = [(RESTConstant.COMMAND_PATH): HaCommands.ReportVmSelfFencerCmd.PATH]
        def cmd = new HaCommands.ReportVmSelfFencerCmd()
        cmd.hostUuid = kvm1.uuid
        cmd.values = [new HaCommands.ReportVmSelfFencerTuple()]
        cmd.values[0].fencerName = HaConstants.KVM_FENCER_HOST_BUSINESS_NIC
        cmd.values[0].vmUuid = vm1.uuid
        restFacade.syncJsonPost(url, JSONObjectUtil.toJsonString(cmd), header, KVMAgentCommands.AgentResponse.class)

        retryInSecs {
            def fenced = zqlQuery("query VmHa where uuid = '${vm1.uuid}'")
            assert fenced.size() == 1
            assert fenced[0].lastFencerName == HaConstants.KVM_FENCER_HOST_BUSINESS_NIC
            assert fenced[0].lastFenceHostUuid == kvm1.uuid
            assert fenced[0].lastFenceTime != null
        }

        logger.info("Test 003: stop without stopHA, the NeverStop scan starts the VM and records the fence reason")
        stopVmInstance {
            delegate.uuid = vm1.uuid
            delegate.stopHA = "false"
        }
        retryInSecs {
            assert threadFacadeForTest().triggerBlockedPeriodicTask(HaScheduler.TASK_NAME_STOPPED_VM_SCANNER) == null
            assert queryVmInstance {
                delegate.conditions = ["uuid=${vm1.uuid}"]
            }[0].state == VmInstanceState.Running.toString()
        }

        List<VmSchedHistoryInventory> records = null
        retryInSecs {
            records = queryVmSchedHistory {
                delegate.conditions = ["vmInstanceUuid=${vm1.uuid}", "schedType=VMHA"]
            } as List<VmSchedHistoryInventory>
            assert records.size() >= 1
        }
        assert records[0].schedType == "VMHA"
        assert records[0].schedReason == Platform.i18n("host business NIC is down")
    }

    @Override
    void clean() {
        env.delete()
    }
}
