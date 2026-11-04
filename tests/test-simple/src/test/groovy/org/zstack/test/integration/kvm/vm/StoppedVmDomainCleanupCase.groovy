package org.zstack.test.integration.kvm.vm

import org.springframework.http.HttpEntity
import org.zstack.core.gc.GCStatus
import org.zstack.core.db.SQL
import org.zstack.header.host.HostStatus
import org.zstack.header.host.HostVO
import org.zstack.header.host.HostVO_
import org.zstack.compute.vm.VmDestroyOnHypervisorFlow
import org.zstack.core.workflow.FlowChainBuilder
import org.zstack.header.core.workflow.FlowDoneHandler
import org.zstack.header.core.workflow.FlowErrorHandler
import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.vm.VmInstanceSpec
import org.zstack.header.vm.VmInstanceConstant
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import org.zstack.header.vm.VmInstanceState
import org.zstack.header.vm.VmInstanceVO
import org.zstack.header.volume.VolumeVO
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMConstant
import org.zstack.sdk.DestroyVmInstanceAction
import org.zstack.sdk.StopVmInstanceAction
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.kvm.Env
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.*

/** ZSV-14217: power-off does not imply persistent domain cleanup. */
class StoppedVmDomainCleanupCase extends SubCase {
    EnvSpec env
    String lateShutdownVm
    String responseFailureVm
    String transportFailureVm
    Set<String> persistentDomains = new HashSet<>()
    List<String> destroyRequests = []

    @Override
    void setup() { useSpring(KvmTest.springSpec) }

    @Override
    void environment() { env = Env.oneVmBasicEnv() }

    VmInstanceInventory createCandidate(String name, String strategy = "InstantStart") {
        return createVmInstance {
            delegate.name = name
            instanceOfferingUuid = (env.specByName("instanceOffering") as InstanceOfferingSpec).inventory.uuid
            imageUuid = (env.specByName("image1") as ImageSpec).inventory.uuid
            l3NetworkUuids = [(env.specByName("l3") as L3NetworkSpec).inventory.uuid]
            delegate.strategy = strategy
        } as VmInstanceInventory
    }

    @Override
    void test() {
        env.create {
            env.afterSimulator(KVMConstant.KVM_STOP_VM_PATH) { rsp, HttpEntity<String> e ->
                def cmd = json(e.body, KVMAgentCommands.StopVmCmd.class)
                if (cmd.uuid == lateShutdownVm) {
                    rsp.success = false
                    rsp.error = "injected graceful stop timeout"
                }
                return rsp
            }
            env.afterSimulator(KVMConstant.KVM_VM_CHECK_STATE) { rsp ->
                if (lateShutdownVm != null) {
                    rsp.states[lateShutdownVm] = "Shutdown"
                }
                return rsp
            }
            env.afterSimulator(KVMConstant.KVM_DESTROY_VM_PATH) { rsp, HttpEntity<String> e ->
                def cmd = json(e.body, KVMAgentCommands.DestroyVmCmd.class)
                destroyRequests.add(cmd.uuid)
                if (cmd.uuid == transportFailureVm) {
                    throw new HttpError(403, "injected destroy transport failure")
                }
                if (cmd.uuid == responseFailureVm) {
                    rsp.success = false
                    rsp.error = "injected persistent domain undefine failure"
                } else {
                    persistentDomains.remove(cmd.uuid)
                }
                return rsp
            }

            VmInstanceInventory vm = env.inventoryByName("vm")
            persistentDomains.add(vm.uuid)
            lateShutdownVm = vm.uuid
            assert new StopVmInstanceAction(uuid: vm.uuid, type: "grace", sessionId: adminSession()).call().error != null
            assert dbFindByUuid(vm.uuid, VmInstanceVO.class).state == VmInstanceState.Stopped

            // Failed undefine must not be hidden by a Stopped power-state check.
            responseFailureVm = vm.uuid
            assert new DestroyVmInstanceAction(uuid: vm.uuid, sessionId: adminSession()).call().error != null
            assert dbFindByUuid(vm.uuid, VmInstanceVO.class).state != VmInstanceState.Destroyed
            assert dbFindByUuid(vm.rootVolumeUuid, VolumeVO.class) != null
            assert persistentDomains.contains(vm.uuid)
            responseFailureVm = null
            lateShutdownVm = null
            destroyVmInstance { uuid = vm.uuid }
            assert !persistentDomains.contains(vm.uuid)
            assert destroyRequests.count(vm.uuid) == 2
            expungeVmInstance { uuid = vm.uuid }
            assert dbFindByUuid(vm.uuid, VmInstanceVO.class) == null
            assert dbFindByUuid(vm.rootVolumeUuid, VolumeVO.class) == null

            // Preserve tracked GC when a stopped VM's host cleanup is unreachable.
            VmInstanceInventory gcVm = createCandidate("stopped-with-gc")
            stopVmInstance { uuid = gcVm.uuid; type = "cold" }
            persistentDomains.add(gcVm.uuid)
            transportFailureVm = gcVm.uuid
            destroyVmInstance { uuid = gcVm.uuid }
            def jobs = queryGCJob { conditions = ["context~=%${gcVm.uuid}%"] }
            assert jobs.size() == 1
            assert jobs[0].status == GCStatus.Idle.toString()
            assert persistentDomains.contains(gcVm.uuid)
            expungeVmInstance { uuid = gcVm.uuid }
            assert dbFindByUuid(gcVm.rootVolumeUuid, VolumeVO.class) == null
            assert queryGCJob { conditions = ["uuid=${jobs[0].uuid}"] }[0].status == GCStatus.Idle.toString()
            transportFailureVm = null
            triggerGCJob { uuid = jobs[0].uuid }
            retryInSecs {
                assert queryGCJob { conditions = ["uuid=${jobs[0].uuid}"] }[0].status == GCStatus.Done.toString()
                assert !persistentDomains.contains(gcVm.uuid)
            }
            deleteGCJob { uuid = jobs[0].uuid }

            // A real disconnected host also leaves an explicit deletion GC.
            VmInstanceInventory offline = createCandidate("stopped-offline")
            stopVmInstance { uuid = offline.uuid; type = "cold" }
            persistentDomains.add(offline.uuid)
            SQL.New(HostVO.class).eq(HostVO_.uuid, offline.hostUuid)
                    .set(HostVO_.status, HostStatus.Disconnected).update()
            destroyVmInstance { uuid = offline.uuid }
            def offlineJobs = queryGCJob { conditions = ["context~=%${offline.uuid}%"] }
            assert offlineJobs.size() == 1
            SQL.New(HostVO.class).eq(HostVO_.uuid, offline.hostUuid)
                    .set(HostVO_.status, HostStatus.Connected).update()
            triggerGCJob { uuid = offlineJobs[0].uuid }
            retryInSecs {
                assert queryGCJob { conditions = ["uuid=${offlineJobs[0].uuid}"] }[0].status == GCStatus.Done.toString()
                assert !persistentDomains.contains(offline.uuid)
            }
            deleteGCJob { uuid = offlineJobs[0].uuid }
            expungeVmInstance { uuid = offline.uuid }

            // A never-started VM has no host domain to clean up.
            VmInstanceInventory created = createCandidate("never-started", "JustCreate")
            int requests = destroyRequests.size()
            def inventory = org.zstack.header.vm.VmInstanceInventory.valueOf(dbFindByUuid(created.uuid, VmInstanceVO.class))
            inventory.state = "Stopped"
            inventory.clusterUuid = "former-cluster"
            inventory.lastHostUuid = "removed-host"
            VmInstanceSpec spec = new VmInstanceSpec(vmInventory: inventory)
            AtomicBoolean completed = new AtomicBoolean()
            AtomicReference<ErrorCode> failure = new AtomicReference<>()
            def chain = FlowChainBuilder.newSimpleFlowChain()
            chain.data.put(VmInstanceConstant.Params.VmInstanceSpec.toString(), spec)
            chain.then(new VmDestroyOnHypervisorFlow())
            chain.done(new FlowDoneHandler(null) {
                @Override
                void handle(Map data) { completed.set(true) }
            }).error(new FlowErrorHandler(null) {
                @Override
                void handle(ErrorCode err, Map data) { failure.set(err); completed.set(true) }
            }).start()
            retryInSecs { assert completed.get() }
            assert failure.get() == null
            assert destroyRequests.size() == requests
            destroyVmInstance { uuid = created.uuid }
            assert destroyRequests.size() == requests
            assert dbFindByUuid(created.uuid, VmInstanceVO.class) == null
        }
    }

    @Override
    void clean() {
        responseFailureVm = null
        transportFailureVm = null
        lateShutdownVm = null
        env.delete()
    }
}
