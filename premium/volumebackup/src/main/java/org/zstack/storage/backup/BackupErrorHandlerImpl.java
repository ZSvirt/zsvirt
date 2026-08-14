package org.zstack.storage.backup;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.storage.backup.BackupErrorHandler;
import org.zstack.header.storage.volume.backup.VolumeBackupErrors;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

/**
 * Created by MaJin on 2019/5/24.
 */

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
class BackupErrorHandlerImpl implements BackupErrorHandler {
    private final static CLogger logger = Utils.getLogger(BackupErrorHandlerImpl.class);

    @Autowired
    private ThreadFacade thdf;

    @Override
    public void handle(ErrorCode error, CreateVmBackupJob job) {
        if (!error.isError(VolumeBackupErrors.VM_STOPPED) || !VolumeBackupGlobalConfig.REBACKUP_STOPPED_VM.value(Boolean.class)) {
            return;
        }

        logger.debug(String.format("vm[rootVolUuid:%s] backup fail for stopped vm, submit a GC job to re-backup once if need.",
                job.getTargetResourceUuid()));

        chainSubmit(job.getTargetResourceUuid(), () -> {
            CreateVmBackupGC gc = CreateVmBackupGC.valueOf(job);
            gc.deduplicateSubmit();
        });
    }

    @Override
    public void handle(ErrorCode error, CreateVolumeBackupJob job) {
        if (!error.isError(VolumeBackupErrors.VM_STOPPED) || !VolumeBackupGlobalConfig.REBACKUP_STOPPED_VM.value(Boolean.class)) {
            return;
        }

        logger.debug(String.format("volume[uuid:%s] backup fail for stopped vm, submit a GC job to re-backup once if need.",
                job.getTargetResourceUuid()));

        chainSubmit(job.getTargetResourceUuid(), () -> {
            CreateVolumeBackupGC gc = CreateVolumeBackupGC.valueOf(job);
            gc.deduplicateSubmit();
        });
    }

    private void chainSubmit(String volumeUuid, Runnable runnable) {
        thdf.chainSubmit(new ChainTask(null) {
            @Override
            public String getSyncSignature() {
                return "gc-re-backup-volume-" + volumeUuid;
            }

            @Override
            public void run(SyncTaskChain chain) {
                runnable.run();
                chain.next();
            }

            @Override
            public String getName() {
                return getSyncSignature();
            }
        });
    }
}
