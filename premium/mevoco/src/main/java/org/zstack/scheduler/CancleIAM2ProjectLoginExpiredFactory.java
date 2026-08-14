package org.zstack.scheduler;

import org.zstack.header.scheduler.CreateSchedulerJobDescMsg;
import org.zstack.scheduler.iam2.CancelIAM2ProjectLoginExpiredJob;

/**
 * @Author: DaoDao
 * @Date: 2021/8/23
 */
public class CancleIAM2ProjectLoginExpiredFactory implements SchedulerJobFactory{
    @Override
    public SchedulerJob createSchedulerJob(CreateSchedulerJobDescMsg msg) {
        return new CancelIAM2ProjectLoginExpiredJob(msg);
    }

    @Override
    public String getJobType() {
        return SchedulerType.CANCEL_IAM2_PROJECT_LOGIN_EXPIRED;
    }

    @Override
    public String getJobClassName() {
        return CancelIAM2ProjectLoginExpiredJob.class.getName();
    }

}

