package org.zstack.test.integration.platforms.ha.config

import org.springframework.http.HttpEntity
import org.springframework.web.util.UriComponentsBuilder
import org.zstack.ha.header.HaGlobalConfig
import org.zstack.ha.header.VmHaLevel
import org.zstack.header.rest.RESTConstant
import org.zstack.header.rest.RESTFacade
import org.zstack.header.vm.VmCreationStrategy
import org.zstack.header.vm.VmInstanceConstant
import org.zstack.header.vm.VmInstanceState
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMConstant
import org.zstack.scheduler.SchedulerConstant
import org.zstack.scheduler.SchedulerType
import org.zstack.sdk.ImageInventory
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.SchedulerJobInventory
import org.zstack.sdk.SchedulerTriggerInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.ZStackTest
import org.zstack.test.integration.platforms.ha.HaTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.gson.JSONObjectUtil

import java.sql.Timestamp

class VmCreationStrategyEffectHaCase extends SubCase {
    EnvSpec env
    VmInstanceInventory vm
    L3NetworkInventory l3
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
            testVmCreationStrategyFromAPI()
            testSchedulerStopVm()
            testKvmReportVmShutdownByGuestEvent()
            testKvmReportVmShutdownByHostEvent()
            testCreateVmWithNeverStopOnSystemTags()
        }
    }

    void prepare() {
        vm = env.inventoryByName("vm") as VmInstanceInventory
        l3 = env.inventoryByName("l3") as L3NetworkInventory
        image = env.inventoryByName("image1") as ImageInventory
    }

    void testVmCreationStrategyFromAPI() {
        logger.info("Test 001: global vm.ha.level NeverStop, a CreateStopped VM inherits NeverStop and inhibition")
        setVmInstanceHaLevel {
            delegate.uuid = vm.uuid
            delegate.level = VmHaLevel.NeverStop.toString()
        }
        updateGlobalConfig {
            delegate.category = HaGlobalConfig.CATEGORY
            delegate.name = HaGlobalConfig.VM_HA_LEVEL.name
            delegate.value = VmHaLevel.NeverStop.toString()
        }

        def newVm = createVmInstance {
            delegate.name = "stopped-by-global"
            delegate.l3NetworkUuids = [l3.uuid]
            delegate.imageUuid = image.uuid
            delegate.cpuNum = 4
            delegate.memorySize = gb(8)
            delegate.strategy = VmCreationStrategy.CreateStopped.toString()
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
                [
                    "size": gb(20),
                ],
            ]
        } as VmInstanceInventory

        assert newVm.state == VmInstanceState.Stopped.toString()
        def records = zqlQuery("query VmHa where uuid = '${newVm.uuid}'")
        assert records.size() == 1
        assert records[0].haLevel == VmHaLevel.NeverStop.toString()
        assert records[0].inhibitionReason == "Before VM stopping on hypervisor"

        logger.info("Test 002: start the VM, expect NeverStop and inhibition cleared")
        startVmInstance {
            delegate.uuid = newVm.uuid
        }
        records = zqlQuery("query VmHa where uuid = '${newVm.uuid}'")
        assert records[0].haLevel == VmHaLevel.NeverStop.toString()
        assert records[0].inhibitionReason == null

        logger.info("Test 004: CreateStopped with ha::NeverStop, expect NeverStop and inhibition")
        newVm = createVmInstance {
            delegate.name = "stopped-by-tag"
            delegate.l3NetworkUuids = [l3.uuid]
            delegate.imageUuid = image.uuid
            delegate.cpuNum = 4
            delegate.memorySize = gb(8)
            delegate.strategy = VmCreationStrategy.CreateStopped.toString()
            delegate.platform = "Linux"
            delegate.guestOsType = "Linux"
            delegate.architecture = "x86_64"
            delegate.systemTags = ["ha::NeverStop"]
            delegate.diskAOs = [
                [
                    "boot"        : true,
                    "platform"    : "Linux",
                    "guestOsType" : "Linux",
                    "architecture": "x86_64",
                ],
            ]
        } as VmInstanceInventory
        assert newVm.state == VmInstanceState.Stopped.toString()
        records = zqlQuery("query VmHa where uuid = '${newVm.uuid}'")
        assert records[0].haLevel == VmHaLevel.NeverStop.toString()
        assert records[0].inhibitionReason == "Before VM stopping on hypervisor"

        destroyVmInstance {
            delegate.uuid = newVm.uuid
        }
        expungeVmInstance {
            delegate.uuid = newVm.uuid
        }
    }

    void testSchedulerStopVm() {
        logger.info("Test 101: a scheduler stop keeps NeverStop and records inhibition")
        if (queryVmInstance { delegate.conditions = ["uuid=${vm.uuid}"] }[0].state == VmInstanceState.Stopped.toString()) {
            startVmInstance {
                delegate.uuid = vm.uuid
            }
        }
        setVmInstanceHaLevel {
            delegate.uuid = vm.uuid
            delegate.level = VmHaLevel.NeverStop.toString()
        }

        KVMAgentCommands.StopVmCmd stopVmCmd = null
        env.afterSimulator(KVMConstant.KVM_STOP_VM_PATH) { rsp, HttpEntity<String> e ->
            stopVmCmd = json(e.body, KVMAgentCommands.StopVmCmd.class)
            return rsp
        }

        SchedulerJobInventory job = createSchedulerJob {
            delegate.targetResourceUuid = vm.uuid
            delegate.name = "stop"
            delegate.type = SchedulerType.STOP_VM
        } as SchedulerJobInventory
        SchedulerTriggerInventory trigger = createSchedulerTrigger {
            delegate.name = "trigger"
            delegate.description = "this is a trigger"
            delegate.repeatCount = 1
            delegate.startTime = new Timestamp(System.currentTimeMillis()).getTime() / 1000 + 2
            delegate.schedulerType = SchedulerConstant.SIMPLE_TYPE_STRING
        } as SchedulerTriggerInventory
        addSchedulerJobToSchedulerTrigger {
            delegate.schedulerJobUuid = job.uuid
            delegate.schedulerTriggerUuid = trigger.uuid
        }

        retryInSecs(25, 5) {
            assert queryVmInstance {
                delegate.conditions = ["uuid=${vm.uuid}"]
            }[0].state == VmInstanceState.Stopped.toString()
            assert stopVmCmd != null
        }
        def records = zqlQuery("query VmHa where uuid = '${vm.uuid}'")
        assert records[0].haLevel == VmHaLevel.NeverStop.toString()
        assert records[0].inhibitionReason == "Before VM stopping on hypervisor"

        startVmInstance {
            delegate.uuid = vm.uuid
        }
    }

    void testKvmReportVmShutdownByGuestEvent() {
        logger.info("Test 201: a guest shutdown report inhibits HA")
        RESTFacade restf = bean(RESTFacade.class)
        UriComponentsBuilder ub = UriComponentsBuilder.fromHttpUrl(restf.getBaseUrl())
        ub.path(RESTConstant.COMMAND_CHANNEL_PATH)
        String url = ub.build().toUriString()

        def header = [(RESTConstant.COMMAND_PATH): KVMConstant.KVM_REPORT_VM_SHUTDOWN_EVENT]
        def cmd = new KVMAgentCommands.ReportVmShutdownEventCmd()
        cmd.vmUuid = vm.uuid
        cmd.detail = VmInstanceConstant.SHUTDOWN_DETAIL_BY_GUEST
        restf.syncJsonPost(url, JSONObjectUtil.toJsonString(cmd), header, KVMAgentCommands.AgentResponse)

        retryInSecs(8, 2) {
            def records = zqlQuery("query VmHa where uuid = '${vm.uuid}'")
            assert records[0].inhibitionReason == "KVM reporting VM shutdown"
        }
    }

    void testKvmReportVmShutdownByHostEvent() {
        logger.info("Test 301: a host shutdown report does not inhibit a VM created with ha::NeverStop")
        def hostVm = createVmInstance {
            delegate.name = "host-shutdown-vm"
            delegate.cpuNum = 1
            delegate.memorySize = gb(1)
            delegate.l3NetworkUuids = [l3.uuid]
            delegate.imageUuid = image.uuid
            delegate.platform = "Linux"
            delegate.guestOsType = "Linux"
            delegate.architecture = "x86_64"
            delegate.systemTags = ["ha::NeverStop"]
            delegate.diskAOs = [
                [
                    "boot"        : true,
                    "platform"    : "Linux",
                    "guestOsType" : "Linux",
                    "architecture": "x86_64",
                ],
            ]
        } as VmInstanceInventory
        def before = zqlQuery("query VmHa where uuid = '${hostVm.uuid}'")
        assert before[0].haLevel == VmHaLevel.NeverStop.toString()
        assert before[0].inhibitionReason == null

        RESTFacade restf = bean(RESTFacade.class)
        UriComponentsBuilder ub = UriComponentsBuilder.fromHttpUrl(restf.getBaseUrl())
        ub.path(RESTConstant.COMMAND_CHANNEL_PATH)
        String url = ub.build().toUriString()
        def header = [(RESTConstant.COMMAND_PATH): KVMConstant.KVM_REPORT_VM_SHUTDOWN_EVENT]
        def cmd = new KVMAgentCommands.ReportVmShutdownEventCmd()
        cmd.vmUuid = hostVm.uuid
        cmd.detail = VmInstanceConstant.SHUTDOWN_DETAIL_BY_HOST
        restf.syncJsonPost(url, JSONObjectUtil.toJsonString(cmd), header, KVMAgentCommands.AgentResponse)

        retryInSecs {
            def records = zqlQuery("query VmHa where uuid = '${hostVm.uuid}'")
            assert records[0].inhibitionReason == null
        }
    }

    void testCreateVmWithNeverStopOnSystemTags() {
        logger.info("Test 401: global level None, CreateStopped with ha::NeverStop still inhibits")
        updateGlobalConfig {
            delegate.category = HaGlobalConfig.CATEGORY
            delegate.name = HaGlobalConfig.VM_HA_LEVEL.name
            delegate.value = VmHaLevel.None.toString()
        }

        def newVm = createVmInstance {
            delegate.name = "vm-stopped-tag"
            delegate.cpuNum = 4
            delegate.memorySize = gb(8)
            delegate.l3NetworkUuids = [l3.uuid]
            delegate.imageUuid = image.uuid
            delegate.strategy = VmCreationStrategy.CreateStopped.toString()
            delegate.platform = "Linux"
            delegate.guestOsType = "Linux"
            delegate.architecture = "x86_64"
            delegate.systemTags = ["ha::NeverStop"]
            delegate.diskAOs = [
                [
                    "boot"        : true,
                    "platform"    : "Linux",
                    "guestOsType" : "Linux",
                    "architecture": "x86_64",
                ],
            ]
        } as VmInstanceInventory
        assert newVm.state == VmInstanceState.Stopped.toString()
        def records = zqlQuery("query VmHa where uuid = '${newVm.uuid}'")
        assert records[0].haLevel == VmHaLevel.NeverStop.toString()
        assert records[0].inhibitionReason == "Before VM stopping on hypervisor"

        destroyVmInstance {
            delegate.uuid = newVm.uuid
        }
        expungeVmInstance {
            delegate.uuid = newVm.uuid
        }

        logger.info("Test 402: an instant-start VM with ha::NeverStop and diskAOs ends without inhibition")
        def newVm2 = createVmInstance {
            delegate.name = "vm-disk-neverstop"
            delegate.cpuNum = 1
            delegate.memorySize = gb(1)
            delegate.l3NetworkUuids = [l3.uuid]
            delegate.imageUuid = image.uuid
            delegate.systemTags = ["ha::NeverStop"]
            delegate.diskAOs = [
                [
                    "boot"        : true,
                    "platform"    : "Linux",
                    "guestOsType" : "Linux",
                    "architecture": "x86_64",
                    "systemTags"  : ["driver::virtio"],
                ],
                [
                    "size": gb(2),
                ],
            ]
        } as VmInstanceInventory
        records = zqlQuery("query VmHa where uuid = '${newVm2.uuid}'")
        assert records[0].haLevel == VmHaLevel.NeverStop.toString()
        assert records[0].inhibitionReason == null

        destroyVmInstance {
            delegate.uuid = newVm2.uuid
        }
        expungeVmInstance {
            delegate.uuid = newVm2.uuid
        }
    }

    @Override
    void clean() {
        env.delete()
    }
}
