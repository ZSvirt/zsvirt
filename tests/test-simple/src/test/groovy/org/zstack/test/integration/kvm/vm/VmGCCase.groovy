package org.zstack.test.integration.kvm.vm

import org.springframework.http.HttpEntity
import org.zstack.core.cloudbus.CloudBus
import org.zstack.core.cloudbus.EventFacade
import org.zstack.core.db.DatabaseFacade
import org.zstack.core.gc.GCStatus
import org.zstack.core.gc.GarbageCollectorVO
import org.zstack.header.host.HostCanonicalEvents
import org.zstack.header.host.HostInventory
import org.zstack.header.host.HostStatus
import org.zstack.header.host.HostVO
import org.zstack.header.message.MessageReply
import org.zstack.header.vm.StopVmInstanceMsg
import org.zstack.header.vm.VmInstanceConstant
import org.zstack.header.vm.VmInstanceState
import org.zstack.header.vm.VmInstanceVO
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMConstant
import org.zstack.sdk.DestroyVmInstanceAction
import org.zstack.sdk.GarbageCollectorInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.kvm.Env
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.*

/**
 * Created by xing5 on 2017/3/3.
 */
class VmGCCase extends SubCase {
    EnvSpec env

    DatabaseFacade dbf
    CloudBus bus

    @Override
    void setup() {
        useSpring(KvmTest.springSpec)
    }

    private VmInstanceInventory createGCCandidateDestroyedVm() {
        def vm = createVmInstance {
            name = "the-vm"
            instanceOfferingUuid = (env.specByName("instanceOffering") as InstanceOfferingSpec).inventory.uuid
            imageUuid = (env.specByName("image1") as ImageSpec).inventory.uuid
            l3NetworkUuids = [(env.specByName("l3") as L3NetworkSpec).inventory.uuid]
        } as VmInstanceInventory

        env.afterSimulator(KVMConstant.KVM_DESTROY_VM_PATH) {
            throw new HttpError(403, "on purpose")
        }

        def a = new DestroyVmInstanceAction()
        a.uuid = vm.uuid
        a.sessionId = adminSession()
        DestroyVmInstanceAction.Result res = a.call()
        // because of the GC, confirm the VM is deleted
        assert res.error == null
        assert dbFindByUuid(vm.uuid, VmInstanceVO.class).state == VmInstanceState.Destroyed
        assert dbf.count(GarbageCollectorVO.class) != 0

        return vm
    }

    @Override
    void environment() {
        env = Env.oneVmBasicEnv()
    }

    void testDeleteVmWhenHostDisconnect() {
        VmInstanceInventory vm = (env.specByName("vm") as VmSpec).inventory

        env.afterSimulator(KVMConstant.KVM_DESTROY_VM_PATH) {
            throw new HttpError(403, "on purpose")
        }

        def a = new DestroyVmInstanceAction()
        a.uuid = vm.uuid
        a.sessionId = adminSession()
        DestroyVmInstanceAction.Result res = a.call()
        // because of the GC, confirm the VM is deleted
        assert res.error == null
        assert dbFindByUuid(vm.uuid, VmInstanceVO.class).state == VmInstanceState.Destroyed

        KVMAgentCommands.DestroyVmCmd cmd = null
        env.afterSimulator(KVMConstant.KVM_DESTROY_VM_PATH) { rsp, HttpEntity<String> e ->
            cmd = json(e.body, KVMAgentCommands.DestroyVmCmd.class)
            return rsp
        }

        // the host reconnecting will trigger the GC
        reconnectHost {
            uuid = vm.hostUuid
        }

        GarbageCollectorInventory inv = null

        retryInSecs {
            inv = queryGCJob {
                conditions=["context~=%${vm.uuid}%"]
            }[0]

            assert cmd != null
            assert cmd.uuid == vm.uuid
            assert inv.status == GCStatus.Done.toString()
        }

        deleteGCJob {
            uuid = inv.uuid
        }
    }

    void testGCJobCancelAfterHostDelete() {
        VmInstanceInventory vm = createGCCandidateDestroyedVm()

        deleteHost {
            uuid = vm.hostUuid
        }

        GarbageCollectorInventory inv = null

        retryInSecs {
            inv = queryGCJob {
                conditions=["context~=%${vm.uuid}%"]
            }[0]

            assert inv.status == GCStatus.Done.toString()
        }

        deleteGCJob {
            uuid = inv.uuid
        }
    }

    void testGCJobTriggeredByEventFromOtherManagementNode() {
        VmInstanceInventory vm = createGCCandidateDestroyedVm()

        recoverVmInstance {
            uuid = vm.uuid
        }

        KVMAgentCommands.DestroyVmCmd cmd = null
        env.afterSimulator(KVMConstant.KVM_DESTROY_VM_PATH) { rsp, HttpEntity<String> e ->
            cmd = json(e.body, KVMAgentCommands.DestroyVmCmd.class)
            return rsp
        }

        HostVO host = dbf.findByUuid(vm.hostUuid, HostVO.class)
        HostCanonicalEvents.HostStatusChangedData data = new HostCanonicalEvents.HostStatusChangedData(
                hostUuid: host.uuid,
                oldStatus: HostStatus.Connecting.toString(),
                newStatus: HostStatus.Connected.toString(),
                inventory: HostInventory.valueOf(host)
        )

        EventFacade evtf = bean(EventFacade.class)
        // mimic an event to trigger the GC
        evtf.fire(HostCanonicalEvents.HOST_STATUS_CHANGED_PATH, data)

        GarbageCollectorInventory inv = null

        retryInSecs {
            inv = queryGCJob {
                conditions=["context~=%${vm.uuid}%"]
            }[0]

            // still destroy command sent even if the vm is recovered
            assert cmd != null
            assert inv.status == GCStatus.Done.toString()
        }

        deleteGCJob {
            uuid = inv.uuid
        }
    }

    void testGCJobTriggeredAfterVmRecovered() {
        VmInstanceInventory vm = createGCCandidateDestroyedVm()

        recoverVmInstance {
            uuid = vm.uuid
        }

        KVMAgentCommands.DestroyVmCmd cmd = null
        env.afterSimulator(KVMConstant.KVM_DESTROY_VM_PATH) { rsp, HttpEntity<String> e ->
            cmd = json(e.body, KVMAgentCommands.DestroyVmCmd.class)
            return rsp
        }

        // the host reconnecting will trigger the GC
        reconnectHost {
            uuid = vm.hostUuid
        }

        GarbageCollectorInventory inv = null

        retryInSecs {
            inv = queryGCJob {
                conditions=["context~=%${vm.uuid}%"]
            }[0]

            //destroy command still sent even if the vm is recovered because of some resource of vm released
            assert cmd != null
            assert inv.status == GCStatus.Done.toString()
        }

        deleteGCJob {
            uuid = inv.uuid
        }
    }

    @Override
    void test() {
        dbf = bean(DatabaseFacade.class)
        bus = bean(CloudBus.class)

        env.create {
            testDeleteVmWhenHostDisconnect()
            testGCJobTriggeredByEventFromOtherManagementNode()
            testGCJobTriggeredAfterVmRecovered()
            testGCJobCancelAfterHostDelete()

            // recreate the host
            env.recreate("kvm")

            testStopVmWhenHostDisconnect()
            testStopVmGCJobCancelAfterVmDeleted()
            testStopVmGCJobCancelAfterHostDeleted()
        }
    }

    private void stopVmWithGCOpen(String vmUuid) {
        StopVmInstanceMsg msg = new StopVmInstanceMsg()
        msg.gcOnFailure = true
        msg.vmInstanceUuid = vmUuid
        bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, vmUuid)
        MessageReply reply = bus.call(msg)
        assert reply.success
    }

    private VmInstanceInventory createGCCandidateStoppedVm() {
        def vm = createVmInstance {
            name = "the-vm"
            instanceOfferingUuid = (env.specByName("instanceOffering") as InstanceOfferingSpec).inventory.uuid
            imageUuid = (env.specByName("image1") as ImageSpec).inventory.uuid
            l3NetworkUuids = [(env.specByName("l3") as L3NetworkSpec).inventory.uuid]
        } as VmInstanceInventory

        env.afterSimulator(KVMConstant.KVM_STOP_VM_PATH) {
            throw new HttpError(403, "on purpose")
        }

        stopVmWithGCOpen(vm.uuid)

        assert dbFindByUuid(vm.uuid, VmInstanceVO.class).state == VmInstanceState.Stopped
        assert dbf.count(GarbageCollectorVO.class) != 0

        return vm
    }

    void testStopVmGCJobCancelAfterHostDeleted() {
        VmInstanceInventory vm = createGCCandidateStoppedVm()

        KVMAgentCommands.StopVmCmd cmd = null
        env.afterSimulator(KVMConstant.KVM_STOP_VM_PATH) { rsp, HttpEntity<String> e ->
            cmd = json(e.body, KVMAgentCommands.StopVmCmd.class)
            return rsp
        }

        deleteHost {
            uuid = vm.hostUuid
        }

        GarbageCollectorInventory inv = null

        retryInSecs {
            inv = queryGCJob {
                conditions=["context~=%${vm.uuid}%"]
            }[0]

            assert cmd == null
            assert inv.status == GCStatus.Done.toString()
        }

        deleteGCJob {
            uuid = inv.uuid
        }
    }

    void testStopVmGCJobCancelAfterVmDeleted() {
        VmInstanceInventory vm = createGCCandidateStoppedVm()

        KVMAgentCommands.StopVmCmd cmd = null
        env.afterSimulator(KVMConstant.KVM_STOP_VM_PATH) { rsp, HttpEntity<String> e ->
            cmd = json(e.body, KVMAgentCommands.StopVmCmd.class)
            return rsp
        }

        destroyVmInstance {
            uuid = vm.uuid
        }

        GarbageCollectorInventory inv = null

        retryInSecs {
            // the GC job is cancelled
            inv = queryGCJob {
                conditions=["context~=%${vm.uuid}%"]
            }[0]

            assert cmd == null
            assert inv.status == GCStatus.Done.toString()
        }

        deleteGCJob {
            uuid = inv.uuid
        }
    }

    void testStopVmWhenHostDisconnect() {
        VmInstanceInventory vm = createGCCandidateStoppedVm()

        KVMAgentCommands.StopVmCmd cmd = null
        env.afterSimulator(KVMConstant.KVM_STOP_VM_PATH) { rsp, HttpEntity<String> e ->
            cmd = json(e.body, KVMAgentCommands.StopVmCmd.class)
            return rsp
        }

        // reconnect host to trigger the GC
        reconnectHost {
            uuid = vm.hostUuid
        }

        GarbageCollectorInventory inv = null
        retryInSecs {
            inv = queryGCJob {
                conditions=["context~=%${vm.uuid}%"]
            }[0]

            assert cmd != null
            assert cmd.uuid == vm.uuid
            assert inv.status == GCStatus.Done.toString()
        }

        deleteGCJob {
            uuid = inv.uuid
        }

        // cleanup our vm
        destroyVmInstance {
            uuid = vm.uuid
        }
    }

    @Override
    void clean() {
        env.delete()
    }
}