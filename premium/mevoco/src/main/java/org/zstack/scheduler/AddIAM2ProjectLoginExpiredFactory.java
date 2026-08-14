package org.zstack.scheduler;

import org.zstack.header.scheduler.CreateSchedulerJobDescMsg;
import org.zstack.scheduler.iam2.AddIAM2ProjectLoginExpiredJob;

/**
 * @Author: DaoDao
 * @Date: 2021/8/23
 */
public class AddIAM2ProjectLoginExpiredFactory implements SchedulerJobFactory {
    @Override
    public SchedulerJob createSchedulerJob(CreateSchedulerJobDescMsg msg) {
        return new AddIAM2ProjectLoginExpiredJob(msg);
    }

    @Override
    public String getJobType() {
        return SchedulerType.ADD_IAM2_PROJECT_LOGIN_EXPIRED;
    }

    @Override
    public String getJobClassName() {
        return AddIAM2ProjectLoginExpiredJob.class.getName();
    }

}

