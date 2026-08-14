package org.zstack.header.core.progress;

/**
 * Created by MaJin on 2019/7/3.
 */
public class PendingTaskInfo extends TaskInfo {
    @Override
    public String toString() {
        return String.format("\nPENDING TASK[NAME: %s, CLASS: %s PENDING TIME: %s secs, INDEX: %s] %s",
                name, className, pendingTime, index, context);
    }
}
