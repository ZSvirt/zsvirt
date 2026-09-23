package org.zstack.test.integration.kvm.vm

import org.springframework.http.HttpEntity
import org.zstack.compute.vm.VmGlobalConfig
import org.zstack.core.cloudbus.CloudBus
import org.zstack.core.db.SQL
import org.zstack.header.host.HostStatus
import org.zstack.header.host.HostVO
import org.zstack.header.host.HostVO_
import org.zstack.header.vm.DestroyVmInstanceMsg
import org.zstack.header.vm.VmInstanceConstant
import org.zstack.header.vm.VmInstanceDeletionPolicyManager.VmInstanceDeletionPolicy
import org.zstack.header.vm.VmInstanceState
import org.zstack.header.vm.VmInstanceVO
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMConstant
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.kvm.Env
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

import java.util.concurrent.CopyOnWriteArrayList

class VmFirmwareFlashCleanupCase extends SubCase {
    EnvSpec env
    List<Map> cleanupCommands = new CopyOnWriteArrayList<>()
    boolean failCleanup
    String originalDeletionPolicy

    @Override
    void setup() {
        useSpring(KvmTest.springSpec)
    }

    @Override
    void environment() {
        env = Env.noVmEnv()
    }

    @Override
    void test() {
        originalDeletionPolicy = VmGlobalConfig.VM_DELETION_POLICY.value()
        env.create {
            env.afterSimulator(KVMConstant.CLEAN_FIRMWARE_FLASH) { KVMAgentCommands.AgentResponse rsp, HttpEntity<String> e ->
                cleanupCommands.add(json(e.body, LinkedHashMap.class))
                if (failCleanup) {
                    rsp.success = false
                    rsp.error = "NVRAM cleanup is temporarily unavailable"
                }
                return rsp
            }

            testStopAndRecoverableDeletionPreserveNvram()
            testDirectDeletionCleansRunningAndStoppedVm()
            testDatabaseOnlyAndKeepVolumePreserveNvram()
            testFailedCleanupDoesNotBlockDeletion()
            testDisconnectedHostDoesNotBlockExpunge()
        }
    }

    void testStopAndRecoverableDeletionPreserveNvram() {
        [VmInstanceDeletionPolicy.Delay, VmInstanceDeletionPolicy.Never].each { policy ->
            [false, true].each { stopped ->
                def vm = createVmInstance {
                    name = "recoverable-${policy}-${stopped}"
                    imageUuid = env.inventoryByName("image1").uuid
                    instanceOfferingUuid = env.inventoryByName("instanceOffering").uuid
                    l3NetworkUuids = [env.inventoryByName("l3").uuid]
                    systemTags = ["bootMode::UEFI"]
                } as VmInstanceInventory
                if (stopped) {
                    stopVmInstance { uuid = vm.uuid }
                    assert cleanupCommands.findAll { it.vmUuid == vm.uuid }.isEmpty()
                }
                VmGlobalConfig.VM_DELETION_POLICY.updateValue(policy.toString())
                destroyVmInstance { uuid = vm.uuid }
                assert cleanupCommands.findAll { it.vmUuid == vm.uuid }.isEmpty()
                assert dbFindByUuid(vm.uuid, VmInstanceVO.class).state == VmInstanceState.Destroyed

                recoverVmInstance { uuid = vm.uuid }
                startVmInstance { uuid = vm.uuid }
                assert cleanupCommands.findAll { it.vmUuid == vm.uuid }.isEmpty()

                VmGlobalConfig.VM_DELETION_POLICY.updateValue(policy.toString())
                destroyVmInstance { uuid = vm.uuid }
                expungeVmInstance { uuid = vm.uuid }
                assert cleanupCommands.findAll { it.vmUuid == vm.uuid }.size() == 1
            }
        }
    }

    void testDirectDeletionCleansRunningAndStoppedVm() {
        [false, true].each { stopped ->
            def vm = createVmInstance {
                name = "direct-${stopped}"
                imageUuid = env.inventoryByName("image1").uuid
                instanceOfferingUuid = env.inventoryByName("instanceOffering").uuid
                l3NetworkUuids = [env.inventoryByName("l3").uuid]
                systemTags = ["bootMode::UEFI"]
            } as VmInstanceInventory
            if (stopped) {
                stopVmInstance { uuid = vm.uuid }
                assert dbFindByUuid(vm.uuid, VmInstanceVO.class).hostUuid == null
                assert dbFindByUuid(vm.uuid, VmInstanceVO.class).lastHostUuid == vm.hostUuid
            }
            VmGlobalConfig.VM_DELETION_POLICY.updateValue(VmInstanceDeletionPolicy.Direct.toString())
            destroyVmInstance { uuid = vm.uuid }
            assert dbFindByUuid(vm.uuid, VmInstanceVO.class) == null
            assert cleanupCommands.findAll { it.vmUuid == vm.uuid }.size() == 1
        }
    }

    void testDatabaseOnlyAndKeepVolumePreserveNvram() {
        CloudBus bus = bean(CloudBus.class)
        [VmInstanceDeletionPolicy.DBOnly, VmInstanceDeletionPolicy.KeepVolume].each { policy ->
            def vm = createVmInstance {
                name = "preserve-${policy}"
                imageUuid = env.inventoryByName("image1").uuid
                instanceOfferingUuid = env.inventoryByName("instanceOffering").uuid
                l3NetworkUuids = [env.inventoryByName("l3").uuid]
                systemTags = ["bootMode::UEFI"]
            } as VmInstanceInventory
            stopVmInstance { uuid = vm.uuid }
            def msg = new DestroyVmInstanceMsg()
            msg.vmInstanceUuid = vm.uuid
            msg.deletionPolicy = policy
            bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, vm.uuid)
            assert bus.call(msg).success
            assert cleanupCommands.findAll { it.vmUuid == vm.uuid }.isEmpty()
        }
    }

    void testFailedCleanupDoesNotBlockDeletion() {
        def vm = createVmInstance {
            name = "failed-cleanup"
            imageUuid = env.inventoryByName("image1").uuid
            instanceOfferingUuid = env.inventoryByName("instanceOffering").uuid
            l3NetworkUuids = [env.inventoryByName("l3").uuid]
            systemTags = ["bootMode::UEFI"]
        } as VmInstanceInventory
        failCleanup = true
        try {
            VmGlobalConfig.VM_DELETION_POLICY.updateValue(VmInstanceDeletionPolicy.Direct.toString())
            destroyVmInstance { uuid = vm.uuid }
            assert dbFindByUuid(vm.uuid, VmInstanceVO.class) == null
            assert cleanupCommands.findAll { it.vmUuid == vm.uuid }.size() == 1
        } finally {
            failCleanup = false
        }
    }

    void testDisconnectedHostDoesNotBlockExpunge() {
        def vm = createVmInstance {
            name = "disconnected-host"
            imageUuid = env.inventoryByName("image1").uuid
            instanceOfferingUuid = env.inventoryByName("instanceOffering").uuid
            l3NetworkUuids = [env.inventoryByName("l3").uuid]
            systemTags = ["bootMode::UEFI"]
        } as VmInstanceInventory
        stopVmInstance { uuid = vm.uuid }
        VmGlobalConfig.VM_DELETION_POLICY.updateValue(VmInstanceDeletionPolicy.Delay.toString())
        destroyVmInstance { uuid = vm.uuid }
        SQL.New(HostVO.class).eq(HostVO_.uuid, vm.hostUuid)
                .set(HostVO_.status, HostStatus.Disconnected).update()
        try {
            expungeVmInstance { uuid = vm.uuid }
            assert cleanupCommands.findAll { it.vmUuid == vm.uuid }.isEmpty()
            assert dbFindByUuid(vm.uuid, VmInstanceVO.class) == null
        } finally {
            SQL.New(HostVO.class).eq(HostVO_.uuid, vm.hostUuid)
                    .set(HostVO_.status, HostStatus.Connected).update()
        }
    }

    @Override
    void clean() {
        if (originalDeletionPolicy != null) {
            VmGlobalConfig.VM_DELETION_POLICY.updateValue(originalDeletionPolicy)
        }
        env.delete()
    }
}
