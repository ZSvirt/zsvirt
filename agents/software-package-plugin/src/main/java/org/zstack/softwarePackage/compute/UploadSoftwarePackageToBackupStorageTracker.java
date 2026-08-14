package org.zstack.softwarePackage.compute;

import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.progress.ActionProgressService;
import org.zstack.core.progress.TaskProgressReporter;
import org.zstack.core.thread.CancelablePeriodicTask;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.core.Completion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.longjob.LongJobState;
import org.zstack.header.longjob.LongJobVO;
import org.zstack.header.longjob.LongJobVO_;
import org.zstack.header.message.MessageReply;
import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.storage.backup.*;
import org.zstack.softwarePackage.SoftwarePackageConstant;
import org.zstack.softwarePackage.entity.SoftwarePackageStatus;
import org.zstack.softwarePackage.entity.SoftwarePackageVO;
import org.zstack.softwarePackage.entity.SoftwarePackageVO_;
import org.zstack.softwarePackage.entity.UploadSoftwarePackageToBackupStorageLongJobData;
import org.zstack.softwarePackage.header.SoftwarePackageInventory;
import org.zstack.softwarePackage.message.CleanSoftwarePackageMsg;
import org.zstack.softwarePackage.message.CleanUpgradeSoftwarePackageMsg;
import org.zstack.tag.SystemTagCreator;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.zstack.core.Platform.err;
import static org.zstack.header.Constants.THREAD_CONTEXT_API;
import static org.zstack.longjob.LongJobUtils.cancelErr;
import static org.zstack.softwarePackage.SoftwarePackageGlobalConfig.*;
import static org.zstack.softwarePackage.SoftwarePackagePluginErrors.*;
import static org.zstack.softwarePackage.compute.SoftwarePackageCanonicalEvents.SOFTWARE_PACKAGE_TRACK_RESULT_PATH;
import static org.zstack.softwarePackage.compute.SoftwarePackageSystemTags.SOFTWARE_PACKAGE_UPGRADE_PACKAGE_PATH;
import static org.zstack.utils.CollectionDSL.*;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class UploadSoftwarePackageToBackupStorageTracker {
    private static final CLogger logger = Utils.getLogger(UploadSoftwarePackageToBackupStorageTracker.class);

    @Autowired
    protected DatabaseFacade databases;
    @Autowired
    private CloudBus bus;
    @Autowired
    private ThreadFacade threads;
    @Autowired
    private EventFacade events;
    @Autowired
    private PluginRegistry plugins;

    private volatile String apiId = ThreadContext.get(THREAD_CONTEXT_API);
    TaskProgressReporter reporter;
    private boolean upgrade = false;

    private String findApiId(String softwarePackageUuid) {
        if (apiId != null) {
            return apiId;
        }
        apiId = Q.New(LongJobVO.class)
                .eq(LongJobVO_.targetResourceUuid, softwarePackageUuid)
                .notIn(LongJobVO_.state, LongJobState.finalStates)
                .orderBy(LongJobVO_.createDate, SimpleQuery.Od.DESC)
                .select(LongJobVO_.apiId).limit(1).findValue();
        return apiId;
    }

    private String resolveApiId(String softwarePackageUuid) {
        String resolvedApiId = findApiId(softwarePackageUuid);
        if (apiId == null) {
            throw new RuntimeException(String.format(
                    "cannot resolve apiId for software package [uuid:%s], no matching LongJobVO found",
                    softwarePackageUuid));
        }
        return resolvedApiId;
    }

    @SuppressWarnings("unchecked")
    public void runTrackTask(UploadSoftwarePackageToBackupStorageLongJobData msgData) {
        final int maxNumOfFailure = UPLOAD_FAILURE_TOLERANCE_COUNT.value(Integer.class);
        final long maxIdleSecond = UPLOAD_MAX_IDLE_IN_SECONDS.value(Long.class);

        logger.debug(String.format("starting tracker for software package upload [uuid: %s, backupStorageUuid: %s]",
                msgData.softwarePackageUuid, msgData.backupStorageUuid));

        threads.submitCancelablePeriodicTask(new CancelablePeriodicTask() {
            private long numError = 0;
            private long createdTime = System.currentTimeMillis();
            private volatile boolean uploadCompleted;
            private final AtomicBoolean cancelCleanupStarted = new AtomicBoolean(false);

            private boolean overMaxIdleTime(long lastOpTimeInMills) {
                long latestTime = Long.max(lastOpTimeInMills, createdTime);
                return System.currentTimeMillis() - latestTime > TimeUnit.SECONDS.toMillis(maxIdleSecond);
            }

            private LongJobVO findLongJob() {
                String resolvedApiId = findApiId(msgData.softwarePackageUuid);
                if (resolvedApiId == null) {
                    return null;
                }

                return Q.New(LongJobVO.class)
                        .eq(LongJobVO_.apiId, resolvedApiId)
                        .orderByDesc(LongJobVO_.createDate)
                        .limit(1)
                        .find();
            }

            private boolean cancelTerminalIfNeeded() {
                LongJobVO job = findLongJob();
                if (job == null) {
                    return false;
                }

                if (job.getState() == LongJobState.Canceling) {
                    String canceledStatus = upgrade
                            ? SoftwarePackageStatus.UpgradePackageUploadFailed.toString()
                            : SoftwarePackageStatus.UploadFailed.toString();
                    SQL.New(SoftwarePackageVO.class)
                            .eq(SoftwarePackageVO_.uuid, msgData.softwarePackageUuid)
                            .in(SoftwarePackageVO_.status, upgrade
                                    ? list(SoftwarePackageStatus.Upgrading.toString(), SoftwarePackageStatus.UpgradePackageUploaded.toString())
                                    : list(SoftwarePackageStatus.Uploading.toString()))
                            .set(SoftwarePackageVO_.status, canceledStatus)
                            .update();
                    if (uploadCompleted) {
                        cleanCanceledUpload(job);
                    } else {
                        fireEvent(null, cancelErr(job.getUuid()));
                    }
                    return true;
                }

                if (job.getState() == LongJobState.Canceled) {
                    if (uploadCompleted) {
                        cleanCanceledUpload(job);
                    } else {
                        fireEvent(null, cancelErr(job.getUuid()));
                    }
                    return true;
                }

                return false;
            }

            private void cleanCanceledUpload(LongJobVO job) {
                if (!cancelCleanupStarted.compareAndSet(false, true)) {
                    return;
                }

                NeedReplyMessage cleanMsg;
                if (upgrade) {
                    cleanMsg = buildCleanUpgradeSoftwarePackageMsg(msgData);
                } else {
                    if (!Q.New(SoftwarePackageVO.class)
                            .eq(SoftwarePackageVO_.uuid, msgData.softwarePackageUuid).isExists()) {
                        deleteUploadedFiles(msgData);
                        fireEvent(null, cancelErr(job.getUuid()));
                        return;
                    }

                    CleanSoftwarePackageMsg msg = new CleanSoftwarePackageMsg();
                    msg.setUuid(msgData.softwarePackageUuid);
                    cleanMsg = msg;
                }
                bus.makeLocalServiceId(cleanMsg, SoftwarePackageConstant.SERVICE_ID);
                bus.send(cleanMsg, new CloudBusCallBack(cleanMsg) {
                    @Override
                    public void run(MessageReply reply) {
                        if (reply.isSuccess()) {
                            fireEvent(null, cancelErr(job.getUuid()));
                            return;
                        }

                        cancelCleanupStarted.set(false);
                        logger.warn(String.format(
                                "failed to clean canceled uploaded software package [uuid:%s]: %s",
                                msgData.softwarePackageUuid, reply.getError().getReadableDetails()));
                    }
                });
            }

            private CleanUpgradeSoftwarePackageMsg buildCleanUpgradeSoftwarePackageMsg(
                    UploadSoftwarePackageToBackupStorageLongJobData data) {
                CleanUpgradeSoftwarePackageMsg msg = new CleanUpgradeSoftwarePackageMsg();
                msg.setUuid(data.softwarePackageUuid);
                msg.setUpgradeBackupStorageUuid(data.backupStorageUuid);
                msg.setUpgradeBackupStorageHostUuid(data.backupStorageHostUuid);
                msg.setUpgradeInstallPath(data.installPath);
                msg.setUpgradeUnzipInstallPath(data.unzipInstallPath);
                msg.setOriginalInstallPath(data.originalInstallPath);
                msg.setOriginalUnzipInstallPath(data.originalUnzipInstallPath);
                msg.setOriginalMd5sum(data.originalMd5sum);
                msg.setOriginalSize(data.originalSize);
                msg.setOriginalBackupStorageUuid(data.originalBackupStorageUuid);
                msg.setOriginalBackupStorageHostUuid(data.originalBackupStorageHostUuid);
                return msg;
            }

            private void markUploadOrUpgradeFailed(SoftwarePackageVO vo, ErrorCode error) {
                if (cancelTerminalIfNeeded()) {
                    return;
                }

                String status = upgrade
                        ? SoftwarePackageStatus.UpgradePackageUploadFailed.toString()
                        : SoftwarePackageStatus.UploadFailed.toString();
                vo.setStatus(status);
                if (!updateSoftwarePackageStatus(msgData.softwarePackageUuid, status,
                        upgrade ? SoftwarePackageStatus.Upgrading.toString() : SoftwarePackageStatus.Uploading.toString())) {
                    return;
                }
                fireEvent(SoftwarePackageInventory.valueOf(vo), error);
            }

            private void markCompletion(final GetFileDownloadProgressFromBackupStorageHostReply dr) {
                if (cancelTerminalIfNeeded()) {
                    return;
                }

                SoftwarePackageVO vo = databases.findByUuid(msgData.softwarePackageUuid, SoftwarePackageVO.class);
                if (vo == null) {
                    fireEvent(null, err(SysErrors.RESOURCE_NOT_FOUND,
                            "software package [uuid:%s] not found", msgData.softwarePackageUuid));
                    return;
                }

                vo.setMd5sum(dr.getMd5sum());
                vo.setSize(dr.getActualSize());
                databases.update(vo);

                UnzipFileOnBackupStorageHostMsg unzipMsg = new UnzipFileOnBackupStorageHostMsg();
                unzipMsg.setBackupStorageUuid(msgData.backupStorageUuid);
                unzipMsg.setBackupStorageHostUuid(msgData.backupStorageHostUuid);
                unzipMsg.setInstallPath(msgData.installPath);
                bus.makeTargetServiceIdByResourceUuid(unzipMsg, BackupStorageConstant.SERVICE_ID, msgData.backupStorageUuid);
                bus.send(unzipMsg, new CloudBusCallBack(unzipMsg) {
                    @Override
                    public void run(MessageReply reply) {
                        SoftwarePackageVO vo = databases.findByUuid(msgData.softwarePackageUuid, SoftwarePackageVO.class);
                        if (vo == null) {
                            fireEvent(null, err(SysErrors.RESOURCE_NOT_FOUND,
                                    "software package [uuid:%s] not found", msgData.softwarePackageUuid));
                            return;
                        }

                        if (!reply.isSuccess()) {
                            deleteUploadedFiles(msgData);
                            markUploadOrUpgradeFailed(vo,
                                    err(GENERAL_ERROR, "failed to unzip software package on backup storage host").withOpaque("error", reply.getError()));
                            return;
                        }

                        UnzipFileOnBackupStorageHostReply unzipR = reply.castReply();

                        vo.setUnzipInstallPath(unzipR.getUnzipInstallPath());
                        vo = databases.updateAndRefresh(vo);
                        msgData.unzipInstallPath = unzipR.getUnzipInstallPath();

                        if (cancelTerminalIfNeeded()) {
                            return;
                        }

                        long imagesTotalSize;
                        UploadSoftwarePackageToBackupStorageExtensionPoint ext = plugins.getExtensionFromMap(vo.getType(), UploadSoftwarePackageToBackupStorageExtensionPoint.class);
                        if (ext == null) {
                            deleteUploadedFiles(msgData);
                            markUploadOrUpgradeFailed(vo, err(UNSUPPORTED_SOFTWARE_TYPE,
                                    "no UploadSoftwarePackageToBackupStorageExtensionPoint found for software package type: %s", vo.getType()));
                            return;
                        }
                        Map<String, Long> imagesSize = ext.getImagesSize(unzipR.getFileSizes());
                        if (imagesSize.isEmpty()) {
                            deleteUploadedFiles(msgData);
                            markUploadOrUpgradeFailed(vo, err(INVALID_SOFTWARE_PACKAGE, "no images found in software package"));
                            return;
                        }
                        imagesTotalSize = imagesSize.values().stream().mapToLong(Long::longValue).sum();
                        msgData.imagesPath = new ArrayList<>(imagesSize.keySet());

                        Long backupStorageAvailableCapacity = Q.New(BackupStorageVO.class)
                                .eq(BackupStorageVO_.uuid, msgData.backupStorageUuid)
                                .select(BackupStorageVO_.availableCapacity).findValue();
                        if (backupStorageAvailableCapacity == null) {
                            deleteUploadedFiles(msgData);
                            markUploadOrUpgradeFailed(vo, err(SysErrors.RESOURCE_NOT_FOUND,
                                    "backup storage [uuid:%s] not found", msgData.backupStorageUuid));
                            return;
                        }
                        if (imagesTotalSize > backupStorageAvailableCapacity) {
                            deleteUploadedFiles(msgData);

                            markUploadOrUpgradeFailed(vo, err(INSUFFICIENT_CAPACITY_FOR_BACKUP_STORAGE,
                                    "imagesTotalSize %d greater than backupStorageAvailableCapacity %d",
                                    imagesTotalSize, backupStorageAvailableCapacity));
                            return;
                        }

                        if (upgrade) {
                            String upgradePackagePath = ext.getUpgradePackagePath(unzipR.getFileSizes());
                            if (upgradePackagePath == null) {
                                deleteUploadedFiles(msgData);
                                markUploadOrUpgradeFailed(vo, err(INVALID_SOFTWARE_PACKAGE,
                                        "no upgrade package found in software package"));
                                return;
                            } else {
                                msgData.upgradePackagePath = upgradePackagePath;
                                SystemTagCreator creator = SOFTWARE_PACKAGE_UPGRADE_PACKAGE_PATH.newSystemTagCreator(msgData.softwarePackageUuid);
                                creator.inherent = false;
                                creator.recreate = true;
                                creator.setTagByTokens(map(e(SoftwarePackageSystemTags.SOFTWARE_PACKAGE_UPGRADE_PACKAGE_PATH_TOKEN, upgradePackagePath)));
                                creator.create();
                            }

                            if (!updateSoftwarePackageStatus(msgData.softwarePackageUuid,
                                    SoftwarePackageStatus.UpgradePackageUploaded.toString(),
                                    SoftwarePackageStatus.Upgrading.toString())) {
                                return;
                            }

                            SoftwarePackageVO finalVo1 = vo;
                            ext.upgradeSoftwarePackage(vo, msgData, new Completion(null) {
                                @Override
                                public void success() {
                                    finalVo1.setStatus(SoftwarePackageStatus.Upgraded.toString());
                                    if (!updateSoftwarePackageStatus(msgData.softwarePackageUuid,
                                            SoftwarePackageStatus.Upgraded.toString(),
                                            SoftwarePackageStatus.UpgradePackageUploaded.toString())) {
                                        return;
                                    }
                                    fireEvent(SoftwarePackageInventory.valueOf(finalVo1), null);
                                }

                                @Override
                                public void fail(ErrorCode errorCode) {
                                    // Intentionally NOT deleting uploaded files here.
                                    // Files must be preserved for Reexecute.
                                    if (!updateSoftwarePackageStatus(msgData.softwarePackageUuid,
                                            SoftwarePackageStatus.UpgradeExecuteFailed.toString(),
                                            SoftwarePackageStatus.UpgradePackageUploaded.toString())) {
                                        return;
                                    }
                                    finalVo1.setStatus(SoftwarePackageStatus.UpgradeExecuteFailed.toString());
                                    fireEvent(SoftwarePackageInventory.valueOf(finalVo1),
                                            err(GENERAL_ERROR, "failed to upgrade").withCause(errorCode));
                                }
                            });
                            return;
                        }

                        ext.afterUploadSoftwarePackageToBackupStorage(vo, msgData, new Completion(null) {
                            @Override
                            public void success() {
                                if (!updateSoftwarePackageStatus(msgData.softwarePackageUuid,
                                        SoftwarePackageStatus.Uploaded.toString(),
                                        SoftwarePackageStatus.Uploading.toString())) {
                                    return;
                                }

                                SoftwarePackageVO vo = databases.findByUuid(msgData.softwarePackageUuid, SoftwarePackageVO.class);
                                if (vo == null) {
                                    fireEvent(null, err(GENERAL_ERROR, "software package [uuid:%s] not found after upload", msgData.softwarePackageUuid));
                                    return;
                                }

                                fireEvent(SoftwarePackageInventory.valueOf(vo), null);
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                deleteUploadedFiles(msgData);
                                if (!updateSoftwarePackageStatus(msgData.softwarePackageUuid,
                                        SoftwarePackageStatus.UploadFailed.toString(),
                                        SoftwarePackageStatus.Uploading.toString())) {
                                    return;
                                }
                                SoftwarePackageVO vo = databases.findByUuid(msgData.softwarePackageUuid, SoftwarePackageVO.class);
                                if (vo == null) {
                                    fireEvent(null, err(SysErrors.RESOURCE_NOT_FOUND,
                                            "software package [uuid:%s] not found", msgData.softwarePackageUuid));
                                    return;
                                }
                                fireEvent(SoftwarePackageInventory.valueOf(vo),
                                        err(GENERAL_ERROR, "failed to upload").withCause(errorCode));
                            }
                        });
                    }
                });
            }

            private boolean updateSoftwarePackageStatus(String softwarePackageUuid, String status, String expectedStatus) {
                return updateSoftwarePackageStatus(softwarePackageUuid, status, list(expectedStatus));
            }

            private boolean updateSoftwarePackageStatus(String softwarePackageUuid, String status, Collection<String> expectedStatuses) {
                if (cancelTerminalIfNeeded()) {
                    return false;
                }

                String resolvedApiId = findApiId(softwarePackageUuid);
                int updated;
                if (resolvedApiId == null) {
                    updated = SQL.New(SoftwarePackageVO.class)
                            .eq(SoftwarePackageVO_.uuid, softwarePackageUuid)
                            .in(SoftwarePackageVO_.status, expectedStatuses)
                            .set(SoftwarePackageVO_.status, status)
                            .update();
                } else {
                    updated = SQL.New("update SoftwarePackageVO sp set sp.status = :status " +
                                    "where sp.uuid = :uuid and sp.status in (:expectedStatuses) " +
                                    "and not exists (select job.uuid from LongJobVO job " +
                                    "where job.apiId = :apiId and job.state in (:cancelStates))")
                            .param("status", status)
                            .param("uuid", softwarePackageUuid)
                            .param("expectedStatuses", expectedStatuses)
                            .param("apiId", resolvedApiId)
                            .param("cancelStates", Arrays.asList(LongJobState.Canceling, LongJobState.Canceled))
                            .execute();
                }

                if (updated > 0) {
                    return true;
                }

                cancelTerminalIfNeeded();
                return false;
            }

            private void markFailure(ErrorCode reason) {
                logger.error(String.format("upload software package to backup storage failed: %s", reason.getReadableDetails()));
                if (cancelTerminalIfNeeded()) {
                    return;
                }

                String resolvedApiId = resolveApiId(msgData.softwarePackageUuid);
                boolean continuable = resolvedApiId != null && Q.New(LongJobVO.class).eq(LongJobVO_.apiId, resolvedApiId).isExists();

                fireEvent(null, reason);
                if (reason.isError(UPLOAD_SOFTWARE_PACKAGE_INTERRUPTED) && continuable) {
                    return;
                }

                deleteUploadedFiles(msgData);

                SoftwarePackageVO vo = databases.findByUuid(msgData.softwarePackageUuid, SoftwarePackageVO.class);
                if (vo == null) {
                    logger.warn(String.format("software package [uuid:%s] not found, skip status update", msgData.softwarePackageUuid));
                    return;
                }
                String failedStatus = upgrade
                        ? SoftwarePackageStatus.UpgradePackageUploadFailed.toString()
                        : SoftwarePackageStatus.UploadFailed.toString();
                vo.setStatus(failedStatus);
                updateSoftwarePackageStatus(msgData.softwarePackageUuid, failedStatus,
                        upgrade ? SoftwarePackageStatus.Upgrading.toString() : SoftwarePackageStatus.Uploading.toString());
            }

            private void fireEvent(SoftwarePackageInventory inventory, ErrorCode error) {
                SoftwarePackageCanonicalEvents.SoftwarePackageTrackData data = new SoftwarePackageCanonicalEvents.SoftwarePackageTrackData();
                data.uuid = msgData.softwarePackageUuid;
                data.inventory = inventory;
                data.setError(error);
                events.fire(SOFTWARE_PACKAGE_TRACK_RESULT_PATH, data);
            }

            @Override
            public boolean run() {
                final GetFileDownloadProgressFromBackupStorageHostReply reply =
                        getDownloadProgress(msgData.softwarePackageUuid, msgData.backupStorageUuid, msgData.backupStorageHostUuid);
                if (!reply.isSuccess()) {
                    if (++numError <= maxNumOfFailure) {
                        return false;
                    }

                    markFailure(reply.getError());
                    return true;
                }

                if (reply.getDownloadSize() == 0 && overMaxIdleTime(createdTime)) {
                    markFailure(err(INVALID_UPLOAD_SESSION, "upload software package session expired"));
                    return true;
                }

                boolean downloadingFileSuspendedTooLong = !reply.isCompleted() && overMaxIdleTime(reply.getLastOpTime());
                if (downloadingFileSuspendedTooLong && reply.isSupportSuspend()) {
                    markFailure(err(UPLOAD_SOFTWARE_PACKAGE_INTERRUPTED, "uploading has been inactive more than %d sec", maxIdleSecond));
                    return true;
                }

                // reset the error counter
                numError = 0;

                if (!reply.isCompleted()) {
                    doReportProgress(resolveApiId(msgData.softwarePackageUuid), "uploading software package", reply.getProgress());
                    return false;
                }

                // upload completed
                uploadCompleted = true;
                doReportProgress(resolveApiId(msgData.softwarePackageUuid), "success to upload software package", 100);
                markCompletion(reply);
                return true;
            }

            @Override
            public TimeUnit getTimeUnit() {
                return TimeUnit.SECONDS;
            }

            @Override
            public long getInterval() {
                return 3;
            }

            @Override
            public String getName() {
                return String.format("tracking upload software package [uuid: %s]", msgData.softwarePackageUuid);
            }
        });
    }

    private void doReportProgress(String apiId, String taskName, long progress) {
        if (reporter == null) {
            reporter = ActionProgressService.taskProgress()
                    .withTotalStep(100L)
                    .withContent(taskName)
                    .withApiId(apiId)
                    .withCurrentStep(progress)
                    .report();
        } else {
            reporter.withCurrentStep(progress)
                    .withContent(taskName)
                    .withApiId(apiId)
                    .report();
        }
    }

    private GetFileDownloadProgressFromBackupStorageHostReply getDownloadProgress(String softwarePackageUuid, String backupStorageUuid, String backupStorageHostUuid) {
        final GetFileDownloadProgressFromBackupStorageHostMsg dmsg = new GetFileDownloadProgressFromBackupStorageHostMsg();
        dmsg.setTaskUuid(softwarePackageUuid);
        dmsg.setBackupStorageUuid(backupStorageUuid);
        dmsg.setBackupStorageHostUuid(backupStorageHostUuid);
        bus.makeTargetServiceIdByResourceUuid(dmsg, BackupStorageConstant.SERVICE_ID, backupStorageUuid);
        final MessageReply reply = bus.call(dmsg);
        if (reply.isSuccess()) {
            return reply.castReply();
        } else {
            GetFileDownloadProgressFromBackupStorageHostReply r = new GetFileDownloadProgressFromBackupStorageHostReply();
            r.setError(reply.getError());
            return r;
        }
    }

    public void setUpgrade(boolean upgrade) {
        this.upgrade = upgrade;
    }

    private void deleteUploadedFiles(UploadSoftwarePackageToBackupStorageLongJobData msgData) {
        if (msgData.backupStorageUuid == null || msgData.backupStorageHostUuid == null) {
            logger.warn(String.format("backupStorageUuid or backupStorageHostUuid is null for software package [uuid:%s], skip cleanup",
                    msgData.softwarePackageUuid));
            return;
        }

        DeleteFilesOnBackupStorageHostMsg dmsg = new DeleteFilesOnBackupStorageHostMsg();
        dmsg.setBackupStorageUuid(msgData.backupStorageUuid);
        dmsg.setBackupStorageHostUuid(msgData.backupStorageHostUuid);
        if (msgData.installPath != null) {
            dmsg.getFilePaths().add(msgData.installPath);
        }
        if (msgData.unzipInstallPath != null) {
            dmsg.getFilePaths().add(msgData.unzipInstallPath);
        }
        if (dmsg.getFilePaths().isEmpty()) {
            return;
        }
        bus.makeTargetServiceIdByResourceUuid(dmsg, BackupStorageConstant.SERVICE_ID, msgData.backupStorageUuid);
        sendDeleteFilesWithRetry(dmsg, 3);
    }

    private void sendDeleteFilesWithRetry(DeleteFilesOnBackupStorageHostMsg dmsg, int maxRetries) {
        bus.send(dmsg, new CloudBusCallBack(null) {
            private int attempt = 1;

            @Override
            public void run(MessageReply deleteReply) {
                if (deleteReply.isSuccess()) {
                    return;
                }
                if (attempt < maxRetries) {
                    attempt++;
                    logger.warn(String.format("failed to cleanup files on backup storage [uuid:%s], retrying (%d/%d): %s",
                            dmsg.getBackupStorageUuid(), attempt, maxRetries, deleteReply.getError()));
                    bus.send(dmsg, this);
                } else {
                    logger.error(String.format("failed to cleanup files on backup storage [uuid:%s] after %d attempts: %s. " +
                                    "Orphan files may remain and require manual cleanup.",
                            dmsg.getBackupStorageUuid(), maxRetries, deleteReply.getError()));
                }
            }
        });
    }
}
