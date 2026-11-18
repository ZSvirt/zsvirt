package org.zstack.test.integration.storage.snapshot

import org.springframework.http.HttpEntity
import org.zstack.core.Platform
import org.zstack.header.errorcode.OperationFailureException
import org.zstack.header.errorcode.SysErrors
import org.zstack.header.vm.AttachDataVolumeToVmMsg
import org.zstack.header.vm.StartVmInstanceMsg
import org.zstack.header.vm.StopVmInstanceMsg
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMConstant
import org.zstack.sdk.*
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.testlib.premium.PremiumEnv
import org.zstack.utils.data.SizeUnit
import org.zstack.utils.gson.JSONObjectUtil

/** The caller's strategy controls the final state after restoring and attaching snapshot-group disks. */
class VolumeSnapshotGroupCreateVmStrategyCase extends SubCase {
    EnvSpec env
    VmInstanceInventory sourceVm
    VmInstanceInventory rootOnlyVm
    String l3Uuid
    List<VolumeInventory> sourceDataVolumes
    List<String> sourceDataVolumeUuids
    List<String> startMessages = Collections.synchronizedList([])
    List<StopVmInstanceMsg> stopMessages = Collections.synchronizedList([])
    List<KVMAgentCommands.StartVmCmd> powerOnCommands = Collections.synchronizedList([])

    @Override
    void setup() {
        useSpring(PremiumEnv.makeSpring())
    }

    @Override
    void environment() {
        env = makeEnv {
            sftpBackupStorage {
                name = "sftp"
                url = "/sftp"
                username = "root"
                password = "password"
                hostname = "localhost"
                image {
                    name = "image"
                    url = "http://zstack.org/download/test.qcow2"
                }
            }
            zone {
                name = "zone"
                cluster {
                    name = "cluster"
                    hypervisorType = "KVM"
                    kvm {
                        name = "kvm"
                        managementIp = "127.0.0.1"
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
                        ip {
                            startIp = "192.168.100.10"
                            endIp = "192.168.100.100"
                            netmask = "255.255.255.0"
                            gateway = "192.168.100.1"
                        }
                    }
                }
                attachBackupStorage("sftp")
            }
            vm {
                name = "source-with-data"
                cpu = 1
                memorySize = SizeUnit.GIGABYTE.toByte(1)
                useImage("image")
                useL3Networks("l3")
                disk {
                    boot = true
                }
                disk {
                    size = SizeUnit.GIGABYTE.toByte(10)
                }
                disk {
                    size = SizeUnit.GIGABYTE.toByte(20)
                }
                useHost("kvm")
            }
            vm {
                name = "source-root-only"
                cpu = 1
                memorySize = SizeUnit.GIGABYTE.toByte(1)
                useImage("image")
                useL3Networks("l3")
                disk {
                    boot = true
                }
                useHost("kvm")
            }
        }
    }

    @Override
    void test() {
        env.create {
            sourceVm = env.inventoryByName("source-with-data") as VmInstanceInventory
            rootOnlyVm = env.inventoryByName("source-root-only") as VmInstanceInventory
            l3Uuid = (env.inventoryByName("l3") as L3NetworkInventory).uuid
            sourceDataVolumes = sourceVm.allVolumes.findAll { it.type == "Data" }.sort { it.deviceId }
            sourceDataVolumeUuids = sourceDataVolumes.collect { it.uuid }
            assert sourceDataVolumeUuids.size() == 2

            // Observe the real message and agent paths without replacing their execution.
            def removeStartObserver = notifyWhenReceivedMessage(StartVmInstanceMsg.class) { StartVmInstanceMsg msg ->
                startMessages.add(msg.vmInstanceUuid)
            }
            def removeStopObserver = notifyWhenReceivedMessage(StopVmInstanceMsg.class) { StopVmInstanceMsg msg ->
                stopMessages.add(msg)
            }
            env.afterSimulator(KVMConstant.KVM_START_VM_PATH) { rsp, HttpEntity<String> entity ->
                KVMAgentCommands.StartVmCmd cmd = JSONObjectUtil.toObject(entity.body, KVMAgentCommands.StartVmCmd.class)
                // CreateStopped defines a paused guest before shutdown; only unpaused commands power it on.
                if (!cmd.createPaused) {
                    powerOnCommands.add(cmd)
                }
                return rsp
            }

            testDataGroupCreateStoppedStopsAfterOriginalStart()
            testDataGroupInstantStartStartsAfterAllDisksAttach()
            testDataGroupDefaultStrategyStartsAfterAllDisksAttach()
            testRootOnlyGroupCreateStoppedDoesNotStart()
            testRootOnlyGroupInstantStartDoesNotStartAgain()
            testAttachmentFailureDoesNotStartAndRemovesRestoredDataVolumes()
            testFinalStopFailureDoesNotFailCreation()

            removeStartObserver()
            removeStopObserver()
            env.cleanSimulatorHandlers()
        }
    }

    void testDataGroupCreateStoppedStopsAfterOriginalStart() {
        // Preserve the original start after both disks attach, then stop to honor the requested final state.
        VolumeSnapshotGroupInventory group = createVolumeSnapshotGroup {
            name = "stopped-data-group"
            rootVolumeUuid = sourceVm.rootVolumeUuid
        } as VolumeSnapshotGroupInventory
        assert group.snapshotCount == 3

        VmInstanceInventory created = createVmInstanceFromVolumeSnapshotGroup {
            name = "stopped-clone"
            volumeSnapshotGroupUuid = group.uuid
            cpuNum = 1
            memorySize = SizeUnit.GIGABYTE.toByte(1)
            l3NetworkUuids = [l3Uuid]
            strategy = "CreateStopped"
        } as VmInstanceInventory

        assert created.state == "Stopped"
        assertRestoredDisks(created, group)
        VmInstanceInventory queried = (queryVmInstance {
            conditions = ["uuid=${created.uuid}".toString()]
        } as List<VmInstanceInventory>)[0]
        assert queried.state == "Stopped"
        assertRestoredDisks(queried, group)
        assert startMessages.count(created.uuid) == 1
        List<KVMAgentCommands.StartVmCmd> commands = powerOnCommands.findAll { it.vmInstanceUuid == created.uuid }
        assert commands.size() == 1
        assert commands[0].dataVolumes.collect { it.volumeUuid }.toSet() == created.allVolumes.findAll { it.type == "Data" }.collect { it.uuid }.toSet()
        List<StopVmInstanceMsg> stops = stopMessages.findAll { it.vmInstanceUuid == created.uuid }
        assert stops.size() == 1
        assert stops[0].stopHA

        deleteCloneAndGroup(created, group)
    }

    void testDataGroupInstantStartStartsAfterAllDisksAttach() {
        // Explicit InstantStart must keep the delayed start, with both restored disks in its agent command.
        VolumeSnapshotGroupInventory group = createVolumeSnapshotGroup {
            name = "instant-start-data-group"
            rootVolumeUuid = sourceVm.rootVolumeUuid
        } as VolumeSnapshotGroupInventory

        VmInstanceInventory created = createVmInstanceFromVolumeSnapshotGroup {
            name = "instant-start-clone"
            volumeSnapshotGroupUuid = group.uuid
            cpuNum = 1
            memorySize = SizeUnit.GIGABYTE.toByte(1)
            l3NetworkUuids = [l3Uuid]
            strategy = "InstantStart"
        } as VmInstanceInventory

        assert created.state == "Running"
        assert stopMessages.findAll { it.vmInstanceUuid == created.uuid }.isEmpty()
        List<VolumeInventory> dataVolumes = assertRestoredDisks(created, group)
        assert startMessages.count(created.uuid) == 1
        List<KVMAgentCommands.StartVmCmd> commands = powerOnCommands.findAll { it.vmInstanceUuid == created.uuid }
        assert commands.size() == 1
        assert commands[0].dataVolumes.collect { it.volumeUuid } == dataVolumes.collect { it.uuid }

        deleteCloneAndGroup(created, group)
    }

    void testDataGroupDefaultStrategyStartsAfterAllDisksAttach() {
        // Omitting strategy must retain the API's default InstantStart behavior after attaching both disks.
        VolumeSnapshotGroupInventory group = createVolumeSnapshotGroup {
            name = "default-strategy-data-group"
            rootVolumeUuid = sourceVm.rootVolumeUuid
        } as VolumeSnapshotGroupInventory

        VmInstanceInventory created = createVmInstanceFromVolumeSnapshotGroup {
            name = "default-strategy-clone"
            volumeSnapshotGroupUuid = group.uuid
            cpuNum = 1
            memorySize = SizeUnit.GIGABYTE.toByte(1)
            l3NetworkUuids = [l3Uuid]
        } as VmInstanceInventory

        assert created.state == "Running"
        assert stopMessages.findAll { it.vmInstanceUuid == created.uuid }.isEmpty()
        List<VolumeInventory> dataVolumes = assertRestoredDisks(created, group)
        assert startMessages.count(created.uuid) == 1
        List<KVMAgentCommands.StartVmCmd> commands = powerOnCommands.findAll { it.vmInstanceUuid == created.uuid }
        assert commands.size() == 1
        assert commands[0].dataVolumes.collect { it.volumeUuid } == dataVolumes.collect { it.uuid }

        deleteCloneAndGroup(created, group)
    }

    void testRootOnlyGroupCreateStoppedDoesNotStart() {
        // The existing root-only path must still honor CreateStopped without entering delayed start.
        VolumeSnapshotGroupInventory group = createVolumeSnapshotGroup {
            name = "root-only-group"
            rootVolumeUuid = rootOnlyVm.rootVolumeUuid
        } as VolumeSnapshotGroupInventory
        assert group.snapshotCount == 1

        VmInstanceInventory created = createVmInstanceFromVolumeSnapshotGroup {
            name = "root-only-stopped-clone"
            volumeSnapshotGroupUuid = group.uuid
            cpuNum = 1
            memorySize = SizeUnit.GIGABYTE.toByte(1)
            l3NetworkUuids = [l3Uuid]
            strategy = "CreateStopped"
        } as VmInstanceInventory

        assert created.state == "Stopped"
        assert stopMessages.findAll { it.vmInstanceUuid == created.uuid }.isEmpty()
        assert created.allVolumes.size() == 1
        assert created.allVolumes[0].type == "Root"
        assert !startMessages.contains(created.uuid)
        assert powerOnCommands.findAll { it.vmInstanceUuid == created.uuid }.isEmpty()

        deleteCloneAndGroup(created, group)
    }

    void testRootOnlyGroupInstantStartDoesNotStartAgain() {
        // Root-only InstantStart already starts during creation; finalization must not request another start.
        VolumeSnapshotGroupInventory group = createVolumeSnapshotGroup {
            name = "root-only-instant-start-group"
            rootVolumeUuid = rootOnlyVm.rootVolumeUuid
        } as VolumeSnapshotGroupInventory
        assert group.snapshotCount == 1

        VmInstanceInventory created = createVmInstanceFromVolumeSnapshotGroup {
            name = "root-only-instant-start-clone"
            volumeSnapshotGroupUuid = group.uuid
            cpuNum = 1
            memorySize = SizeUnit.GIGABYTE.toByte(1)
            l3NetworkUuids = [l3Uuid]
            strategy = "InstantStart"
        } as VmInstanceInventory

        assert created.state == "Running"
        assert stopMessages.findAll { it.vmInstanceUuid == created.uuid }.isEmpty()
        assert created.allVolumes.size() == 1
        assert created.allVolumes[0].type == "Root"
        assert !startMessages.contains(created.uuid)
        List<KVMAgentCommands.StartVmCmd> commands = powerOnCommands.findAll { it.vmInstanceUuid == created.uuid }
        assert commands.size() == 1
        assert commands[0].dataVolumes.isEmpty()

        deleteCloneAndGroup(created, group)
    }

    void testAttachmentFailureDoesNotStartAndRemovesRestoredDataVolumes() {
        // Fail the second data-disk attachment: InstantStart must fail before start and roll back restored disks.
        VolumeSnapshotGroupInventory group = createVolumeSnapshotGroup {
            name = "attachment-failure-group"
            rootVolumeUuid = sourceVm.rootVolumeUuid
        } as VolumeSnapshotGroupInventory
        String failedVmUuid = Platform.getUuid()
        String secondDataSnapshotUuid = group.volumeSnapshotRefs.findAll { it.volumeType == "Data" }
                .sort { it.deviceId }[1].volumeSnapshotUuid
        int failures = 0
        def removeFailureObserver = notifyWhenReceivedMessage(AttachDataVolumeToVmMsg.class) { AttachDataVolumeToVmMsg msg ->
            if (msg.vmInstanceUuid == failedVmUuid && msg.volume.name == "data-volume-from-snapshot-${secondDataSnapshotUuid}") {
                failures++
                throw new OperationFailureException(Platform.operr("injected snapshot-group second-disk attachment failure"))
            }
        }

        expectApiFailure({
            createVmInstanceFromVolumeSnapshotGroup {
                name = "attachment-failure-clone"
                resourceUuid = failedVmUuid
                volumeSnapshotGroupUuid = group.uuid
                cpuNum = 1
                memorySize = SizeUnit.GIGABYTE.toByte(1)
                l3NetworkUuids = [l3Uuid]
                strategy = "InstantStart"
            }
        }) {
            assert delegate.code == SysErrors.OPERATION_ERROR.toString()
            assert delegate.details.contains("injected snapshot-group second-disk attachment failure")
        }

        assert failures == 1
        assert !startMessages.contains(failedVmUuid)
        assert stopMessages.findAll { it.vmInstanceUuid == failedVmUuid }.isEmpty()
        assert powerOnCommands.findAll { it.vmInstanceUuid == failedVmUuid }.isEmpty()
        VmInstanceInventory failedVm = (queryVmInstance {
            conditions = ["uuid=${failedVmUuid}".toString()]
        } as List<VmInstanceInventory>)[0]
        assert failedVm.state == "Stopped"
        assert failedVm.allVolumes.findAll { it.type == "Data" }.isEmpty()
        group.volumeSnapshotRefs.findAll { it.volumeType == "Data" }.each { snapshot ->
            assert (queryVolume {
                conditions = ["name=data-volume-from-snapshot-${snapshot.volumeSnapshotUuid}".toString()]
            } as List<VolumeInventory>).isEmpty()
        }
        VmInstanceInventory source = (queryVmInstance {
            conditions = ["uuid=${sourceVm.uuid}".toString()]
        } as List<VmInstanceInventory>)[0]
        assert source.state == "Running"
        assert source.allVolumes.findAll { it.type == "Data" }.collect { it.uuid }.toSet() == sourceDataVolumeUuids.toSet()

        removeFailureObserver()
        deleteCloneAndGroup(failedVm, group)
    }

    void testFinalStopFailureDoesNotFailCreation() {
        // The host rejects the final stop and reports Running. Creation must still succeed with all disks intact.
        VolumeSnapshotGroupInventory group = createVolumeSnapshotGroup {
            name = "final-stop-failure-group"
            rootVolumeUuid = sourceVm.rootVolumeUuid
        } as VolumeSnapshotGroupInventory
        String failedVmUuid = Platform.getUuid()
        boolean failFinalStop = true
        int failures = 0
        env.afterSimulator(KVMConstant.KVM_STOP_VM_PATH) { KVMAgentCommands.StopVmResponse rsp, HttpEntity<String> entity ->
            KVMAgentCommands.StopVmCmd cmd = JSONObjectUtil.toObject(entity.body, KVMAgentCommands.StopVmCmd.class)
            if (failFinalStop && cmd.uuid == failedVmUuid && startMessages.contains(failedVmUuid)) {
                failures++
                rsp.success = false
                rsp.error = "injected final snapshot-group stop failure"
            }
            return rsp
        }
        env.afterSimulator(KVMConstant.KVM_VM_CHECK_STATE) { KVMAgentCommands.CheckVmStateRsp rsp, HttpEntity<String> entity ->
            KVMAgentCommands.CheckVmStateCmd cmd = JSONObjectUtil.toObject(entity.body, KVMAgentCommands.CheckVmStateCmd.class)
            if (failFinalStop && startMessages.contains(failedVmUuid) && cmd.vmUuids.contains(failedVmUuid)) {
                rsp.states[failedVmUuid] = KVMConstant.KvmVmState.Running.toString()
            }
            return rsp
        }

        VmInstanceInventory created = createVmInstanceFromVolumeSnapshotGroup {
            name = "final-stop-failure-clone"
            resourceUuid = failedVmUuid
            volumeSnapshotGroupUuid = group.uuid
            cpuNum = 1
            memorySize = SizeUnit.GIGABYTE.toByte(1)
            l3NetworkUuids = [l3Uuid]
            strategy = "CreateStopped"
        } as VmInstanceInventory

        assert created.uuid == failedVmUuid
        assert created.state == "Running"
        assertRestoredDisks(created, group)

        assert failures == 1
        assert startMessages.count(failedVmUuid) == 1
        assert stopMessages.findAll { it.vmInstanceUuid == failedVmUuid }.size() == 1
        VmInstanceInventory retained = (queryVmInstance {
            conditions = ["uuid=${failedVmUuid}".toString()]
        } as List<VmInstanceInventory>)[0]
        assert retained.state == "Running"
        assertRestoredDisks(retained, group)

        failFinalStop = false
        deleteCloneAndGroup(retained, group)
    }

    private List<VolumeInventory> assertRestoredDisks(VmInstanceInventory vm, VolumeSnapshotGroupInventory group) {
        List<VolumeInventory> dataVolumes = (queryVolume {
            conditions = ["vmInstanceUuid=${vm.uuid}".toString(), "type=Data"]
        } as List<VolumeInventory>).sort { it.deviceId }
        List<VolumeSnapshotGroupRefInventory> dataSnapshots = group.volumeSnapshotRefs.findAll { it.volumeType == "Data" }
                .sort { it.deviceId }
        assert dataVolumes.size() == 2
        // Batch snapshot VFS uses zero-sized overlays; prove snapshot identity and attachment order.
        assert dataVolumes.collect { it.name } == dataSnapshots.collect { "data-volume-from-snapshot-${it.volumeSnapshotUuid}".toString() }
        assert dataVolumes.collect { it.deviceId } == dataSnapshots.collect { it.deviceId }
        assert dataVolumes.every { it.status == "Ready" && !sourceDataVolumeUuids.contains(it.uuid) }
        assert vm.allVolumes.size() == 3
        assert vm.allVolumes.findAll { it.type == "Data" }.collect { it.uuid }.toSet() == dataVolumes.collect { it.uuid }.toSet()
        assert vm.rootVolumeUuid != sourceVm.rootVolumeUuid
        return dataVolumes
    }

    private void deleteCloneAndGroup(VmInstanceInventory vm, VolumeSnapshotGroupInventory group) {
        List<String> dataVolumeUuids = (queryVolume {
            conditions = ["vmInstanceUuid=${vm.uuid}".toString(), "type=Data"]
        } as List<VolumeInventory>).collect { it.uuid }
        destroyVmInstance { uuid = vm.uuid }
        expungeVmInstance { uuid = vm.uuid }
        dataVolumeUuids.each { String volumeUuid ->
            deleteDataVolume { uuid = volumeUuid }
            expungeDataVolume { uuid = volumeUuid }
        }
        deleteVolumeSnapshotGroup { uuid = group.uuid }
    }

    @Override
    void clean() {
        // A failed scenario can leave a group whose deletion still needs its source VM.
        // Tear down these fixture dependencies before EnvSpec deletes the source VMs.
        List<String> fixtureVmUuids = [sourceVm?.uuid, rootOnlyVm?.uuid].findAll { it != null }
        fixtureVmUuids.each { String fixtureVmUuid ->
            List<VolumeSnapshotGroupInventory> remainingGroups = queryVolumeSnapshotGroup {
                conditions = ["vmInstanceUuid=${fixtureVmUuid}".toString()]
            } as List<VolumeSnapshotGroupInventory>
            remainingGroups.each { VolumeSnapshotGroupInventory group ->
                deleteVolumeSnapshotGroup { uuid = group.uuid }
            }
        }
        env.delete()
    }
}
