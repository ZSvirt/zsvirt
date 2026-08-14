package org.zstack.kvm.vmfiles;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.EventCallback;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.core.cloudbus.ResourceDestinationMaker;
import org.zstack.core.config.GlobalConfigUpdateExtensionPoint;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.thread.PeriodicTask;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.timeout.TimeHelper;
import org.zstack.header.Component;
import org.zstack.header.core.FutureReturnValueCompletion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.host.HostVO;
import org.zstack.header.host.HostVO_;
import org.zstack.header.message.MessageReply;
import org.zstack.header.vm.VmCanonicalEvents;
import org.zstack.header.vm.VmInstanceAO_;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceEO;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmInstanceVO_;
import org.zstack.header.vm.additions.VmHostBackupFileDeletionMsg;
import org.zstack.header.vm.additions.VmHostBackupFileVO;
import org.zstack.header.vm.additions.VmHostFileContentVO;
import org.zstack.header.vm.additions.VmHostFileContentVO_;
import org.zstack.header.vm.additions.VmHostFileDeletionMsg;
import org.zstack.header.vm.additions.VmHostFileOperation;
import org.zstack.header.vm.additions.VmHostFileSyncReason;
import org.zstack.header.vm.additions.VmHostFileType;
import org.zstack.header.vm.additions.VmHostFileVO;
import org.zstack.header.vm.additions.VmHostFileVO_;
import org.zstack.header.vo.ResourceVO;
import org.zstack.header.vo.ResourceVO_;
import org.zstack.kvm.KVMAgentCommands;
import org.zstack.kvm.KVMConstant;
import org.zstack.kvm.KVMGlobalConfig;
import org.zstack.kvm.KvmCommandSender;
import org.zstack.kvm.vmfiles.message.SyncVmHostFilesFromHostMsg;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.data.Pair;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Tuple;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.operr;
import static org.zstack.utils.CollectionUtils.transformToSet;

public class VmHostFileTracker implements Component {
    private static final CLogger logger = Utils.getLogger(VmHostFileTracker.class);

    private static final long CLEANUP_INTERVAL_SECONDS = 1800;
    private static final long ONE_DAY_MS = TimeUnit.DAYS.toMillis(1);
    private static final long SEVEN_DAYS_MS = TimeUnit.DAYS.toMillis(7);
    private static final long NINETY_DAYS_MS = TimeUnit.DAYS.toMillis(90);
    private static final long KVM_CMD_TIMEOUT_MS = TimeUnit.MINUTES.toMillis(3);

    @Autowired
    private ThreadFacade threadFacade;
    @Autowired
    private CloudBus bus;
    @Autowired
    private ResourceDestinationMaker destinationMaker;
    @Autowired
    private TimeHelper timeHelper;
    @Autowired
    private EventFacade eventFacade;

    private Future<Void> trackerThread;
    private Future<Void> cleanupThread;

    @Override
    public boolean start() {
        if (CoreGlobalProperty.UNIT_TEST_ON) {
            logger.info("VmHostFileTracker is disabled in unit test");
            return true;
        }

        submitTrackerTask();
        submitCleanupTask();
        setupCanonicalEvents();

        GlobalConfigUpdateExtensionPoint listener = (oldConfig, newConfig) -> {
            logger.debug(String.format("%s changed from %s to %s, restarting vm-host-file-tracker",
                    newConfig.getCanonicalName(), oldConfig.value(), newConfig.value()));
            submitTrackerTask();
        };

        KVMGlobalConfig.VM_HOST_FILE_SYNC_INTERVAL.installUpdateExtension(listener);
        KVMGlobalConfig.VM_HOST_FILE_SYNC_CONCURRENCY.installUpdateExtension(listener);

        return true;
    }

    @Override
    public boolean stop() {
        if (trackerThread != null) {
            trackerThread.cancel(true);
        }
        if (cleanupThread != null) {
            cleanupThread.cancel(true);
        }
        return true;
    }

    private void setupCanonicalEvents() {
        eventFacade.on(VmCanonicalEvents.VM_HOST_FILE_CHANGED_PATH, new EventCallback<Object>() {
            @Override
            protected void run(Map tokens, Object data) {
                if (!(data instanceof VmCanonicalEvents.VmHostFileChangedData)) {
                    return;
                }

                VmCanonicalEvents.VmHostFileChangedData d = (VmCanonicalEvents.VmHostFileChangedData) data;
                String hostUuid = d.getHostUuid();
                String vmUuid = d.getVmUuid();
                List<String> types = d.getTypes();

                if (hostUuid == null || vmUuid == null || types == null || types.isEmpty()) {
                    logger.warn(String.format("received VM_HOST_FILE_CHANGED event with incomplete data: hostUuid=%s, vmUuid=%s", hostUuid, vmUuid));
                    return;
                }

                Timestamp now = new Timestamp(timeHelper.getCurrentTimeMillis());
                for (String type : types) {
                    VmHostFileType fileType;
                    try {
                        fileType = VmHostFileType.valueOf(type);
                    } catch (Exception e) {
                        logger.warn(String.format("ignore invalid vm host file type in event: vmUuid=%s, hostUuid=%s, type=%s",
                                vmUuid, hostUuid, type), e);
                        continue;
                    }

                    long updated = SQL.New(VmHostFileVO.class)
                            .eq(VmHostFileVO_.hostUuid, hostUuid)
                            .eq(VmHostFileVO_.vmInstanceUuid, vmUuid)
                            .eq(VmHostFileVO_.type, fileType)
                            .set(VmHostFileVO_.changeDate, now)
                            .update();

                    if (updated > 0) {
                        logger.debug(String.format("marked VmHostFile changed: vmUuid=%s, hostUuid=%s, type=%s", vmUuid, hostUuid, type));
                    } else {
                        logger.warn(String.format("no VmHostFileVO found for vmUuid=%s, hostUuid=%s, type=%s", vmUuid, hostUuid, type));
                    }
                }
            }
        });
    }

    private synchronized void submitTrackerTask() {
        if (trackerThread != null) {
            trackerThread.cancel(true);
        }

        long interval = KVMGlobalConfig.VM_HOST_FILE_SYNC_INTERVAL.value(Long.class);
        trackerThread = threadFacade.submitPeriodicTask(new PeriodicTask() {
            @Override
            public TimeUnit getTimeUnit() {
                return TimeUnit.SECONDS;
            }

            @Override
            public long getInterval() {
                return interval;
            }

            @Override
            public String getName() {
                return "vm-host-file-tracker";
            }

            @Override
            public void run() {
                logger.info("VmHostFileTracker: starting periodic check of VM host files");
                syncVmHostFiles();
            }
        });
    }

    private synchronized void submitCleanupTask() {
        if (cleanupThread != null) {
            cleanupThread.cancel(true);
        }

        cleanupThread = threadFacade.submitPeriodicTask(new PeriodicTask() {
            @Override
            public TimeUnit getTimeUnit() {
                return TimeUnit.SECONDS;
            }

            @Override
            public long getInterval() {
                return CLEANUP_INTERVAL_SECONDS;
            }

            @Override
            public String getName() {
                return "vm-host-file-cleanup-tracker";
            }

            @Override
            public void run() {
                logger.info("VmHostFileTracker: starting periodic cleanup of expired VM host files");
                try {
                    cleanupExpiredVmHostFiles();
                } catch (Throwable t) {
                    logger.warn("error during VmHostFile cleanup", t);
                }
                try {
                    cleanupExpiredVmHostBackupFiles();
                } catch (Throwable t) {
                    logger.warn("error during VmHostBackupFile cleanup", t);
                }
            }
        });
    }

    /**
     * Vm host files the periodic sync should consider: (1) running VM files on the current host,
     * and (2) stopped VM files on the last host when at least one is dirty ({@code changeDate} set).
     */
    private List<VmHostFileVO> listVmHostFilesToSync() {
        List<VmHostFileVO> hostFiles = new ArrayList<>(Q.New(VmHostFileVO.class, VmInstanceVO.class)
                .table0()
                    .eq(VmHostFileVO_.vmInstanceUuid).table1(VmInstanceAO_.uuid)
                    .eq(VmHostFileVO_.hostUuid).table1(VmInstanceAO_.hostUuid)
                    .selectThisTable()
                .table1()
                    .eq(VmInstanceAO_.state, VmInstanceState.Running)
                .list());
        hostFiles.addAll(Q.New(VmHostFileVO.class, VmInstanceVO.class)
                .table0()
                    .eq(VmHostFileVO_.vmInstanceUuid).table1(VmInstanceAO_.uuid)
                    .eq(VmHostFileVO_.hostUuid).table1(VmInstanceAO_.lastHostUuid)
                    .notNull(VmHostFileVO_.changeDate)
                    .selectThisTable()
                .table1()
                    .eq(VmInstanceAO_.state, VmInstanceState.Stopped)
                    .notNull(VmInstanceAO_.lastHostUuid)
                .list());
        return hostFiles;
    }

    private void syncVmHostFiles() {
        List<VmHostFileVO> hostFiles = listVmHostFilesToSync();
        if (hostFiles.isEmpty()) {
            return;
        }

        Map<String, List<VmHostFileVO>> grouped = hostFiles.stream()
                .collect(Collectors.groupingBy(f -> f.getVmInstanceUuid() + "::" + f.getHostUuid()));
        List<List<VmHostFileVO>> groups = new ArrayList<>(grouped.values());

        long now = timeHelper.getCurrentTimeMillis();
        long checkIntervalMs = KVMGlobalConfig.VM_HOST_FILE_SYNC_INTERVAL.value(Long.class) * 1000;
        long forceSyncThresholdMs = checkIntervalMs * 100;
        int concurrency = KVMGlobalConfig.VM_HOST_FILE_SYNC_CONCURRENCY.value(Integer.class);

        new While<>(groups).step((group, whileCompletion) -> {
            VmHostFileVO first = group.get(0);
            String vmUuid = first.getVmInstanceUuid();
            String hostUuid = first.getHostUuid();

            if (!destinationMaker.isManagedByUs(vmUuid)) {
                whileCompletion.done();
                return;
            }

            boolean hasChanged = group.stream().anyMatch(f -> f.getChangeDate() != null);
            String syncReason;
            if (hasChanged) {
                syncReason = VmHostFileSyncReason.PeriodicDirtyCheck.reason();
            } else {
                // check if force sync is needed based on lastSyncDate
                Timestamp oldestLastSync = group.stream()
                        .map(VmHostFileVO::getLastSyncDate)
                        .filter(Objects::nonNull)
                        .min(Comparator.naturalOrder())
                        .orElse(null);

                if (oldestLastSync != null && (now - oldestLastSync.getTime()) < forceSyncThresholdMs) {
                    whileCompletion.done();
                    return;
                }
                syncReason = VmHostFileSyncReason.PeriodicForceSync.reason();
            }

            SyncVmHostFilesFromHostMsg syncMsg = new SyncVmHostFilesFromHostMsg();
            syncMsg.setHostUuid(hostUuid);
            syncMsg.setVmUuid(vmUuid);
            syncMsg.setSyncReason(syncReason);

            for (VmHostFileVO file : group) {
                if (file.getType() == VmHostFileType.NvRam) {
                    syncMsg.setNvRamPath(file.getPath());
                } else if (file.getType() == VmHostFileType.TpmState) {
                    syncMsg.setTpmStateFolder(file.getPath());
                }
            }

            bus.makeLocalServiceId(syncMsg, VmInstanceConstant.SECURE_BOOT_SERVICE_ID);
            bus.send(syncMsg, new CloudBusCallBack(whileCompletion) {
                @Override
                public void run(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        logger.warn(String.format("failed to sync VM host file[vmUuid=%s] from host[uuid=%s]: %s",
                                vmUuid, hostUuid, reply.getError().getReadableDetails()));
                    }
                    whileCompletion.done();
                }
            });
        }, concurrency).run(new WhileDoneCompletion(null) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                // periodic check round finished, empty callback
            }
        });
    }

    /**
     * active VM:                In each type, max(lastOpDate) will not be deleted;
     *                           delete lastOpDate > 1d if delete command success
     * VmInstanceEO(deleted):    delete lastOpDate > 90d if record is latest;
     *                           delete lastOpDate > 7d if record is not latest even if delete command failed
     *                           delete lastOpDate < 7d if record is not latest and delete command success
     * VmInstanceEO(not exists): delete
     * Host not exists:          delete and not send command
     */
    private void cleanupExpiredVmHostFiles() {
        List<Tuple> vmTypeList = Q.New(VmHostFileVO.class)
                .select(VmHostFileVO_.vmInstanceUuid, VmHostFileVO_.type)
                .listTuple();
        if (vmTypeList.isEmpty()) {
            return;
        }

        Set<Pair<String, VmHostFileType>> pairs = new HashSet<>();
        for (Tuple tuple : vmTypeList) {
            pairs.add(new Pair<>(tuple.get(0, String.class), tuple.get(1, VmHostFileType.class)));
        }

        long now = timeHelper.getCurrentTimeMillis();
        CollectionUtils.safeForEach(pairs, pair -> {
            String vmUuid = pair.first();
            if (!destinationMaker.isManagedByUs(vmUuid)) {
                return;
            }

            VmHostFileType type = pair.second();
            List<VmHostFileVO> files = Q.New(VmHostFileVO.class)
                    .eq(VmHostFileVO_.vmInstanceUuid, vmUuid)
                    .eq(VmHostFileVO_.type, type)
                    .list();
            if (files.isEmpty()) {
                return; // should not be here
            }
            files.sort((a, b) -> b.getLastOpDate().compareTo(a.getLastOpDate()));

            // Re-check VM status each iteration as per user's requirement
            VmInstanceEO eo = Q.New(VmInstanceEO.class).eq(VmInstanceVO_.uuid, vmUuid).find();
            boolean vmActive = eo != null && eo.getDeleted() == null;

            if (vmActive) {
                cleanupActiveVmHostFiles(files, now);
            } else if (eo != null && eo.getDeleted() != null) {
                cleanupDeletedVmHostFiles(files, now);
            } else {
                for (VmHostFileVO file : files) {
                    logger.info(String.format("deleting VmHostFileVO[uuid=%s] for expunged VM[uuid=%s]",
                            file.getUuid(), vmUuid));
                    deleteVmHostFileFromDb(file.getUuid());
                }
            }
        });
    }

    private void cleanupActiveVmHostFiles(List<VmHostFileVO> files, long now) {
        if (files.size() <= 1) {
            return;
        }

        VmHostFileVO latestWithContent = null;
        for (VmHostFileVO file : files) {
            if (hasContent(file.getUuid())) {
                latestWithContent = file;
                break;
            }
        }

        boolean allOlderThanOneDay = files.stream()
                .allMatch(f -> now - f.getLastOpDate().getTime() > ONE_DAY_MS);

        for (int i = 1; i < files.size(); i++) {
            VmHostFileVO file = files.get(i);
            long age = now - file.getLastOpDate().getTime();

            if (age <= ONE_DAY_MS) {
                continue;
            }

            if (allOlderThanOneDay && latestWithContent != null
                    && latestWithContent.getUuid().equals(file.getUuid())
                    && !latestWithContent.getUuid().equals(files.get(0).getUuid())) {
                continue;
            }

            if (hasContent(file.getUuid())) {
                boolean latestHasContent = hasContent(files.get(0).getUuid());
                if (!latestHasContent && latestWithContent != null
                        && latestWithContent.getUuid().equals(file.getUuid())) {
                    continue;
                }
            }

            boolean cmdSuccess = sendDeleteCommandToHost(file);
            if (cmdSuccess) {
                logger.info(String.format("deleting expired VmHostFileVO[uuid=%s, vm=%s, type=%s, age=%d hours]",
                        file.getUuid(), file.getVmInstanceUuid(), file.getType(), TimeUnit.MILLISECONDS.toHours(age)));
                deleteVmHostFileFromDb(file.getUuid());
            } else {
                logger.warn(String.format("failed to delete host file for VmHostFileVO[uuid=%s], skipping DB deletion",
                        file.getUuid()));
            }
        }
    }

    private void cleanupDeletedVmHostFiles(List<VmHostFileVO> files, long now) {
        for (int i = 0; i < files.size(); i++) {
            VmHostFileVO file = files.get(i);
            long age = now - file.getLastOpDate().getTime();

            if (i == 0 && age <= NINETY_DAYS_MS) {
                continue;
            }

            if (age <= SEVEN_DAYS_MS) {
                boolean cmdSuccess = sendDeleteCommandToHost(file);
                if (!cmdSuccess) {
                    logger.warn(String.format(
                            "KVM delete failed for VmHostFileVO[uuid=%s] of deleted VM, lastOpDate within 7 days, skipping",
                            file.getUuid()));
                    continue;
                }
            } else {
                sendDeleteCommandToHost(file);
            }

            logger.info(String.format("deleting VmHostFileVO[uuid=%s] for deleted VM[uuid=%s, type=%s, age=%d days]",
            file.getUuid(), file.getVmInstanceUuid(), file.getType(), TimeUnit.MILLISECONDS.toDays(age)));
            deleteVmHostFileFromDb(file.getUuid());
        }
    }

    /**
     * resourceVO no longer exists: delete
     * Other:                       do not delete
     */
    private void cleanupExpiredVmHostBackupFiles() {
        List<VmHostBackupFileVO> allBackupFiles = Q.New(VmHostBackupFileVO.class).list();
        if (allBackupFiles.isEmpty()) {
            return;
        }

        Set<String> existingResourceUuids = new HashSet<>(Q.New(ResourceVO.class)
                .in(ResourceVO_.uuid, transformToSet(allBackupFiles, VmHostBackupFileVO::getResourceUuid))
                .select(ResourceVO_.uuid)
                .listValues());

        CollectionUtils.safeForEach(allBackupFiles, backupFile -> {
            String resourceUuid = backupFile.getResourceUuid();
            if (!destinationMaker.isManagedByUs(resourceUuid)) {
                return;
            }

            if (existingResourceUuids.contains(resourceUuid)) {
                return;
            }

            logger.info(String.format(
                    "deleting VmHostBackupFileVO[uuid=%s] -> referenced resource[uuid=%s] no longer exists",
                    backupFile.getUuid(), resourceUuid));
            deleteVmHostBackupFileFromDb(backupFile.getUuid());
        });
    }

    /**
     * Send a KVM delete command to the host for the given file.
     * Returns true if the command succeeded or the host doesn't exist (allowing DB cleanup).
     */
    private boolean sendDeleteCommandToHost(VmHostFileVO file) {
        String hostUuid = file.getHostUuid();

        // If host is gone (soft-deleted or expunged), allow direct DB cleanup
        if (!Q.New(HostVO.class).eq(HostVO_.uuid, hostUuid).isExists()) {
            return true;
        }

        KVMAgentCommands.WriteVmHostFileContentCmd cmd = new KVMAgentCommands.WriteVmHostFileContentCmd();
        KVMAgentCommands.VmHostFileTO to = new KVMAgentCommands.VmHostFileTO();
        to.setPath(file.getPath());
        to.setType(file.getType().toString());
        to.setOperation(VmHostFileOperation.Delete.toString());
        cmd.setHostFiles(Collections.singletonList(to));

        FutureReturnValueCompletion future = new FutureReturnValueCompletion(null);
        new KvmCommandSender(hostUuid).send(cmd, KVMConstant.WRITE_VM_HOST_FILE_PATH, wrapper -> {
            KVMAgentCommands.WriteVmHostFileContentResponse rsp =
                    wrapper.getResponse(KVMAgentCommands.WriteVmHostFileContentResponse.class);
            return rsp.isSuccess() ? null : operr("failed to delete host file[path=%s] on host[uuid=%s]",
                    file.getPath(), hostUuid);
        }, future);

        future.await(KVM_CMD_TIMEOUT_MS);
        return future.isSuccess();
    }

    private boolean hasContent(String fileUuid) {
        return Q.New(VmHostFileContentVO.class).eq(VmHostFileContentVO_.uuid, fileUuid).isExists();
    }

    private void deleteVmHostFileFromDb(String fileUuid) {
        VmHostFileDeletionMsg msg = new VmHostFileDeletionMsg();
        msg.setUuid(fileUuid);
        msg.setForceDelete(false);
        bus.makeLocalServiceId(msg, VmInstanceConstant.SECURE_BOOT_SERVICE_ID);
        MessageReply reply = bus.call(msg);
        if (!reply.isSuccess()) {
            logger.warn(String.format("failed to delete VmHostFileVO[uuid=%s] via VmHostFileDeletionMsg: %s",
                    fileUuid, reply.getError().getReadableDetails()));
        }
    }

    /**
     * Deletes a {@link VmHostBackupFileVO} only through {@link VmHostBackupFileDeletionMsg} so related rows
     * are cleaned in the handler/factory order. If the message fails, do not issue a second direct SQL delete:
     * removing {@code VmHostBackupFileVO} alone could orphan ref tables that would then never be cleaned up.
     */
    private void deleteVmHostBackupFileFromDb(String fileUuid) {
        VmHostBackupFileDeletionMsg msg = new VmHostBackupFileDeletionMsg();
        msg.setUuid(fileUuid);
        msg.setForceDelete(true);
        bus.makeLocalServiceId(msg, VmInstanceConstant.SECURE_BOOT_SERVICE_ID);
        MessageReply reply = bus.call(msg);
        if (!reply.isSuccess()) {
            // No fallback DB delete: keep the row until a successful deletion message runs the full cleanup path.
            logger.warn(String.format("failed to delete VmHostBackupFileVO[uuid=%s] via VmHostBackupFileDeletionMsg: %s",
                    fileUuid, reply.getError().getReadableDetails()));
        }
    }
}
