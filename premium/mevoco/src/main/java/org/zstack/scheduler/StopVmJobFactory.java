package org.zstack.scheduler;

import org.zstack.header.scheduler.CreateSchedulerJobDescMsg;
import org.zstack.scheduler.vm.StopVmInstanceJob;

public class StopVmJobFactory implements SchedulerJobFactory {
    @Override
    public SchedulerJob createSchedulerJob(CreateSchedulerJobDescMsg msg) {
        return new StopVmInstanceJob(msg);
    }

    @Override
    public String getJobType(){
        return SchedulerType.STOP_VM;
    }

    @Override
    public String getJobClassName() {
        return StopVmInstanceJob.class.getName();
    }
}
