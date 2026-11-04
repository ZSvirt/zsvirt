package org.zstack.test.integration.kvm.vm

import org.springframework.http.HttpEntity
import org.zstack.compute.host.HostGlobalConfig
import org.zstack.compute.vm.StopVmGC
import org.zstack.core.cloudbus.CloudBus
import org.zstack.core.gc.GCStatus
import org.zstack.core.gc.GarbageCollectorVO
import org.zstack.header.host.ChangeHostStateMsg
import org.zstack.header.host.HostConstant
import org.zstack.header.host.HostState
import org.zstack.header.host.HostStatus
import org.zstack.header.host.HostVO
import org.zstack.header.message.MessageReply
import org.zstack.header.vm.*
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMConstant
import org.zstack.sdk.StopVmInstanceAction
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.kvm.Env
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.*
import org.zstack.utils.gson.JSONObjectUtil

/** ZSV-13604: user grace must fail safely without breaking host cleanup. */
class GracefulStopHostCleanupCase extends SubCase {
    EnvSpec env
    CloudBus bus
    String originalMaintenancePolicy
    List<KVMAgentCommands.StopVmCmd> stops = Collections.synchronizedList([])
    Set<String> failStop = Collections.synchronizedSet(new HashSet<String>())

    @Override
    void setup() { useSpring(KvmTest.springSpec) }

    @Override
    void environment() { env = Env.oneVmTwoHostNfsEnv() }

    int gcCount(String uuid) {
        return queryGCJob { conditions = ["context~=%${uuid}%"] }.size()
    }

    MessageReply internalStop(String uuid) {
        // Deliberately leave type at its default grace, even with GC requested.
        StopVmInstanceMsg msg = new StopVmInstanceMsg()
        msg.vmInstanceUuid = uuid
        msg.gcOnFailure = true
        bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, uuid)
        return bus.call(msg)
    }

    MessageReply maintain(String hostUuid, boolean force) {
        ChangeHostStateMsg msg = new ChangeHostStateMsg(hostUuid, "preMaintain")
        msg.justChangeState = false
        msg.forceChange = force
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hostUuid)
        return bus.call(msg)
    }

    void testUserGraceFailure(VmInstanceInventory vm) {
        failStop.add(vm.uuid)
        def action = new StopVmInstanceAction(uuid: vm.uuid, type: "grace", sessionId: adminSession())
        assert action.call().error != null
        assert stops.last().type == "grace"
        assert dbFindByUuid(vm.uuid, VmInstanceVO.class).state == VmInstanceState.Running
        assert gcCount(vm.uuid) == 0
        assert !internalStop(vm.uuid).success
        assert gcCount(vm.uuid) == 0
        failStop.clear()

        // A paused user request must fail before any hypervisor command.
        pauseVmInstance { uuid = vm.uuid }
        int requests = stops.size()
        assert action.call().error != null
        assert stops.size() == requests
        assert dbFindByUuid(vm.uuid, VmInstanceVO.class).state == VmInstanceState.Paused
        assert gcCount(vm.uuid) == 0
    }

    void testMaintenanceAndGcRecovery(VmInstanceInventory vm) {
        // Force migration failure, so maintenance really exercises its stop path.
        env.message(MigrateVmMsg.class) { MigrateVmMsg msg, CloudBus cloudBus ->
            cloudBus.replyErrorByMessageType(msg, "injected migration failure")
        }
        int requests = stops.size()
        assert !maintain(vm.hostUuid, false).success
        assert stops.size() == requests
        assert dbFindByUuid(vm.uuid, VmInstanceVO.class).state == VmInstanceState.Paused
        assert gcCount(vm.uuid) == 0

        // HTTP failure on forced cleanup must schedule GC and allow maintenance.
        failStop.add(vm.uuid)
        MessageReply forced = maintain(vm.hostUuid, true)
        assert forced.success : forced.error
        assert stops.last().uuid == vm.uuid
        assert stops.last().type == "cold"
        assert dbFindByUuid(vm.uuid, VmInstanceVO.class).state == VmInstanceState.Stopped
        def jobs = queryGCJob { conditions = ["context~=%${vm.uuid}%"] }
        assert jobs.size() == 1
        assert jobs[0].status == GCStatus.Idle.toString()
        String context = dbFindByUuid(jobs[0].uuid, GarbageCollectorVO.class).context
        assert JSONObjectUtil.toObject(context, StopVmGC.class).stopType == "cold"
        Map legacyContext = JSONObjectUtil.toObject(context, LinkedHashMap.class)
        legacyContext.remove("stopType")
        assert JSONObjectUtil.toObject(JSONObjectUtil.toJsonString(legacyContext), StopVmGC.class).stopType == null

        // The retry must also retain the authorized hard-stop semantics.
        failStop.clear()
        stops.clear()
        triggerGCJob { uuid = jobs[0].uuid }
        retryInSecs {
            assert queryGCJob { conditions = ["uuid=${jobs[0].uuid}"] }[0].status == GCStatus.Done.toString()
            assert stops.any { it.uuid == vm.uuid && it.type == "cold" }
        }
        deleteGCJob { uuid = jobs[0].uuid }
        changeHostState { uuid = vm.hostUuid; stateEvent = "enable" }
        retryInSecs {
            HostVO host = dbFindByUuid(vm.hostUuid, HostVO.class)
            assert host.state == HostState.Enabled
            assert host.status == HostStatus.Connected
        }
        startVmInstance { uuid = vm.uuid; hostUuid = vm.hostUuid }
    }

    void testHostDeletion(VmInstanceInventory running) {
        VmInstanceInventory paused = createVmInstance {
            name = "paused-host-cleanup"
            instanceOfferingUuid = (env.specByName("instanceOffering") as InstanceOfferingSpec).inventory.uuid
            imageUuid = running.imageUuid
            l3NetworkUuids = [running.defaultL3NetworkUuid]
            hostUuid = running.hostUuid
        }
        pauseVmInstance { uuid = paused.uuid }
        failStop.add(running.uuid)
        stops.clear()
        // NFS retains the VMs: this goes through host OP_STOP, not OP_DELETION.
        deleteHost { uuid = running.hostUuid }
        assert dbFindByUuid(running.hostUuid, HostVO.class) == null
        [running, paused].each { vm ->
            assert stops.any { it.uuid == vm.uuid && it.type == "cold" }
            assert dbFindByUuid(vm.uuid, VmInstanceVO.class).state == VmInstanceState.Stopped
        }
        assert gcCount(running.uuid) == 1
        assert gcCount(paused.uuid) == 0
        retryInSecs {
            // Host deletion cancels the now-unnecessary queued cleanup.
            assert queryGCJob { conditions = ["context~=%${running.uuid}%"] }[0].status == GCStatus.Done.toString()
        }
        failStop.clear()
    }

    @Override
    void test() {
        bus = bean(CloudBus.class)
        env.create {
            originalMaintenancePolicy = HostGlobalConfig.HOST_MAINTENANCE_POLICY.value()
            HostGlobalConfig.HOST_MAINTENANCE_POLICY.updateValue("StopVmOnMigrationFailure")
            env.afterSimulator(KVMConstant.KVM_STOP_VM_PATH) { rsp, HttpEntity<String> entity ->
                def cmd = json(entity.body, KVMAgentCommands.StopVmCmd.class)
                stops.add(cmd)
                if (failStop.contains(cmd.uuid)) {
                    throw new HttpError(403, "injected stop transport failure")
                }
                return rsp
            }
            VmInstanceInventory vm = env.inventoryByName("vm")
            testUserGraceFailure(vm)
            testMaintenanceAndGcRecovery(vm)
            testHostDeletion(vm)
        }
    }

    @Override
    void clean() {
        failStop.clear()
        if (originalMaintenancePolicy != null) {
            HostGlobalConfig.HOST_MAINTENANCE_POLICY.updateValue(originalMaintenancePolicy)
        }
        env.delete()
    }
}
