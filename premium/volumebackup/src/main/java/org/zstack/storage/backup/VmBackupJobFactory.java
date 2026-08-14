package org.zstack.storage.backup;

import org.zstack.header.scheduler.CreateSchedulerJobDescMsg;
import org.zstack.scheduler.SchedulerJob;
import org.zstack.scheduler.SchedulerJobFactory;
import org.zstack.scheduler.SchedulerType;

import static org.zstack.core.Platform.err;

public class VmBackupJobFactory implements SchedulerJobFactory {
    @Override
    public SchedulerJob createSchedulerJob(CreateSchedulerJobDescMsg msg) {
        return new CreateVmBackupJob(msg);
    }

    @Override
    public String getJobType() {
        return SchedulerType.VM_BACKUP;
    }

    @Override
    public String getJobClassName() {
        return CreateVmBackupJob.class.getName();
    }
}
