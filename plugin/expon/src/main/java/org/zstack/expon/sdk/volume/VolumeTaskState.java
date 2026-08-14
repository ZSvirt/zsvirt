package org.zstack.expon.sdk.volume;

import org.zstack.expon.sdk.TaskStatus;

public enum VolumeTaskState {
    TASK_COMPLETE,
    TASK_FAILED,
    TASK_RUNNING,
    TASK_TORUN;

    public TaskStatus toTaskStatus() {
        switch (this) {
            case TASK_COMPLETE:
                return TaskStatus.SUCCESS;
            case TASK_FAILED:
                return TaskStatus.FAILED;
            case TASK_RUNNING:
                return TaskStatus.RUNNING;
            case TASK_TORUN:
                return TaskStatus.RUNNING;
            default:
                return TaskStatus.FAILED;
        }
    }
}
