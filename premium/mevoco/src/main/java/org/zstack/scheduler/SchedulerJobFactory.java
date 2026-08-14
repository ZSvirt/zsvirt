package org.zstack.scheduler;


import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.scheduler.CreateSchedulerJobDescMsg;

/**
 * Created by AlanJager on 2017/6/9.
 */
public interface SchedulerJobFactory {
    SchedulerJob createSchedulerJob(CreateSchedulerJobDescMsg msg);
    String getJobType();
    String getJobClassName();

    default ErrorCode validateMsg(CreateSchedulerJobDescMsg msg) {
        return null;
    }
}
