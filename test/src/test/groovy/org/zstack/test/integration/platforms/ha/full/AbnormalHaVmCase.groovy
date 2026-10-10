package org.zstack.test.integration.platforms.ha.full

import org.springframework.http.HttpEntity
import org.springframework.web.util.UriComponentsBuilder
import org.zstack.core.db.Q
import org.zstack.ha.header.VmHaLevel
import org.zstack.header.rest.RESTConstant
import org.zstack.header.rest.RESTFacade
import org.zstack.header.vm.VmInstanceState
import org.zstack.header.vm.VmSchedHistoryVO
import org.zstack.header.vm.VmSchedHistoryVO_
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMConstant
import org.zstack.sdk.HostInventory
import org.zstack.sdk.ImageInventory
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.ZStackTest
import org.zstack.test.integration.platforms.ha.HaTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

class AbnormalHaVmCase extends SubCase {
    EnvSpec env

    ImageInventory image1
    L3NetworkInventory l3
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
            testExpungeActuallyStoppedButRunningVm()
            testAbnormallyStoppedHaVMStartOnNewHost()
        }
    }

    void prepare() {
        image1 = env.inventoryByName("image1") as ImageInventory
        l3 = env.inventoryByName("l3") as L3NetworkInventory
        vm = env.inventoryByName("vm") as VmInstanceInventory
    }

    /**
     * from cloud: AbnormalHaVmCase.testExpungeActuallyStoppedVmButRunningOnZStack
     */
    void testExpungeActuallyStoppedButRunningVm() {
        int num = 5

        List<KVMAgentCommands.ReportVmStateCmd> cmdList = []
        env.afterSimulator(KVMConstant.KVM_REPORT_VM_STATE) { HttpEntity<String> e ->
            cmdList.add(json(e.body, KVMAgentCommands.ReportVmStateCmd))
        }

        List<VmInstanceInventory> vmInstanceInventories = []
        for (int i = 0; i < num; i++) {
            def vmInstanceInventory = createVmInstance {
                delegate.name = "test-" + i
                delegate.cpuNum = 1
                delegate.memorySize = gb(1)
                delegate.imageUuid = image1.uuid
                delegate.l3NetworkUuids = [l3.uuid]
            } as VmInstanceInventory

            setVmInstanceHaLevel {
                delegate.uuid = vmInstanceInventory.uuid
                delegate.level = VmHaLevel.NeverStop.toString()
            }

            vmInstanceInventories.add(vmInstanceInventory)
        }

        def vm000 = queryVmInstance {} as List<VmInstanceInventory>
        assert vm000.size() == num + 1 // include this.vm
        assert vm000.any { it.uuid == vm.uuid }

        logger.info("Test 001: mock VM shutdown event report")
        def restf = bean(RESTFacade)

        for (int i = 0; i < num; i++) {
            KVMAgentCommands.ReportVmStateCmd cmd = new KVMAgentCommands.ReportVmStateCmd()
            cmd.hostUuid = vmInstanceInventories.get(i).hostUuid
            cmd.vmState = KVMConstant.KvmVmState.Shutdown.toString()
            cmd.vmUuid = vmInstanceInventories.get(i).uuid

            Map<String, String> header = [(RESTConstant.COMMAND_PATH): KVMConstant.KVM_REPORT_VM_STATE]
            UriComponentsBuilder ub = UriComponentsBuilder.fromHttpUrl(restf.getBaseUrl())
            ub.path(RESTConstant.COMMAND_CHANNEL_PATH)
            String url = ub.build().toUriString()
            new Thread({ restf.syncJsonPost(url, jsonString(cmd), header, String.class) }).start()
        }

        for (int i = 0; i < num; i++) {
            String vmUuid = vmInstanceInventories.get(i).uuid
            destroyVmInstance {
                uuid = vmUuid
            }
            expungeVmInstance {
                uuid = vmUuid
            }
        }

        def vm001 = queryVmInstance {} as List<VmInstanceInventory>
        assert vm001.size() == 1
        assert vm001[0].uuid == vm.uuid
    }

    /**
     * from cloud: AbnormalHaVmCase.testAbnormallyStoppedHaVMStartOnNewHost
     */
    void testAbnormallyStoppedHaVMStartOnNewHost() {
        setVmInstanceHaLevel {
            uuid = vm.uuid
            level = VmHaLevel.NeverStop.toString()
        }

        def vm010 = queryVmInstance {} as List<VmInstanceInventory>
        assert vm010.size() == 1
        assert vm010[0].uuid == vm.uuid
        assert vm010[0].state == VmInstanceState.Running.toString()
        def currentHostUuid = vm010[0].lastHostUuid

        def kvm1 = env.inventoryByName("kvm1") as HostInventory
        def kvm2 = env.inventoryByName("kvm2") as HostInventory

        logger.info("Test 012: mock VM event: report VM shutdown")
        assert !Q.New(VmSchedHistoryVO)
                    .eq(VmSchedHistoryVO_.vmInstanceUuid, vm.uuid)
                    .isExists()
        def restf = bean(RESTFacade)

        KVMAgentCommands.ReportVmStateCmd cmd = new KVMAgentCommands.ReportVmStateCmd()
        cmd.hostUuid = currentHostUuid
        cmd.vmState = KVMConstant.KvmVmState.Shutdown.toString()
        cmd.vmUuid = vm.uuid

        Map<String, String> header = [(RESTConstant.COMMAND_PATH) : KVMConstant.KVM_REPORT_VM_STATE]
        UriComponentsBuilder ub = UriComponentsBuilder.fromHttpUrl(restf.getBaseUrl())
        ub.path(RESTConstant.COMMAND_CHANNEL_PATH)
        String url = ub.build().toUriString()
        restf.syncJsonPost(url, jsonString(cmd), header, String.class)

        retryInSecs {
            assert Q.New(VmSchedHistoryVO)
                    .eq(VmSchedHistoryVO_.vmInstanceUuid, vm.uuid)
                    .isExists()
        }

        VmInstanceInventory vm012 = null
        retryInSecs {
            def vm012List = queryVmInstance {
                delegate.conditions = ["uuid=" + vm.uuid]
            } as List<VmInstanceInventory>
            assert vm012List.size() == 1
            assert vm012List[0].uuid == vm.uuid
            assert vm012List[0].state == VmInstanceState.Running.toString()
            vm012 = vm012List[0]
            return true
        }

        currentHostUuid = vm012.hostUuid

        logger.info("Test 013: mock VM event: report VM shutdown (but in next host)")
        cmd.hostUuid = currentHostUuid == kvm1.uuid ? kvm2.uuid : kvm1.uuid
        restf.syncJsonPost(url, jsonString(cmd), header, String.class)

        VmInstanceInventory vm013 = null
        retryInSecs {
            def vm013List = queryVmInstance {
                delegate.conditions = ["uuid=" + vm.uuid]
            } as List<VmInstanceInventory>
            assert vm013List.size() == 1
            assert vm013List[0].uuid == vm.uuid
            assert vm013List[0].state == VmInstanceState.Running.toString()
            vm013 = vm013List[0]
            return true
        }
    }

    @Override
    void clean() {
        env.delete()
    }
}
