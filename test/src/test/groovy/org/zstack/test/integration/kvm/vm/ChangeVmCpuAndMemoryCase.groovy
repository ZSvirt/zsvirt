package org.zstack.test.integration.kvm.vm

import org.springframework.http.HttpEntity
import org.zstack.compute.allocator.HostAllocatorGlobalConfig
import org.zstack.compute.vm.VmGlobalConfig
import org.zstack.compute.vm.VmQuotaConstant
import org.zstack.core.db.DatabaseFacade
import org.zstack.core.db.Q
import org.zstack.core.db.SQL
import org.zstack.header.allocator.HostCapacityVO
import org.zstack.header.allocator.HostCapacityVO_
import org.zstack.header.host.HostVO
import org.zstack.header.network.service.NetworkServiceType
import org.zstack.header.vm.VmInstanceState
import org.zstack.header.vm.VmInstanceVO
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMConstant
import org.zstack.network.securitygroup.SecurityGroupConstant
import org.zstack.network.service.virtualrouter.VirtualRouterConstant
import org.zstack.sdk.AccountInventory
import org.zstack.sdk.HostInventory
import org.zstack.sdk.ImageInventory
import org.zstack.sdk.InstanceOfferingInventory
import org.zstack.sdk.KVMHostInventory
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.UpdateVmInstanceAction
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit
import org.zstack.utils.gson.JSONObjectUtil

/**
 * Created by AlanJager on 2017/4/26.
 */
class ChangeVmCpuAndMemoryCase extends SubCase {
    EnvSpec env
    VmInstanceInventory vm
    DatabaseFacade dbf

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void setup() {
        useSpring(KvmTest.springSpec)
    }

    @Override
    void environment() {
        env = env {
            instanceOffering {
                name = "instanceOffering"
                memory = SizeUnit.GIGABYTE.toByte(2)
                cpu = 1
            }

            instanceOffering {
                name = "instanceOffering2"
                memory = SizeUnit.GIGABYTE.toByte(4)
                cpu = 2
            }

            diskOffering {
                name = "diskOffering"
                diskSize = SizeUnit.GIGABYTE.toByte(20)
            }

            sftpBackupStorage {
                name = "sftp"
                url = "/sftp"
                username = "root"
                password = "password"
                hostname = "localhost"

                image {
                    name = "image1"
                    url = "http://zstack.org/download/test.qcow2"
                }

                image {
                    name = "vr"
                    url = "http://zstack.org/download/vr.qcow2"
                }
            }

            zone {
                name = "zone"
                description = "test"

                cluster {
                    name = "cluster"
                    hypervisorType = "KVM"

                    kvm {
                        name = "kvm"
                        managementIp = "localhost"
                        username = "root"
                        password = "password"
                    }

                    attachPrimaryStorage("local")
                    attachL2Network("l2")
                }

                localPrimaryStorage {
                    name = "local"
                    url = "/local_ps"
                }

                l2NoVlanNetwork {
                    name = "l2"
                    physicalInterface = "eth0"

                    l3Network {
                        name = "l3"

                        service {
                            provider = VirtualRouterConstant.PROVIDER_TYPE
                            types = [NetworkServiceType.DHCP.toString(), NetworkServiceType.DNS.toString()]
                        }

                        service {
                            provider = SecurityGroupConstant.SECURITY_GROUP_PROVIDER_TYPE
                            types = [SecurityGroupConstant.SECURITY_GROUP_NETWORK_SERVICE_TYPE]
                        }

                        ip {
                            startIp = "192.168.100.10"
                            endIp = "192.168.100.100"
                            netmask = "255.255.255.0"
                            gateway = "192.168.100.1"
                        }
                    }

                    l3Network {
                        name = "pubL3"

                        ip {
                            startIp = "12.16.10.10"
                            endIp = "12.16.10.100"
                            netmask = "255.255.255.0"
                            gateway = "12.16.10.1"
                        }
                    }
                }

                virtualRouterOffering {
                    name = "vr"
                    memory = SizeUnit.MEGABYTE.toByte(512)
                    cpu = 2
                    useManagementL3Network("pubL3")
                    usePublicL3Network("pubL3")
                    useImage("vr")
                }

                attachBackupStorage("sftp")
            }

            vm {
                name = "vm"
                useInstanceOffering("instanceOffering")
                useImage("image1")
                useL3Networks("l3")
            }
        }
    }

    @Override
    void test() {
        env.create {
            VmGlobalConfig.NUMA.updateValue(true)
            vm = env.inventoryByName("vm") as VmInstanceInventory

            dbf = bean(DatabaseFacade.class)

            testOnlineChangeCpuAndMemory()
            testChangeCpuAndMemoryWhenVmStopped()
            testChangeCpuWhenVmRunning()
            testChangeMemoryWhenVmRunning()
            testIncreaseMemoryAutoAlignment()
            testRandomIncreaseMemoryCase()
            testNumaGlobalConfig()
            testFailureCameoutAfterAllocateHostCapacityTheCapacityWillBeReturned()
            testCannotFindHostWontMakeChangeVmCpuAndMemoryChainRollback()
            testUpdateCpuOrMemoryWhenVMisUnknownOrDestroy()
            testDecreaseVmCpuAndMemoryReturnFail()
            testVmChangeCpuMemoryQuota()
            testVmChangeCpuMemoryWhenHostAvailableIsZero()
            testCreateVmMeorySizeExceedHostMemorySize()
        }
    }

    /**
     * Test allocate host capacity when cpu/memory on available but require 0 should success
     * 1. Create vm
     * 2. Make host available memory to 0
     * 3. Increase vm cpu success
     * 4. Free host available capacity
     * 5. Make host available cpu to 0
     * 6. Increase vm memory success
     */
    void testVmChangeCpuMemoryWhenHostAvailableIsZero() {
        def image = env.inventoryByName("image1")
        def l3 = env.inventoryByName("l3")
        def instance = env.inventoryByName("instanceOffering") as InstanceOfferingInventory
        def kvm = env.inventoryByName("kvm") as KVMHostInventory

        def capacityVm = createVmInstance {
            name = "cap"
            imageUuid = image.uuid
            l3NetworkUuids = [l3.uuid]
            instanceOfferingUuid = instance.uuid
        } as VmInstanceInventory

        def memoryBefore = Q.New(HostCapacityVO.class)
                .select(HostCapacityVO_.availableMemory)
                .eq(HostCapacityVO_.uuid, kvm.uuid)
                .findValue()
        SQL.New(HostCapacityVO.class)
                .eq(HostCapacityVO_.uuid, kvm.uuid)
                .set(HostCapacityVO_.availableMemory, 0L).update()

        updateVmInstance {
            uuid = capacityVm.uuid
            cpuNum = instance.cpuNum + 1
        }

        SQL.New(HostCapacityVO.class)
                .eq(HostCapacityVO_.uuid, kvm.uuid)
                .set(HostCapacityVO_.availableMemory, memoryBefore).update()


        def cpuBefore = Q.New(HostCapacityVO.class)
                .select(HostCapacityVO_.availableCpu)
                .eq(HostCapacityVO_.uuid, kvm.uuid)
                .findValue()
        SQL.New(HostCapacityVO.class)
                .eq(HostCapacityVO_.uuid, kvm.uuid)
                .set(HostCapacityVO_.availableCpu, 0L).update()

        updateVmInstance {
            uuid = capacityVm.uuid
            memorySize = instance.memorySize + SizeUnit.GIGABYTE.toByte(1)
        }

        SQL.New(HostCapacityVO.class)
                .eq(HostCapacityVO_.uuid, kvm.uuid)
                .set(HostCapacityVO_.availableCpu, cpuBefore).update()
    }

    void testVmChangeCpuMemoryQuota() {
        def image = env.inventoryByName("image1")
        def l3 = env.inventoryByName("l3")
        def instance = env.inventoryByName("instanceOffering")

        def test = createAccount {
            name = "test"
            password = "password"
        } as AccountInventory
        attachPredefineRoles(test.uuid, "vm")

        def session = logInByAccount {
            accountName = "test"
            password = "password"
        }

        updateQuota {
            identityUuid = test.uuid
            name = VmQuotaConstant.VM_RUNNING_MEMORY_SIZE
            value = SizeUnit.GIGABYTE.toByte(3)
        }

        shareResource {
            resourceUuids = [instance.uuid, image.uuid, l3.uuid]
            toPublic = true
        }

        def vm2 = createVmInstance {
            name = "test"
            imageUuid = image.uuid
            l3NetworkUuids = [l3.uuid]
            instanceOfferingUuid = instance.uuid
            sessionId = session.uuid
        }

        updateVmInstance {
            uuid = vm2.uuid
            cpuNum = 10
            sessionId = session.uuid
        }

        updateQuota {
            identityUuid = test.uuid
            name = VmQuotaConstant.VM_RUNNING_CPU_NUM
            value = 10
        }

        updateVmInstance {
            uuid = vm2.uuid
            memorySize = SizeUnit.GIGABYTE.toByte(3)
            sessionId = session.uuid
        }

        expect(AssertionError.class) {
            updateVmInstance {
                uuid = vm2.uuid
                memorySize = SizeUnit.GIGABYTE.toByte(4)
                sessionId = session.uuid
            }
        }

        expect(AssertionError.class) {
            updateVmInstance {
                uuid = vm2.uuid
                cpuNum = 11
                sessionId = session.uuid
            }
        }

    }

    void testOnlineChangeCpuAndMemory() {
        InstanceOfferingInventory instanceOffering = env.inventoryByName("instanceOffering2")
        assert vm.getCpuNum() == 1
        assert vm.getMemorySize() == SizeUnit.GIGABYTE.toByte(2)

        KVMAgentCommands.IncreaseCpuCmd cmd = null
        env.afterSimulator(KVMConstant.KVM_VM_ONLINE_INCREASE_CPU) { KVMAgentCommands.IncreaseCpuResponse rsp, HttpEntity<String> e ->
            cmd = JSONObjectUtil.toObject(e.body, KVMAgentCommands.IncreaseCpuCmd.class)
            rsp.cpuNum = instanceOffering.cpuNum
            rsp.success = true
            return rsp
        }

        KVMAgentCommands.IncreaseMemoryCmd cmd2 = null
        env.afterSimulator(KVMConstant.KVM_VM_ONLINE_INCREASE_MEMORY) { KVMAgentCommands.IncreaseMemoryResponse rsp, HttpEntity<String> e ->
            cmd2 = JSONObjectUtil.toObject(e.body, KVMAgentCommands.IncreaseMemoryCmd.class)
            rsp.memorySize = instanceOffering.memorySize
            rsp.success = true
            return rsp
        }

        VmInstanceInventory result = changeInstanceOffering {
            vmInstanceUuid = vm.uuid
            instanceOfferingUuid = instanceOffering.uuid
        }

        retryInSecs {
            assert cmd != null
            assert cmd2 != null
            assert result.cpuNum == 2
            assert result.memorySize == SizeUnit.GIGABYTE.toByte(4)
            assert result.instanceOfferingUuid == instanceOffering.uuid
        }

        env.cleanAfterSimulatorHandlers()
    }

    void testChangeCpuAndMemoryWhenVmStopped() {
        InstanceOfferingInventory instanceOffering = env.inventoryByName("instanceOffering")

        KVMAgentCommands.IncreaseCpuCmd cmd = null
        env.afterSimulator(KVMConstant.KVM_VM_ONLINE_INCREASE_CPU) { KVMAgentCommands.IncreaseCpuResponse rsp, HttpEntity<String> e ->
            cmd = JSONObjectUtil.toObject(e.body, KVMAgentCommands.IncreaseCpuCmd.class)
            rsp.cpuNum = instanceOffering.cpuNum
            rsp.success = true
            return rsp
        }

        KVMAgentCommands.IncreaseMemoryCmd cmd2 = null
        env.afterSimulator(KVMConstant.KVM_VM_ONLINE_INCREASE_MEMORY) { KVMAgentCommands.IncreaseMemoryResponse rsp, HttpEntity<String> e ->
            cmd2 = JSONObjectUtil.toObject(e.body, KVMAgentCommands.IncreaseMemoryCmd.class)
            rsp.memorySize = instanceOffering.memorySize
            rsp.success = true
            return rsp
        }

        stopVmInstance {
            uuid = vm.uuid
        }
        VmInstanceVO vo = dbFindByUuid(vm.uuid, VmInstanceVO.class)
        assert vo.state == VmInstanceState.Stopped

        VmInstanceInventory result = changeInstanceOffering {
            vmInstanceUuid = vm.uuid
            instanceOfferingUuid = instanceOffering.uuid
        }
        assert result.cpuNum == 1
        assert result.memorySize == SizeUnit.GIGABYTE.toByte(2)

        env.cleanAfterSimulatorHandlers()
    }

    void testChangeCpuWhenVmRunning() {
        KVMAgentCommands.IncreaseCpuCmd cmd = null
        env.afterSimulator(KVMConstant.KVM_VM_ONLINE_INCREASE_CPU) { KVMAgentCommands.IncreaseCpuResponse rsp, HttpEntity<String> e ->
            cmd = JSONObjectUtil.toObject(e.body, KVMAgentCommands.IncreaseCpuCmd.class)
            rsp.cpuNum = 8
            rsp.success = true
            return rsp
        }

        KVMAgentCommands.StartVmCmd startCmd = null
        env.afterSimulator(KVMConstant.KVM_START_VM_PATH) { rsp, HttpEntity<String> e ->
            startCmd = JSONObjectUtil.toObject(e.body, KVMAgentCommands.StartVmCmd.class)
            return rsp
        }

        startVmInstance {
            uuid = vm.uuid
        }
        VmInstanceVO vo = dbFindByUuid(vm.uuid, VmInstanceVO.class)
        assert vo.state == VmInstanceState.Running
        assert startCmd.maxVcpuNum == 128

        VmInstanceInventory vmInstanceInventory = updateVmInstance {
            uuid = vm.uuid
            cpuNum = 8
        }

        retryInSecs {
            assert vmInstanceInventory.cpuNum == 8
            assert cmd != null
        }

        env.cleanAfterSimulatorHandlers()
    }

    void testChangeMemoryWhenVmRunning() {
        KVMAgentCommands.IncreaseMemoryCmd cmd = null
        env.afterSimulator(KVMConstant.KVM_VM_ONLINE_INCREASE_MEMORY) { KVMAgentCommands.IncreaseMemoryResponse rsp, HttpEntity<String> e ->
            cmd = JSONObjectUtil.toObject(e.body, KVMAgentCommands.IncreaseMemoryCmd.class)
            rsp.memorySize = SizeUnit.GIGABYTE.toByte(8)
            rsp.success = true
            return rsp
        }

        VmInstanceVO vo = dbFindByUuid(vm.uuid, VmInstanceVO.class)
        assert vo.state == VmInstanceState.Running

        updateVmInstance {
            uuid = vm.uuid
            memorySize = SizeUnit.GIGABYTE.toByte(8)
        }

        retryInSecs {
            vo = dbFindByUuid(vm.uuid, VmInstanceVO.class)
            assert vo.memorySize == SizeUnit.GIGABYTE.toByte(8)
            assert cmd != null
        }

        env.cleanAfterSimulatorHandlers()
    }

    void testIncreaseMemoryAutoAlignment() {
        HostInventory host = env.inventoryByName("kvm")

        stopVmInstance {
            uuid = vm.uuid
        }
        VmInstanceVO vo = dbFindByUuid(vm.uuid, VmInstanceVO.class)
        assert vo.state == VmInstanceState.Stopped

        updateVmInstance {
            uuid = vm.uuid
            memorySize = SizeUnit.MEGABYTE.toByte(1)
        }

        startVmInstance {
            uuid = vm.uuid
        }
        vo = dbFindByUuid(vm.uuid, VmInstanceVO.class)
        assert vo.state == VmInstanceState.Running

        KVMAgentCommands.IncreaseMemoryCmd cmd = null
        env.afterSimulator(KVMConstant.KVM_VM_ONLINE_INCREASE_MEMORY) { KVMAgentCommands.IncreaseMemoryResponse rsp, HttpEntity<String> e ->
            cmd = JSONObjectUtil.toObject(e.body, KVMAgentCommands.IncreaseMemoryCmd.class)
            rsp.memorySize = cmd.memorySize
            rsp.success = true
            return rsp
        }

        updateVmInstance {
            uuid = vm.uuid
            memorySize = SizeUnit.MEGABYTE.toByte(1)
        }

        // test increase 1MB
        assert cmd == null
        UpdateVmInstanceAction updateVmInstanceAction = new UpdateVmInstanceAction()
        updateVmInstanceAction.uuid = vm.uuid
        updateVmInstanceAction.memorySize = SizeUnit.MEGABYTE.toByte(1) + SizeUnit.MEGABYTE.toByte(1)
        updateVmInstanceAction.sessionId = adminSession()
        UpdateVmInstanceAction.Result updateVmInstanceResult = updateVmInstanceAction.call()
        assert updateVmInstanceResult.error == null
        assert cmd != null
        assert cmd.memorySize == (long) SizeUnit.MEGABYTE.toByte(1) + (long) SizeUnit.MEGABYTE.toByte(128)

        // test increase 64MB align to 128MB
        cmd = null
        UpdateVmInstanceAction updateVmInstanceAction2 = new UpdateVmInstanceAction()
        updateVmInstanceAction2.uuid = vm.uuid
        updateVmInstanceAction2.memorySize = (long) SizeUnit.MEGABYTE.toByte(1) + (long) SizeUnit.MEGABYTE.toByte(128) + SizeUnit.MEGABYTE.toByte(64)
        updateVmInstanceAction2.sessionId = adminSession()
        UpdateVmInstanceAction.Result updateVmInstanceResult2 = updateVmInstanceAction2.call()
        assert updateVmInstanceResult2.error == null

        assert SizeUnit.MEGABYTE.toByte(64) % SizeUnit.MEGABYTE.toByte(128) == SizeUnit.MEGABYTE.toByte(128) / 2
        assert ((((long) (SizeUnit.MEGABYTE.toByte(64))/ SizeUnit.MEGABYTE.toByte(128)) + (long) 1) * SizeUnit.MEGABYTE.toByte(128)
        + (long) SizeUnit.MEGABYTE.toByte(1) + (long) SizeUnit.MEGABYTE.toByte(128)) == cmd.memorySize
        assert ((long) SizeUnit.MEGABYTE.toByte(1) + SizeUnit.MEGABYTE.toByte(256)) == cmd.memorySize
        assert cmd != null

        // test increase 63MB align to 128MB
        cmd = null
        UpdateVmInstanceAction updateVmInstanceAction3 = new UpdateVmInstanceAction()
        updateVmInstanceAction3.uuid = vm.uuid
        updateVmInstanceAction3.memorySize = (long) SizeUnit.MEGABYTE.toByte(1) + SizeUnit.MEGABYTE.toByte(256) + SizeUnit.MEGABYTE.toByte(63)
        updateVmInstanceAction3.sessionId = adminSession()
        UpdateVmInstanceAction.Result updateVmInstanceResult3 = updateVmInstanceAction3.call()
        assert updateVmInstanceResult3.error == null

        assert SizeUnit.MEGABYTE.toByte(63) % SizeUnit.MEGABYTE.toByte(128) < SizeUnit.MEGABYTE.toByte(128) / 2
        assert (((long) (SizeUnit.MEGABYTE.toByte(63)) / SizeUnit.MEGABYTE.toByte(128) + (long) 1) * SizeUnit.MEGABYTE.toByte(128)
        + (long) SizeUnit.MEGABYTE.toByte(1) + SizeUnit.MEGABYTE.toByte(256)) == cmd.memorySize
        assert cmd.memorySize == SizeUnit.MEGABYTE.toByte(1) + SizeUnit.MEGABYTE.toByte(384)
        assert cmd != null

        // test increase 129MB align to 128MB
        cmd = null
        UpdateVmInstanceAction updateVmInstanceAction4 = new UpdateVmInstanceAction()
        updateVmInstanceAction4.uuid = vm.uuid
        updateVmInstanceAction4.memorySize = (long) SizeUnit.MEGABYTE.toByte(1) + SizeUnit.MEGABYTE.toByte(384) + SizeUnit.MEGABYTE.toByte(129)
        updateVmInstanceAction4.sessionId = adminSession()
        UpdateVmInstanceAction.Result updateVmInstanceResult4 = updateVmInstanceAction4.call()
        assert updateVmInstanceResult4.error == null

        assert SizeUnit.MEGABYTE.toByte(129) % SizeUnit.MEGABYTE.toByte(128) < SizeUnit.MEGABYTE.toByte(128) / 2
        assert (((long) (SizeUnit.MEGABYTE.toByte(129)) / SizeUnit.MEGABYTE.toByte(128)) * SizeUnit.MEGABYTE.toByte(128) + (long) SizeUnit.MEGABYTE.toByte(1) + SizeUnit.MEGABYTE.toByte(384)) == cmd.memorySize
        assert cmd.memorySize == SizeUnit.MEGABYTE.toByte(1) + SizeUnit.MEGABYTE.toByte(512)
        assert cmd != null


        // test increase 128MB
        cmd = null
        UpdateVmInstanceAction updateVmInstanceAction5 = new UpdateVmInstanceAction()
        updateVmInstanceAction5.uuid = vm.uuid
        updateVmInstanceAction5.memorySize = (long) SizeUnit.MEGABYTE.toByte(1) + SizeUnit.MEGABYTE.toByte(512) + SizeUnit.MEGABYTE.toByte(128)
        updateVmInstanceAction5.sessionId = adminSession()
        UpdateVmInstanceAction.Result updateVmInstanceResult5 = updateVmInstanceAction5.call()
        assert updateVmInstanceResult5.error == null

        assert SizeUnit.MEGABYTE.toByte(128) % SizeUnit.MEGABYTE.toByte(128) == 0
        assert cmd.memorySize == SizeUnit.MEGABYTE.toByte(1) + SizeUnit.MEGABYTE.toByte(640)
        assert cmd != null

        // test increase 193MB align to 256MB
        cmd = null
        UpdateVmInstanceAction updateVmInstanceAction6 = new UpdateVmInstanceAction()
        updateVmInstanceAction6.uuid = vm.uuid
        updateVmInstanceAction6.memorySize = (long) SizeUnit.MEGABYTE.toByte(1) + SizeUnit.MEGABYTE.toByte(640) + SizeUnit.MEGABYTE.toByte(193)
        updateVmInstanceAction6.sessionId = adminSession()
        UpdateVmInstanceAction.Result updateVmInstanceResult6 = updateVmInstanceAction6.call()
        assert updateVmInstanceResult6.error == null

        assert SizeUnit.MEGABYTE.toByte(193) % SizeUnit.MEGABYTE.toByte(128) > SizeUnit.MEGABYTE.toByte(128) / 2
        assert ((((long) (SizeUnit.MEGABYTE.toByte(193)) / SizeUnit.MEGABYTE.toByte(128) + (long) 1)) * SizeUnit.MEGABYTE.toByte(128) + SizeUnit.MEGABYTE.toByte(640) + (long) SizeUnit.MEGABYTE.toByte(1)) == cmd.memorySize
        assert cmd.memorySize == SizeUnit.MEGABYTE.toByte(1) + SizeUnit.MEGABYTE.toByte(640) + SizeUnit.MEGABYTE.toByte(256)
        assert cmd != null


        // test update vm memory to 31GB align to 31GB + 128MB
        cmd = null
        UpdateVmInstanceAction updateVmInstanceAction7 = new UpdateVmInstanceAction()
        updateVmInstanceAction7.uuid = vm.uuid
        updateVmInstanceAction7.memorySize = (long) SizeUnit.GIGABYTE.toByte(31)
        updateVmInstanceAction7.sessionId = adminSession()
        UpdateVmInstanceAction.Result updateVmInstanceResult7 = updateVmInstanceAction7.call()
        assert updateVmInstanceResult7.error == null

        long increase = (long) SizeUnit.GIGABYTE.toByte(31) - (long) SizeUnit.MEGABYTE.toByte(1) - SizeUnit.MEGABYTE.toByte(896)
        assert increase % SizeUnit.MEGABYTE.toByte(128) > SizeUnit.MEGABYTE.toByte(128) / 2
        assert ((((increase / SizeUnit.MEGABYTE.toByte(128)) as long) + (long) 1) * SizeUnit.MEGABYTE.toByte(128))
        + SizeUnit.MEGABYTE.toByte(1) + SizeUnit.MEGABYTE.toByte(896) == cmd.memorySize
        assert cmd != null

        stopVmInstance {
            uuid = vm.uuid
        }
        vo = dbFindByUuid(vm.uuid, VmInstanceVO.class)
        assert vo.state == VmInstanceState.Stopped

        updateVmInstance {
            uuid = vm.uuid
            memorySize = SizeUnit.GIGABYTE.toByte(8)
        }

        startVmInstance {
            uuid = vm.uuid
        }
        vo = dbFindByUuid(vm.uuid, VmInstanceVO.class)
        assert vo.state == VmInstanceState.Running
        env.cleanAfterSimulatorHandlers()
    }

    void testRandomIncreaseMemoryCase() {
        stopVmInstance {
            uuid = vm.uuid
        }
        VmInstanceVO vo = dbFindByUuid(vm.uuid, VmInstanceVO.class)
        assert vo.state == VmInstanceState.Stopped

        updateVmInstance {
            uuid = vm.uuid
            memorySize = SizeUnit.BYTE.toByte(1)
        }

        startVmInstance {
            uuid = vm.uuid
        }
        vo = dbFindByUuid(vm.uuid, VmInstanceVO.class)
        assert vo.state == VmInstanceState.Running

        def random = new Random()
        10.times{
            checkAlignmentCondition(SizeUnit.MEGABYTE.toByte(random.nextInt(1000)))
        }

        stopVmInstance {
            uuid = vm.uuid
        }
        vo = dbFindByUuid(vm.uuid, VmInstanceVO.class)
        assert vo.state == VmInstanceState.Stopped

        updateVmInstance {
            uuid = vm.uuid
            memorySize = SizeUnit.GIGABYTE.toByte(8)
        }

        startVmInstance {
            uuid = vm.uuid
        }
        vo = dbFindByUuid(vm.uuid, VmInstanceVO.class)
        assert vo.state == VmInstanceState.Running
        env.cleanAfterSimulatorHandlers()
    }

    void checkAlignmentCondition(long increaseMem) {
        KVMAgentCommands.IncreaseMemoryCmd cmd = null
        env.afterSimulator(KVMConstant.KVM_VM_ONLINE_INCREASE_MEMORY) { KVMAgentCommands.IncreaseMemoryResponse rsp, HttpEntity<String> e ->
            cmd = JSONObjectUtil.toObject(e.body, KVMAgentCommands.IncreaseMemoryCmd.class)
            rsp.memorySize = cmd.memorySize
            rsp.success = true
            return rsp
        }

        VmInstanceVO vo = dbFindByUuid(vm.uuid, VmInstanceVO.class)

        UpdateVmInstanceAction updateVmInstanceAction = new UpdateVmInstanceAction()
        updateVmInstanceAction.uuid = vm.uuid
        updateVmInstanceAction.memorySize = vo.getMemorySize() + increaseMem
        updateVmInstanceAction.sessionId = adminSession()
        UpdateVmInstanceAction.Result updateVmInstanceResult = updateVmInstanceAction.call()
        assert updateVmInstanceResult.error == null

        if (increaseMem % SizeUnit.MEGABYTE.toByte(128) == 0 as long) {
            assert increaseMem == 0L ? cmd == null : cmd != null
            if (cmd != null) {
                assert cmd.memorySize == vo.getMemorySize() + increaseMem
            }
        } else {
            def reminder = increaseMem % SizeUnit.MEGABYTE.toByte(128) as long
            if (reminder < SizeUnit.MEGABYTE.toByte(128) / 2) {
                if (increaseMem > SizeUnit.MEGABYTE.toByte(128)) {
                    assert cmd != null
                    assert cmd.memorySize == vo.getMemorySize() + (increaseMem / SizeUnit.MEGABYTE.toByte(128) as long) * SizeUnit.MEGABYTE.toByte(128)
                } else {
                    assert cmd != null
                    assert cmd.memorySize == vo.getMemorySize() + SizeUnit.MEGABYTE.toByte(128)
                }
            } else {
                assert cmd != null
                assert cmd.memorySize == vo.getMemorySize() + ((increaseMem / SizeUnit.MEGABYTE.toByte(128) as long) + 1) * SizeUnit.MEGABYTE.toByte(128)
            }
        }
    }

    void testNumaGlobalConfig() {
        VmGlobalConfig.NUMA.updateValue(false)

        UpdateVmInstanceAction updateVmInstanceAction = new UpdateVmInstanceAction()
        updateVmInstanceAction.uuid = vm.uuid
        updateVmInstanceAction.cpuNum = 100
        updateVmInstanceAction.sessionId = adminSession()
        UpdateVmInstanceAction.Result updateVmInstanceResult = updateVmInstanceAction.call()
        assert updateVmInstanceResult.error != null

        VmGlobalConfig.NUMA.updateValue(true)

        UpdateVmInstanceAction updateVmInstanceAction2 = new UpdateVmInstanceAction()
        updateVmInstanceAction2.uuid = vm.uuid
        updateVmInstanceAction2.memorySize = SizeUnit.GIGABYTE.toByte(8)
        updateVmInstanceAction2.sessionId = adminSession()
        UpdateVmInstanceAction.Result updateVmInstanceResult2 = updateVmInstanceAction2.call()
        assert updateVmInstanceResult2.error == null
    }

    void testDecreaseVmCpuAndMemoryReturnFail() {
        UpdateVmInstanceAction updateVmInstanceAction = new UpdateVmInstanceAction()
        updateVmInstanceAction.uuid = vm.uuid
        updateVmInstanceAction.cpuNum = 1
        updateVmInstanceAction.sessionId = adminSession()
        UpdateVmInstanceAction.Result updateVmInstanceResult = updateVmInstanceAction.call()
        assert updateVmInstanceResult.error != null

        UpdateVmInstanceAction updateVmInstanceAction2 = new UpdateVmInstanceAction()
        updateVmInstanceAction2.uuid = vm.uuid
        updateVmInstanceAction2.memorySize = SizeUnit.GIGABYTE.toByte(2)
        updateVmInstanceAction2.sessionId = adminSession()
        UpdateVmInstanceAction.Result updateVmInstanceResult2 = updateVmInstanceAction2.call()
        assert updateVmInstanceResult2.error != null
    }

    void testFailureCameoutAfterAllocateHostCapacityTheCapacityWillBeReturned() {
        HostInventory host = env.inventoryByName("kvm")

        KVMAgentCommands.IncreaseCpuCmd cmd = null
        env.afterSimulator(KVMConstant.KVM_VM_ONLINE_INCREASE_CPU) { KVMAgentCommands.IncreaseCpuResponse rsp, HttpEntity<String> e ->
            cmd = JSONObjectUtil.toObject(e.body, KVMAgentCommands.IncreaseCpuCmd.class)
            rsp.error = "on purpose"
            rsp.success = false
            return rsp
        }

        UpdateVmInstanceAction updateVmInstanceAction = new UpdateVmInstanceAction()
        updateVmInstanceAction.uuid = vm.uuid
        updateVmInstanceAction.cpuNum = 9
        updateVmInstanceAction.memorySize = SizeUnit.GIGABYTE.toByte(10)
        updateVmInstanceAction.sessionId = adminSession()
        UpdateVmInstanceAction.Result updateVmInstanceResult = updateVmInstanceAction.call()
        assert updateVmInstanceResult.error != null

        HostVO vo
        retryInSecs {
            vo = dbFindByUuid(host.uuid, HostVO.class)
            assert vo.getCapacity().getAvailableCpu() == vo.getCapacity().getTotalCpu() - 10
            assert vo.getCapacity().getAvailableMemory() == vo.getCapacity().getTotalMemory() - SizeUnit.GIGABYTE.toByte(8) - SizeUnit.MEGABYTE.toByte(512)
            assert cmd != null
        }

        env.cleanAfterSimulatorHandlers()
    }

    void testCannotFindHostWontMakeChangeVmCpuAndMemoryChainRollback() {
        HostInventory host = env.inventoryByName("kvm")
        HostVO vo = dbFindByUuid(host.uuid, HostVO.class)

        UpdateVmInstanceAction updateVmInstanceAction = new UpdateVmInstanceAction()
        updateVmInstanceAction.uuid = vm.uuid
        updateVmInstanceAction.cpuNum = vo.getCapacity().getAvailableCpu() + 10
        updateVmInstanceAction.sessionId = adminSession()
        UpdateVmInstanceAction.Result updateVmInstanceResult = updateVmInstanceAction.call()
        assert updateVmInstanceResult.error != null

        retryInSecs {
            vo = dbFindByUuid(host.uuid, HostVO.class)
            assert vo.getCapacity().getAvailableCpu() == vo.getCapacity().getTotalCpu() - 10
            assert vo.getCapacity().getAvailableMemory() == vo.getCapacity().getTotalMemory() - SizeUnit.GIGABYTE.toByte(8) - SizeUnit.MEGABYTE.toByte(512)
        }

        env.cleanAfterSimulatorHandlers()
    }

    void testUpdateCpuOrMemoryWhenVMisUnknownOrDestroy() {
        VmInstanceVO vo = dbFindByUuid(vm.uuid, VmInstanceVO.class)
        vo.setState(VmInstanceState.Unknown)
        dbf.updateAndRefresh(vo)
        vo = dbFindByUuid(vm.uuid, VmInstanceVO.class)
        assert vo.state == VmInstanceState.Unknown
        UpdateVmInstanceAction updateVmInstanceAction = new UpdateVmInstanceAction()
        updateVmInstanceAction.uuid = vm.uuid
        updateVmInstanceAction.cpuNum = 20
        updateVmInstanceAction.sessionId = adminSession()
        UpdateVmInstanceAction.Result updateVmInstanceResult = updateVmInstanceAction.call()
        assert updateVmInstanceResult.error != null

        vo = dbFindByUuid(vm.uuid, VmInstanceVO.class)
        vo.setState(VmInstanceState.Destroyed)
        dbf.updateAndRefresh(vo)
        vo = dbFindByUuid(vm.uuid, VmInstanceVO.class)
        assert vo.state == VmInstanceState.Destroyed
        UpdateVmInstanceAction updateVmInstanceAction2 = new UpdateVmInstanceAction()
        updateVmInstanceAction2.uuid = vm.uuid
        updateVmInstanceAction2.cpuNum = 20
        updateVmInstanceAction2.sessionId = adminSession()
        UpdateVmInstanceAction.Result updateVmInstanceResult2 = updateVmInstanceAction.call()
        assert updateVmInstanceResult2.error != null

        vo = dbFindByUuid(vm.uuid, VmInstanceVO.class)
        vo.setState(VmInstanceState.Running)
        dbf.updateAndRefresh(vo)
        vo = dbFindByUuid(vm.uuid, VmInstanceVO.class)
        assert vo.state == VmInstanceState.Running
    }

    void testCreateVmMeorySizeExceedHostMemorySize() {
        def image = env.inventoryByName("image1") as ImageInventory
        def l3 = env.inventoryByName("l3") as L3NetworkInventory
        def instance = env.inventoryByName("instanceOffering") as InstanceOfferingInventory
        def instance2 = env.inventoryByName("instanceOffering2") as InstanceOfferingInventory
        def host = env.inventoryByName("kvm") as HostInventory

        HostCapacityVO capacity = Q.New(HostCapacityVO.class).eq(HostCapacityVO_.uuid, host.uuid).find()
        SQL.New(HostCapacityVO.class).eq(HostCapacityVO_.uuid, host.uuid).set(HostCapacityVO_.totalPhysicalMemory, instance2.memorySize).update()

        expect (AssertionError.class) {
            createVmInstance {
                name = "test-memory"
                imageUuid = image.uuid
                l3NetworkUuids = [l3.uuid]
                instanceOfferingUuid = instance2.uuid
            }
        }

        HostAllocatorGlobalConfig.HOST_ALLOCATOR_MAX_MEMORY.updateValue(false)
        VmInstanceInventory vm = createVmInstance {
            name = "test-memory"
            imageUuid = image.uuid
            l3NetworkUuids = [l3.uuid]
            instanceOfferingUuid = instance2.uuid
        }

        destroyVmInstance{
            uuid = vm.uuid
        }

        VmInstanceInventory vm2 = createVmInstance {
            name = "update-memory"
            imageUuid = image.uuid
            l3NetworkUuids = [l3.uuid]
            instanceOfferingUuid = instance.uuid
        }

        HostAllocatorGlobalConfig.HOST_ALLOCATOR_MAX_MEMORY.updateValue(true)
        expect (AssertionError.class) {
            updateVmInstance {
                uuid = vm2.uuid
                memorySize = instance2.memorySize
            }
        }

        HostAllocatorGlobalConfig.HOST_ALLOCATOR_MAX_MEMORY.updateValue(false)
        updateVmInstance {
            uuid = vm2.uuid
            memorySize = instance2.memorySize
        }

        destroyVmInstance{
            uuid = vm2.uuid
        }

        SQL.New(HostCapacityVO.class).eq(HostCapacityVO_.uuid, host.uuid)
                .set(HostCapacityVO_.totalPhysicalMemory, capacity.totalPhysicalMemory).update()
    }

}
