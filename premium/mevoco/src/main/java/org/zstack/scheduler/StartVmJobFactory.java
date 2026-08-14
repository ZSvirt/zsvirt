package org.zstack.scheduler;

import org.zstack.header.scheduler.CreateSchedulerJobDescMsg;
import org.zstack.scheduler.vm.StartVmInstanceJob;

public class StartVmJobFactory implements SchedulerJobFactory {
    @Override
    public SchedulerJob createSchedulerJob(CreateSchedulerJobDescMsg msg) {
        return new StartVmInstanceJob(msg);
    }

    @Override
    public String getJobType() {
        return SchedulerType.START_VM;
    }

    @Override
    public String getJobClassName() {
        return StartVmInstanceJob.class.getName();
    }
}
