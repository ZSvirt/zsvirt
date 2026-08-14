package org.zstack.header.scheduler;

import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.message.NeedJsonSchema;

import java.util.List;

/**
 * Created by kayo on 2018/3/29.
 */
public class SchedulerCanonicalEvents {
    public static final String VM_START_SCHEDULER_PATH = "/scheduler/vm/start";
    public static final String VM_STOP_SCHEDULER_PATH = "/scheduler/vm/stop";
    public static final String VM_REBOOT_SCHEDULER_PATH = "/scheduler/vm/reboot";
    public static final String VOLUME_SNAPSHOT_SCHEDULER_PATH = "/scheduler/volume/snapshot";
    public static final String VOLUME_SNAPSHOT_GROUP_SCHEDULER_PATH = "/scheduler/volume/snapshot/group";
    public static final String VOLUME_BACKUP_SCHEDULER_PATH = "/scheduler/volume/backup";
    public static final String DATABASE_BACKUP_SCHEDULER_PATH = "/scheduler/database/backup";
    public static final String GROUP_SCHEDULER_PATH = "/scheduler/group";

    @NeedJsonSchema
    public static class SchedulerExecutedData {
        private String jobUuid;
        private String targetResourceUuid;
        private String schedulerName;
        private String resultMessage;
        private boolean success = true;
        private ErrorCode error;

        public ErrorCode getError() {
            return error;
        }

        public void setError(ErrorCode error) {
            this.success = false;
            this.error = error;
        }

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public String getJobUuid() {
            return jobUuid;
        }

        public void setJobUuid(String jobUuid) {
            this.jobUuid = jobUuid;
        }

        public String getTargetResourceUuid() {
            return targetResourceUuid;
        }

        public void setTargetResourceUuid(String targetResourceUuid) {
            this.targetResourceUuid = targetResourceUuid;
        }

        public String getSchedulerName() {
            return schedulerName;
        }

        public void setSchedulerName(String schedulerName) {
            this.schedulerName = schedulerName;
        }

        public String getResultMessage() {
            return resultMessage;
        }

        public void setResultMessage(String resultMessage) {
            this.resultMessage = resultMessage;
        }
    }

    @NeedJsonSchema
    public static class SchedulerGroupExecutedData {
        private String jobGroupUuid;
        private String schedulerGroupName;
        private long totalCount;
        private long failedCount;
        private List<String> errors;

        public String getJobGroupUuid() {
            return jobGroupUuid;
        }

        public void setJobGroupUuid(String jobGroupUuid) {
            this.jobGroupUuid = jobGroupUuid;
        }

        public String getSchedulerGroupName() {
            return schedulerGroupName;
        }

        public void setSchedulerGroupName(String schedulerGroupName) {
            this.schedulerGroupName = schedulerGroupName;
        }

        public long getTotalCount() {
            return totalCount;
        }

        public void setTotalCount(long totalCount) {
            this.totalCount = totalCount;
        }

        public long getFailedCount() {
            return failedCount;
        }

        public void setFailedCount(int failedCount) {
            this.failedCount = failedCount;
        }

        public List<String> getErrors() {
            return errors;
        }

        public void setErrors(List<String> errors) {
            this.errors = errors;
        }
    }
}
