package org.zstack.storage.backup;

import org.zstack.core.db.Q;
import org.zstack.core.gc.*;
import org.zstack.header.core.NopeReturnValueCompletion;
import org.zstack.header.scheduler.SchedulerJobVO;
import org.zstack.header.storage.backup.BackupMode;
import org.zstack.header.vm.VmCanonicalEvents;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.volume.VolumeCanonicalEvents;
import org.zstack.scheduler.SchedulerType;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Created by MaJin on 2019/5/24.
 */

public class CreateVolumeBackupGC extends EventBasedGarbageCollector {
    private final static CLogger logger = Utils.getLogger(CreateVolumeBackupGC.class);

    @GC
    private String jobUuid;

    @GC
    private String volumeUuid;

    @GC
    private String jobData;

    @GC
    private String jobType;

    private final static String name_format = "re-backup-volume-%s-mode-%s";

    @Override
    protected void setup() {
        onEvent(VmCanonicalEvents.VM_FULL_STATE_CHANGED_PATH, ((tokens, data) -> {
            VmCanonicalEvents.VmStateChangedData d = (VmCanonicalEvents.VmStateChangedData) data;
            VmInstanceInventory vm = d.getInventory();

            return d.getNewState().equals(VmInstanceState.Running.toString()) &&
                    vm.getAllVolumes().stream().anyMatch(it -> it.getUuid().equals(volumeUuid));
        }));

        onEvent(VolumeCanonicalEvents.VOLUME_ATTACHED_VM_PATH, ((tokens, data) -> {
            VolumeCanonicalEvents.VolumeAttachedData d = (VolumeCanonicalEvents.VolumeAttachedData) data;

            return d.getVolumeUuid().equals(volumeUuid) && Arrays.asList(
                    VmInstanceState.Running.toString(), VmInstanceState.Paused.toString())
                    .contains(d.getVmInventory().getState());
        }));
    }

    @Override
    protected void triggerNow(GCCompletion completion) {
        if (!VolumeBackupGlobalConfig.REBACKUP_STOPPED_VM.value(Boolean.class) || !dbf.isExist(jobUuid, SchedulerJobVO.class)) {
            completion.cancel();
            return;
        }

        logger.debug(String.format("start to re-backup volume[uuid:%s] once", volumeUuid));
        CreateVolumeBackupJob job = SchedulerType.ROOT_VOLUME_BACKUP.equals(jobType) ?
                JSONObjectUtil.toObject(jobData, CreateRootVolumeBackupJob.class) :
                JSONObjectUtil.toObject(jobData, CreateVolumeBackupJob.class);
        job.execute(job.buildRequest(), new NopeReturnValueCompletion());
        completion.success();
    }

    static CreateVolumeBackupGC valueOf(CreateVolumeBackupJob job) {
        CreateVolumeBackupGC gcJob = new CreateVolumeBackupGC();
        gcJob.NAME = buildName(job);
        gcJob.jobUuid = job.getUuid();
        gcJob.jobData = JSONObjectUtil.toJsonString(job);
        gcJob.volumeUuid = job.getTargetResourceUuid();
        gcJob.jobType = job.getType();
        return gcJob;
    }

    @Override
    protected void deduplicateSubmit() {
        List<GarbageCollectorVO> existGcs = getExistsGcJobs();
        if (existGcs.isEmpty()) {
            submit();
            return;
        }

        GarbageCollectorVO gc = deduplicateVolumeBackupGc(existGcs);
        if (gc != null && NAME.equals(gc.getName()) || getBackupMode() == BackupMode.auto) {
            return;
        }

        submit();
        dbf.remove(gc);
    }

    private GarbageCollectorVO deduplicateVolumeBackupGc(List<GarbageCollectorVO> gcs) {
        if (gcs == null || gcs.isEmpty()) {
            return null;
        }

        if (gcs.size() == 1) {
            return gcs.get(0);
        }

        List<GarbageCollectorVO> fullModeGc = new ArrayList<>();
        List<GarbageCollectorVO> autoModeGc = new ArrayList<>();
        for (GarbageCollectorVO gc : gcs) {
            if (gc.getName().endsWith(BackupMode.full.toString())) {
                fullModeGc.add(gc);
            } else {
                autoModeGc.add(gc);
            }
        }

        if (!fullModeGc.isEmpty()) {
            dbf.removeCollection(autoModeGc, GarbageCollectorVO.class);
            fullModeGc.sort(Comparator.comparing(GarbageCollectorVO::getCreateDate));
            dbf.removeCollection(fullModeGc.subList(0, fullModeGc.size() - 1), GarbageCollectorVO.class);
            return fullModeGc.get(fullModeGc.size() - 1);
        } else if (!autoModeGc.isEmpty()) {
            autoModeGc.sort(Comparator.comparing(GarbageCollectorVO::getCreateDate));
            dbf.removeCollection(autoModeGc.subList(0, autoModeGc.size() - 1), GarbageCollectorVO.class);
            return autoModeGc.get(autoModeGc.size() - 1);
        } else {
            return null;
        }
    }

    private static String buildName(CreateVolumeBackupJob job) {
        return String.format(name_format, job.getTargetResourceUuid(), job.getBackupMode());
    }

    private BackupMode getBackupMode() {
        return NAME.endsWith(BackupMode.full.toString()) ? BackupMode.full : BackupMode.auto;
    }

    private List<GarbageCollectorVO> getExistsGcJobs() {
        return Q.New(GarbageCollectorVO.class).like(GarbageCollectorVO_.name, String.format(name_format, volumeUuid, "%"))
                .notEq(GarbageCollectorVO_.status, GCStatus.Done)
                .list();
    }
}
