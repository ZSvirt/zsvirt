package org.zstack.storage.backup;

import org.zstack.header.scheduler.CreateSchedulerJobDescMsg;
import org.zstack.scheduler.SchedulerJob;
import org.zstack.scheduler.SchedulerJobFactory;
import org.zstack.scheduler.SchedulerType;

import static org.zstack.core.Platform.err;

public class VolumeBackupJobFactory implements SchedulerJobFactory {
    @Override
    public SchedulerJob createSchedulerJob(CreateSchedulerJobDescMsg msg) {
        return new CreateVolumeBackupJob(msg);
    }

    @Override
    public String getJobType() {
        return SchedulerType.VOLUME_BACKUP;
    }

    @Override
    public String getJobClassName() {
        return CreateVolumeBackupJob.class.getName();
    }
}
