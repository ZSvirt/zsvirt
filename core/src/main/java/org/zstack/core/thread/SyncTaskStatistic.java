package org.zstack.core.thread;

import java.beans.ConstructorProperties;

/**
 */
public class SyncTaskStatistic {
    private String syncSignature;
    private int syncLevel;
    private int currentRunningThreadNum;
    private long pendingTaskNum;

    @ConstructorProperties({"syncSignature", "syncLevel", "currentRunningThreadNum", "pendingTaskNum"})
    public SyncTaskStatistic(String syncSignature, int syncLevel, int currentRunningThreadNum, long pendingTaskNum) {
        this.syncSignature = syncSignature;
        this.syncLevel = syncLevel;
        this.currentRunningThreadNum = currentRunningThreadNum;
        this.pendingTaskNum = pendingTaskNum;
    }

    public String getSyncSignature() {
        return syncSignature;
    }

    public int getSyncLevel() {
        return syncLevel;
    }

    public int getCurrentRunningThreadNum() {
        return currentRunningThreadNum;
    }

    public long getPendingTaskNum() {
        return pendingTaskNum;
    }
}
