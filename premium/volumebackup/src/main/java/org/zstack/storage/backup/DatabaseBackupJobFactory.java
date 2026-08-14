package org.zstack.storage.backup;

import org.zstack.header.scheduler.CreateSchedulerJobDescMsg;
import org.zstack.scheduler.SchedulerJob;
import org.zstack.scheduler.SchedulerJobFactory;
import org.zstack.scheduler.SchedulerType;

import static org.zstack.core.Platform.err;

public class DatabaseBackupJobFactory implements SchedulerJobFactory {
    @Override
    public SchedulerJob createSchedulerJob(CreateSchedulerJobDescMsg msg) {
        return new CreateDatabaseBackupJob(msg);
    }

    @Override
    public String getJobType() {
        return SchedulerType.DATABASE_BACKUP;
    }

    @Override
    public String getJobClassName() {
        return CreateDatabaseBackupJob.class.getName();
    }
}
