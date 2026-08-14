package org.zstack.core.thread;

import java.beans.ConstructorProperties;

/**
 */
public class ThreadPoolStatistic {
    private int totalThreadNum;
    private int activeThreadNum;
    private long completedTaskNum;
    private long pendingTaskNum;
    private long corePoolSize;
    private long maxPoolSize;
    private long queuedTaskNum;

    @ConstructorProperties({"totalThreadNum", "activeThreadNum", "completedTaskNum", "pendingTaskNum", "corePoolSize", "maxPoolSize", "queuedTaskNum"})
    public ThreadPoolStatistic(int totalThreadNum, int activeThreadNum, long completedTaskNum, long pendingTaskNum, long corePoolSize, long maxPoolSize, long queuedTaskNum) {
        this.totalThreadNum = totalThreadNum;
        this.activeThreadNum = activeThreadNum;
        this.completedTaskNum = completedTaskNum;
        this.pendingTaskNum = pendingTaskNum;
        this.corePoolSize = corePoolSize;
        this.maxPoolSize = maxPoolSize;
        this.queuedTaskNum = queuedTaskNum;
    }

    public long getPendingTaskNum() {
        return pendingTaskNum;
    }

    public long getQueuedTaskNum() {
        return queuedTaskNum;
    }

    public long getCorePoolSize() {
        return corePoolSize;
    }

    public long getMaxPoolSize() {
        return maxPoolSize;
    }

    public int getTotalThreadNum() {
        return totalThreadNum;
    }

    public int getActiveThreadNum() {
        return activeThreadNum;
    }

    public long getCompletedTaskNum() {
        return completedTaskNum;
    }

}
