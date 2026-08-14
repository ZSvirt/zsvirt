package org.zstack.header.scheduler;

public interface SchedulerJobDesc {
    String getUuid();
    String getJobClassName();
    String getJobData();
}
