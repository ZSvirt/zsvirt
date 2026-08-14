package org.zstack.scheduler;

import org.zstack.header.scheduler.*;
import org.zstack.scheduler.autoscalinggroup.AutoScalingGroupJob;

/**
 * @author qiuyu.zhang
 * @Package org.zstack.scheduler
 * @date 2020/12/8 3:06 PM
 */
public class AutoScalingGroupFactory implements SchedulerJobFactory {
    @Override
    public SchedulerJob createSchedulerJob(CreateSchedulerJobDescMsg msg) {
        return new AutoScalingGroupJob(msg);
    }

    @Override
    public String getJobType() {
        return SchedulerType.RUN_AUTO_SCALING_GROUP;
    }

    @Override
    public String getJobClassName() {
        return AutoScalingGroupJob.class.getName();
    }
}
