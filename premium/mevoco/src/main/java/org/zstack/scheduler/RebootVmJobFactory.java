package org.zstack.scheduler;

import org.zstack.header.scheduler.CreateSchedulerJobDescMsg;
import org.zstack.scheduler.vm.RebootVmInstanceJob;

public class RebootVmJobFactory implements SchedulerJobFactory {
    @Override
    public SchedulerJob createSchedulerJob(CreateSchedulerJobDescMsg msg) {
        return new RebootVmInstanceJob(msg);
    }

    @Override
    public String getJobType() {
        return SchedulerType.REBOOT_VM;
    }

    @Override
    public String getJobClassName() {
        return RebootVmInstanceJob.class.getName();
    }
}
