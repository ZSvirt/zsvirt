package org.zstack.scheduler;

/**
 * Created by Mei Lei on 7/4/16.
 */
public interface SchedulerConstant {
    String SERVICE_ID = "scheduler";
    String ACTION_CATEGORY = "scheduler";

    String QUOTA_SCHEDULER_NUM = "scheduler.num";

    String SIMPLE_TYPE_STRING = "simple";
    String CRON_TYPE_STRING = "cron";
    String FIRE_INSTANCE_ID = "fireInstanceUuid";
    String GROUP_JOBS_TO_SCHEDULER = "group.jobs.to.scheduler";
    Long TIMESTAMP_MAX_VALUE = 2147454847L;

    String DISASTER_RECOVER_JOB_QUEUE = "disaster-recovery-job";
}
