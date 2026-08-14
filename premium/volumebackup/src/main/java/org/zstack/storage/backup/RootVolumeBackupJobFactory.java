package org.zstack.storage.backup;

import org.zstack.header.scheduler.CreateSchedulerJobDescMsg;
import org.zstack.scheduler.SchedulerJob;
import org.zstack.scheduler.SchedulerType;

public class RootVolumeBackupJobFactory extends VolumeBackupJobFactory {
    @Override
    public String getJobType() {
        return SchedulerType.ROOT_VOLUME_BACKUP;
    }

    @Override
    public SchedulerJob createSchedulerJob(CreateSchedulerJobDescMsg msg) {
        return new CreateRootVolumeBackupJob(msg);
    }

    @Override
    public String getJobClassName() {
        return CreateRootVolumeBackupJob.class.getName();
    }
}
