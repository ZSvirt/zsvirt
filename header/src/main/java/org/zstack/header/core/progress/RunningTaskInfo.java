package org.zstack.header.core.progress;

/**
 * Created by MaJin on 2019/7/3.
 */
public class RunningTaskInfo extends TaskInfo {
    @Override
    public String toString() {
        return String.format("\nRUNNING TASK[NAME: %s, CLASS: %s, PENDING TIME: %s sec, EXECUTION TIME: %s secs, INDEX: %s] %s",
                name, className, pendingTime, executionTime, index, context);
    }
}
