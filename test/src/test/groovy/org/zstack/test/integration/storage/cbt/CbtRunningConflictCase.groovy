package org.zstack.test.integration.storage.cbt

import org.springframework.http.HttpEntity
import org.zstack.core.Platform
import org.zstack.core.cloudbus.CloudBus
import org.zstack.core.db.DatabaseFacade
import org.zstack.core.db.Q
import org.zstack.core.db.SQL
import org.zstack.core.thread.ThreadFacade
import org.zstack.guesttools.GuestToolsConstant
import org.zstack.guesttools.kvm.GuestToolsKvmCommands
import org.zstack.header.agent.AgentResponse
import org.zstack.header.cbt.CbtTaskResourceRefVO
import org.zstack.header.cbt.CbtTaskResourceRefVO_
import org.zstack.header.cbt.CbtTaskStatus
import org.zstack.header.cbt.CbtTaskVO
import org.zstack.header.cbt.CbtTaskVO_
import org.zstack.header.cbt.DisableCbtTaskMsg
import org.zstack.header.cbt.VolumeCbtBackupRecordVO
import org.zstack.header.cbt.VolumeCbtBackupRecordVO_
import org.zstack.header.image.ImageVO
import org.zstack.header.scheduler.SchedulerJobVO
import org.zstack.header.scheduler.SchedulerJobHistoryVO
import org.zstack.header.scheduler.SchedulerJobHistoryVO_
import org.zstack.header.storage.backup.BackupStorageState
import org.zstack.header.storage.backup.BackupStorageStatus
import org.zstack.header.storage.backup.BackupStorageVO
import org.zstack.header.storage.backup.BackupStorageZoneRefVO
import org.zstack.header.storage.backup.BackupStorageZoneRefVO_
import org.zstack.header.storage.primary.AskVolumeSnapshotCapabilityMsg
import org.zstack.header.storage.primary.AskVolumeSnapshotCapabilityReply
import org.zstack.header.storage.primary.CreateTemplateFromVolumeOnPrimaryStorageMsg
import org.zstack.header.storage.primary.VolumeSnapshotCapability
import org.zstack.header.storage.snapshot.CreateVolumeSnapshotMsg
import org.zstack.header.storage.snapshot.CreateVolumesSnapshotMsg
import org.zstack.header.storage.snapshot.VolumeSnapshotVO
import org.zstack.header.storage.snapshot.VolumeSnapshotVO_
import org.zstack.header.storage.snapshot.group.VolumeSnapshotGroupVO
import org.zstack.header.storage.snapshot.group.VolumeSnapshotGroupVO_
import org.zstack.header.vm.MigrateVmMsg
import org.zstack.header.vm.VmInstanceVO
import org.zstack.header.volume.FlattenVolumeMsg
import org.zstack.header.volume.VolumeVO
import org.zstack.scheduler.CreateSchedulerJobMsg
import org.zstack.sdk.*
import org.zstack.sdk.storage.volumebackup.CreateVmBackupAction
import org.zstack.sdk.storage.volumebackup.CreateVolumeBackupAction
import org.zstack.storage.backup.imagestore.ImageStoreBackupStorageConstant
import org.zstack.storage.backup.imagestore.ImageStoreBackupStorageVO
import org.zstack.storage.cbt.CbtBackupConstant
import org.zstack.storage.cbt.CbtBackupKvmCommands
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.testlib.premium.PremiumEnv
import org.zstack.utils.data.SizeUnit
import org.zstack.utils.gson.JSONObjectUtil

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.atomic.AtomicBoolean

class CbtRunningConflictCase extends SubCase {
    EnvSpec env
    VmInstanceInventory vm
    VmInstanceInventory otherVm
    VolumeInventory dataVolume
    HostInventory secondHost
    PrimaryStorageInventory destination
    String backupStorageUuid
    String taskUuid
    List<Closure> removeObservers = []
    AtomicInteger disableRequests = new AtomicInteger()
    AtomicInteger diskOperationRequests = new AtomicInteger()
    AtomicReference<VolumeSnapshotCapability.VolumeSnapshotArrangementType> arrangement =
            new AtomicReference<>(VolumeSnapshotCapability.VolumeSnapshotArrangementType.CHAIN)

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
                hostname = "localhost"
                username = "root"
                password = "password"
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
                        name = "host1"
                        managementIp = "127.0.0.1"
                        username = "root"
                        password = "password"
                    }
                    kvm {
                        name = "host2"
                        managementIp = "127.0.0.2"
                        username = "root"
                        password = "password"
                    }
                    attachPrimaryStorage("nfs", "destination")
                    attachL2Network("l2")
                }
                nfsPrimaryStorage {
                    name = "nfs"
                    url = "localhost:/nfs"
                }
                nfsPrimaryStorage {
                    name = "destination"
                    url = "localhost:/destination"
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
                name = "protected-vm"
                cpu = 1
                memorySize = SizeUnit.GIGABYTE.toByte(1)
                useHost("host1")
                useImage("image")
                useL3Networks("l3")
                disk {
                    boot = true
                }
                disk {
                    size = SizeUnit.GIGABYTE.toByte(1)
                }
                usePrimaryStorage("nfs")
            }
            vm {
                name = "other-vm"
                cpu = 1
                memorySize = SizeUnit.GIGABYTE.toByte(1)
                useHost("host1")
                useImage("image")
                useL3Networks("l3")
                usePrimaryStorage("nfs")
            }
        }
    }

    @Override
    void test() {
        env.create {
            vm = env.inventoryByName("protected-vm") as VmInstanceInventory
            otherVm = env.inventoryByName("other-vm") as VmInstanceInventory
            secondHost = env.inventoryByName("host2") as HostInventory
            destination = env.inventoryByName("destination") as PrimaryStorageInventory
            dataVolume = (queryVolume {
                conditions = ["vmInstanceUuid=" + vm.uuid, "type=Data"]
            } as List<VolumeInventory>).first()
            // States and snapshot capabilities are fixtures; API/DB/plugin routing stays real.
            env.message(AskVolumeSnapshotCapabilityMsg.class) { AskVolumeSnapshotCapabilityMsg msg, CloudBus bus ->
                VolumeSnapshotCapability capability = new VolumeSnapshotCapability()
                capability.support = true
                capability.arrangementType = arrangement.get()
                capability.supportCreateOnHypervisor = true
                AskVolumeSnapshotCapabilityReply reply = new AskVolumeSnapshotCapabilityReply()
                reply.capability = capability
                bus.reply(msg, reply)
            }
            removeObservers.add(notifyWhenReceivedMessage(DisableCbtTaskMsg.class) {
                disableRequests.incrementAndGet()
            })
            [CreateVolumeSnapshotMsg, CreateVolumesSnapshotMsg, CreateTemplateFromVolumeOnPrimaryStorageMsg,
             FlattenVolumeMsg, MigrateVmMsg, CreateSchedulerJobMsg].each { Class messageClass ->
                removeObservers.add(notifyWhenReceivedMessage(messageClass) {
                    diskOperationRequests.incrementAndGet()
                })
            }

            testRunningRejectsConflictsWithoutSideEffects()
            testRunningTaskDoesNotProtectAnotherVm()
            [CbtTaskStatus.Created, CbtTaskStatus.Stopped, CbtTaskStatus.Failed].each { CbtTaskStatus status ->
                testNonRunningTaskAllowsSnapshotMigrationAndSchedulers(status)
            }
            testExistingTaskStillPreventsDuplicateCreation()
            testScheduledSnapshotsRejectRunningCbt("volumeSnapshot", vm.rootVolumeUuid)
            testScheduledSnapshotsRejectRunningCbt("volumeSnapshot", dataVolume.uuid)
            testScheduledSnapshotsRejectRunningCbt("volumeSnapshotGroup", vm.rootVolumeUuid)
            testFailedRollbackKeepsConflictsUntilCleanup()
            removeObservers.each { it() }
        }
    }

    private void createTaskFixture(CbtTaskStatus status) {
        taskUuid = (createCbtTask {
            name = "api-conflict-task"
            vmInstanceUuid = vm.uuid
        } as CbtTaskInventory).uuid
        setTaskStatus(status)
    }

    private void prepareBackupStorageFixture() {
        DatabaseFacade dbf = bean(DatabaseFacade.class)
        // Only metadata is needed: Running CBT must reject before contacting image-store.
        backupStorageUuid = Platform.getUuid()
        ImageStoreBackupStorageVO storage = new ImageStoreBackupStorageVO()
        storage.uuid = backupStorageUuid
        storage.name = "cbt-admission-image-store"
        storage.type = ImageStoreBackupStorageConstant.IMAGE_STORE_BACKUP_STORAGE_TYPE
        storage.url = "/image-store"
        storage.hostname = "localhost"
        storage.username = "root"
        storage.password = "password"
        storage.sshPort = 22
        storage.state = BackupStorageState.Enabled
        storage.status = BackupStorageStatus.Connected
        storage.totalCapacity = SizeUnit.GIGABYTE.toByte(100)
        storage.availableCapacity = storage.totalCapacity
        dbf.persistAndRefresh(storage)
        BackupStorageZoneRefVO ref = new BackupStorageZoneRefVO()
        ref.backupStorageUuid = backupStorageUuid
        ref.zoneUuid = (env.inventoryByName("zone") as ZoneInventory).uuid
        dbf.persistAndRefresh(ref)
    }

    private void setTaskStatus(CbtTaskStatus status) {
        SQL.New(CbtTaskVO.class).eq(CbtTaskVO_.uuid, taskUuid).set(CbtTaskVO_.status, status).update()
        assert dbFindByUuid(taskUuid, CbtTaskVO.class).status == status
        assert Q.New(CbtTaskResourceRefVO.class).eq(CbtTaskResourceRefVO_.taskUuid, taskUuid)
                .eq(CbtTaskResourceRefVO_.resourceUuid, vm.uuid).isExists()
    }

    private Map protectedState() {
        def currentVm = dbFindByUuid(vm.uuid, VmInstanceVO.class)
        return [
                vmState: currentVm.state, hostUuid: currentVm.hostUuid,
                taskStatus: dbFindByUuid(taskUuid, CbtTaskVO.class).status,
                refs: Q.New(CbtTaskResourceRefVO.class).eq(CbtTaskResourceRefVO_.taskUuid, taskUuid)
                        .list().collect { it.resourceType + ":" + it.resourceUuid }.sort(),
                volumes: Q.New(VolumeVO.class).count(), images: Q.New(ImageVO.class).count(),
                snapshots: Q.New(VolumeSnapshotVO.class).count(), jobs: Q.New(SchedulerJobVO.class).count()
        ]
    }

    private List<Map> schedulerTargets() {
        return [
                [type: "vmBackup", target: vm.uuid],
                [type: "rootVolumeBackup", target: vm.rootVolumeUuid],
                [type: "volumeBackup", target: dataVolume.uuid],
                [type: "volumeSnapshot", target: vm.rootVolumeUuid],
                [type: "volumeSnapshotGroup", target: vm.rootVolumeUuid]
        ]
    }

    private Map<String, Closure> conflictingOperations() {
        String session = adminSession()
        String migrationHostUuid = dbFindByUuid(vm.uuid, VmInstanceVO.class).hostUuid == secondHost.uuid ?
                (env.inventoryByName("host1") as HostInventory).uuid : secondHost.uuid
        Map<String, Closure> operations = [
                rootSnapshot: { new CreateVolumeSnapshotAction(volumeUuid: vm.rootVolumeUuid,
                        name: "blocked-root", sessionId: session).call() },
                dataSnapshot: { new CreateVolumeSnapshotAction(volumeUuid: dataVolume.uuid,
                        name: "blocked-data", sessionId: session).call() },
                snapshotGroup: { new CreateVolumeSnapshotGroupAction(rootVolumeUuid: vm.rootVolumeUuid,
                        name: "blocked-group", sessionId: session).call() },
                rootImage: { new CreateRootVolumeTemplateFromRootVolumeAction(rootVolumeUuid: vm.rootVolumeUuid,
                        backupStorageUuids: [backupStorageUuid], name: "blocked-root-image", sessionId: session).call() },
                dataImage: { new CreateDataVolumeTemplateFromVolumeAction(volumeUuid: dataVolume.uuid,
                        backupStorageUuids: [backupStorageUuid], name: "blocked-data-image", sessionId: session).call() },
                rootFlatten: { new FlattenVolumeAction(uuid: vm.rootVolumeUuid, sessionId: session).call() },
                vmFlatten: { new FlattenVmInstanceAction(uuid: vm.uuid, sessionId: session).call() },
                migrateHost: { new MigrateVmAction(vmInstanceUuid: vm.uuid,
                        hostUuid: migrationHostUuid, sessionId: session).call() },
                migrateStorage: { new PrimaryStorageMigrateVmAction(vmInstanceUuid: vm.uuid,
                        dstPrimaryStorageUuid: destination.uuid, sessionId: session).call() },
                migrateDataVolume: { new PrimaryStorageMigrateVolumeAction(volumeUuid: dataVolume.uuid,
                        dstPrimaryStorageUuid: destination.uuid, sessionId: session).call() },
                vmBackup: { new CreateVmBackupAction(rootVolumeUuid: vm.rootVolumeUuid,
                        backupStorageUuid: backupStorageUuid, name: "blocked-vm-backup", sessionId: session).call() },
                volumeBackup: { new CreateVolumeBackupAction(volumeUuid: dataVolume.uuid,
                        backupStorageUuid: backupStorageUuid, name: "blocked-volume-backup", sessionId: session).call() }
        ]
        schedulerTargets().each { Map scheduler ->
            operations["scheduler-" + scheduler.type] = {
                new CreateSchedulerJobAction(type: scheduler.type, targetResourceUuid: scheduler.target,
                        name: "blocked-" + scheduler.type, parameters: [backupStorageUuid: backupStorageUuid],
                        sessionId: session).call()
            }
        }
        return operations
    }

    private void testRunningRejectsConflictsWithoutSideEffects() {
        prepareBackupStorageFixture()
        createTaskFixture(CbtTaskStatus.Running)
        Map before = protectedState()
        [VolumeSnapshotCapability.VolumeSnapshotArrangementType.CHAIN,
         VolumeSnapshotCapability.VolumeSnapshotArrangementType.INDIVIDUAL].each { mode ->
            arrangement.set(mode)
            conflictingOperations().each { String operation, Closure request ->
                def result = request()
                assert result.error != null: operation + " must reject Running CBT (" + mode + ")"
                String error = JSONObjectUtil.toJsonString(result.error)
                assert error.contains("running CBT task") && error.contains(vm.uuid) && error.contains(taskUuid): error
                assert protectedState() == before: operation + " changed protected resources"
                assert disableRequests.get() == 0
                assert diskOperationRequests.get() == 0
            }
        }
        arrangement.set(VolumeSnapshotCapability.VolumeSnapshotArrangementType.CHAIN)
        deleteTaskFixture()
        deleteBackupStorageFixture()
    }

    private void testRunningTaskDoesNotProtectAnotherVm() {
        createTaskFixture(CbtTaskStatus.Running)
        def snapshot = createVolumeSnapshot {
            volumeUuid = otherVm.rootVolumeUuid
            name = "unprotected-vm-snapshot"
        } as VolumeSnapshotInventory
        assert snapshot.status == "Ready"
        assert dbFindByUuid(taskUuid, CbtTaskVO.class).status == CbtTaskStatus.Running
        deleteVolumeSnapshot { uuid = snapshot.uuid }
        deleteTaskFixture()
    }

    private void testNonRunningTaskAllowsSnapshotMigrationAndSchedulers(CbtTaskStatus status) {
        prepareBackupStorageFixture()
        createTaskFixture(status)
        List<VolumeSnapshotInventory> snapshots = [vm.rootVolumeUuid, dataVolume.uuid].collect { String volume ->
            def snapshot = createVolumeSnapshot {
                volumeUuid = volume
                name = "allowed-" + status + "-" + volume
            } as VolumeSnapshotInventory
            assert snapshot.status == "Ready"
            return snapshot
        }
        def currentVm = dbFindByUuid(vm.uuid, VmInstanceVO.class)
        String target = currentVm.hostUuid == secondHost.uuid ?
                (env.inventoryByName("host1") as HostInventory).uuid : secondHost.uuid
        // The stability suite also enables the normal GuestTools migration extension.
        env.simulator(GuestToolsConstant.DETACH_GUEST_TOOLS_ISO_FROM_VM_PATH) {
            new GuestToolsKvmCommands.DetachGuestToolsIsoFromVmRsp()
        }
        migrateVm { vmInstanceUuid = vm.uuid; hostUuid = target }
        assert dbFindByUuid(vm.uuid, VmInstanceVO.class).hostUuid == target
        assert dbFindByUuid(taskUuid, CbtTaskVO.class).status == status
        assert disableRequests.get() == 0

        List<SchedulerJobInventory> jobs = schedulerTargets().collect { Map scheduler ->
            def job = createSchedulerJob {
                type = scheduler.type
                targetResourceUuid = scheduler.target
                name = "allowed-" + status + "-" + scheduler.type
                parameters = [backupStorageUuid: backupStorageUuid]
            } as SchedulerJobInventory
            assert job.uuid != null
            return job
        }
        jobs.each { SchedulerJobInventory job -> deleteSchedulerJob { uuid = job.uuid } }
        snapshots.each { VolumeSnapshotInventory snapshot -> deleteVolumeSnapshot { uuid = snapshot.uuid } }
        deleteTaskFixture()
        deleteBackupStorageFixture()
        env.cleanSimulatorHandlers()
    }

    private void testExistingTaskStillPreventsDuplicateCreation() {
        createTaskFixture(CbtTaskStatus.Created)
        long before = Q.New(CbtTaskVO.class).count()
        def result = new CreateCbtTaskAction(vmInstanceUuid: vm.uuid, name: "duplicate",
                sessionId: adminSession()).call()
        assert result.error != null
        assert JSONObjectUtil.toJsonString(result.error).contains("already has a CBT task")
        assert Q.New(CbtTaskVO.class).count() == before
        deleteTaskFixture()
    }

    private void testScheduledSnapshotsRejectRunningCbt(String jobType, String targetUuid) {
        createTaskFixture(CbtTaskStatus.Created)
        def job = createSchedulerJob {
            name = "existing-" + jobType
            type = jobType
            targetResourceUuid = targetUuid
            parameters = [:]
        } as SchedulerJobInventory
        def trigger = createSchedulerTrigger {
            name = "cbt-snapshot-trigger"
            schedulerType = "simple"
            schedulerInterval = 3600
            repeatCount = 1
            startTime = System.currentTimeMillis().intdiv(1000) + 3600
        } as SchedulerTriggerInventory
        addSchedulerJobToSchedulerTrigger {
            schedulerJobUuid = job.uuid
            schedulerTriggerUuid = trigger.uuid
        }
        List<String> beforeSnapshots = Q.New(VolumeSnapshotVO.class)
                .select(VolumeSnapshotVO_.uuid).listValues()
        List<String> beforeGroups = Q.New(VolumeSnapshotGroupVO.class)
                .select(VolumeSnapshotGroupVO_.uuid).listValues()
        setTaskStatus(CbtTaskStatus.Running)
        Map before = protectedState()
        int disabledBefore = disableRequests.get()
        [VolumeSnapshotCapability.VolumeSnapshotArrangementType.CHAIN,
         VolumeSnapshotCapability.VolumeSnapshotArrangementType.INDIVIDUAL].eachWithIndex { mode, index ->
            arrangement.set(mode)
            runSchedulerTrigger { uuid = trigger.uuid; jobUuids = [job.uuid] }
            List<SchedulerJobHistoryVO> histories = []
            retryInSecs {
                histories = Q.New(SchedulerJobHistoryVO.class)
                        .eq(SchedulerJobHistoryVO_.schedulerJobUuid, job.uuid).list()
                assert histories.size() == index + 1
                assert histories.every { !it.running }
            }
            def result = histories.max { it.id }
            waitForSnapshotJob(jobType, targetUuid)
            assert !result.success
            assert result.resultDump.contains("running CBT task") && result.resultDump.contains(taskUuid)
            assert protectedState() == before
            assert disableRequests.get() == disabledBefore
        }
        arrangement.set(VolumeSnapshotCapability.VolumeSnapshotArrangementType.CHAIN)
        setTaskStatus(CbtTaskStatus.Stopped)
        runSchedulerTrigger { uuid = trigger.uuid; jobUuids = [job.uuid] }
        retryInSecs {
            def histories = Q.New(SchedulerJobHistoryVO.class)
                    .eq(SchedulerJobHistoryVO_.schedulerJobUuid, job.uuid).list()
            assert histories.size() == 3
            assert histories.every { !it.running }
            assert histories.max { it.id }.success
        }
        waitForSnapshotJob(jobType, targetUuid)
        assert Q.New(VolumeSnapshotVO.class).count() > beforeSnapshots.size()
        assert dbFindByUuid(taskUuid, CbtTaskVO.class).status == CbtTaskStatus.Stopped
        deleteSchedulerTrigger { uuid = trigger.uuid }
        deleteSchedulerJob { uuid = job.uuid }
        Q.New(VolumeSnapshotGroupVO.class).list().findAll { !beforeGroups.contains(it.uuid) }.each { group ->
            deleteVolumeSnapshotGroup { uuid = group.uuid; scope = "single" }
        }
        Q.New(VolumeSnapshotVO.class).list().findAll { !beforeSnapshots.contains(it.uuid) }.each { snapshot ->
            if (dbFindByUuid(snapshot.uuid, VolumeSnapshotVO.class) != null) {
                deleteVolumeSnapshot { uuid = snapshot.uuid }
            }
        }
        SQL.New(SchedulerJobHistoryVO.class).eq(SchedulerJobHistoryVO_.schedulerJobUuid, job.uuid).delete()
        deleteTaskFixture()
    }

    private void waitForSnapshotJob(String jobType, String targetUuid) {
        String queue = jobType == "volumeSnapshotGroup" ?
                "create-vm-" + targetUuid + "-snapshot-group" : "create-snapshot-" + targetUuid
        retryInSecs {
            assert !bean(ThreadFacade).isChainTaskRunning(queue)
        }
    }

    private void installCbtAgents(AtomicBoolean bitmapFails, AtomicBoolean rollbackFails) {
        env.simulator(CbtBackupKvmCommands.REST_API_TAKE_VOLUME_CBT_BACKUP_PATH) { HttpEntity<String> entity ->
            def cmd = JSONObjectUtil.toObject(entity.body, CbtBackupKvmCommands.TakeVolumeCbtBackupCmd.class)
            cmd.volumeInfos.eachWithIndex { info, index ->
                info.scratchNodeName = "case-scratch-" + index
                info.bitmapName = "case-bitmap-" + index
                info.mode = "full"
                info.nbdPort = 10809L + index
            }
            return new CbtBackupKvmCommands.TakeVolumeCbtBackupResponse(volumeInfos: cmd.volumeInfos)
        }
        env.simulator(CbtBackupKvmCommands.REST_API_GET_VOLUMES_BITMAPS_PATH) { HttpEntity<String> entity ->
            def cmd = JSONObjectUtil.toObject(entity.body, CbtBackupKvmCommands.GetVolumesBitmapCmd.class)
            def reply = new CbtBackupKvmCommands.GetVolumesBitmapResponse(volumeInfos: cmd.volumeInfos)
            if (bitmapFails.get()) {
                reply.error = "controlled bitmap failure"
            }
            return reply
        }
        env.simulator(CbtBackupKvmCommands.REST_API_ROLLBACK_VOLUME_CBT_BACKUP_PATH) {
            AgentResponse reply = new AgentResponse()
            if (rollbackFails.get()) {
                reply.error = "controlled rollback failure"
            }
            return reply
        }
        env.simulator(CbtBackupKvmCommands.REST_API_STOP_VOLUME_CBT_BACKUP_PATH) { new AgentResponse() }
    }

    private void testFailedRollbackKeepsConflictsUntilCleanup() {
        prepareBackupStorageFixture()
        createTaskFixture(CbtTaskStatus.Created)
        AtomicBoolean rollbackFails = new AtomicBoolean(true)
        installCbtAgents(new AtomicBoolean(true), rollbackFails)
        def enabled = new EnableCbtTaskAction(uuid: taskUuid, bitmapName: "case-bitmap",
                sessionId: adminSession()).call()
        assert enabled.error != null
        assert dbFindByUuid(taskUuid, CbtTaskVO.class).status == CbtTaskStatus.Failed
        def records = Q.New(VolumeCbtBackupRecordVO.class)
                .eq(VolumeCbtBackupRecordVO_.taskUuid, taskUuid).list()
        assert records.size() == 2
        assert records.every { it.cleanupAttemptUuid != null && it.scratchNodeName != null }
        def scratchUuids = Q.New(CbtTaskResourceRefVO.class).eq(CbtTaskResourceRefVO_.taskUuid, taskUuid)
                .eq(CbtTaskResourceRefVO_.resourceType, VolumeVO.class.simpleName)
                .select(CbtTaskResourceRefVO_.resourceUuid).listValues()
        assert scratchUuids.size() == 2
        assert scratchUuids.every { dbFindByUuid(it, VolumeVO.class) != null }
        Map before = protectedState()
        int disabledBefore = disableRequests.get()
        conflictingOperations().each { String operation, Closure request ->
            def result = request()
            assert result.error != null: operation + " must reject pending CBT cleanup"
            String error = JSONObjectUtil.toJsonString(result.error)
            assert error.contains("pending runtime cleanup") && error.contains(taskUuid): error
            assert protectedState() == before
        }
        assert disableRequests.get() == disabledBefore
        rollbackFails.set(false)
        CloudBus bus = bean(CloudBus)
        DisableCbtTaskMsg cleanup = new DisableCbtTaskMsg(uuid: taskUuid, force: true)
        bus.makeTargetServiceIdByResourceUuid(cleanup, CbtBackupConstant.SERVICE_ID, vm.uuid)
        assert bus.call(cleanup).success
        assert !Q.New(VolumeCbtBackupRecordVO.class).eq(VolumeCbtBackupRecordVO_.taskUuid, taskUuid).isExists()
        assert scratchUuids.every { dbFindByUuid(it, VolumeVO.class) == null }
        def snapshot = createVolumeSnapshot { volumeUuid = vm.rootVolumeUuid; name = "after-cleanup" }
        assert snapshot.status == "Ready"
        deleteVolumeSnapshot { uuid = snapshot.uuid }
        deleteTaskFixture()
        deleteBackupStorageFixture()
        env.cleanSimulatorHandlers()
    }

    private void deleteTaskFixture() {
        SQL.New(CbtTaskVO.class).eq(CbtTaskVO_.uuid, taskUuid)
                .set(CbtTaskVO_.status, CbtTaskStatus.Stopped).update()
        deleteCbtTask { uuid = taskUuid; force = true }
        assert !Q.New(CbtTaskResourceRefVO.class).eq(CbtTaskResourceRefVO_.taskUuid, taskUuid).isExists()
        assert dbFindByUuid(taskUuid, CbtTaskVO.class) == null
        taskUuid = null
    }

    private void deleteBackupStorageFixture() {
        DatabaseFacade dbf = bean(DatabaseFacade.class)
        SQL.New(BackupStorageZoneRefVO.class)
                .eq(BackupStorageZoneRefVO_.backupStorageUuid, backupStorageUuid).delete()
        dbf.removeByPrimaryKey(backupStorageUuid, ImageStoreBackupStorageVO.class)
        dbf.eoCleanup(BackupStorageVO.class, backupStorageUuid)
        assert dbFindByUuid(backupStorageUuid, BackupStorageVO.class) == null
        backupStorageUuid = null
    }

    @Override
    void clean() {
        env.delete()
    }
}
