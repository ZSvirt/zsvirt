package org.zstack.storage.backup;

import org.zstack.core.progress.TaskTracker;
import org.zstack.header.storage.backup.BackupConstant;
import org.zstack.header.storage.backup.BackupProcess;
import org.zstack.header.volume.VolumeVO;

import java.util.Collections;

/**
 * Created by MaJin on 2019/3/13.
 */
public class VmBackupTracker extends TaskTracker {
    public static final String TASK_NAME = "SchedulerVmBackup";

    public VmBackupTracker(String rootVolumeUuid) {
        super(rootVolumeUuid);
    }

    @Override
    protected String getResourceType() {
        return VolumeVO.class.getSimpleName();
    }

    @Override
    protected String getTaskName() {
        return TASK_NAME;
    }

    public void track(BackupProcess process){
        track(null, Collections.singletonMap(BackupConstant.PARAM_PROCESS, process.toString()));
    }
}
