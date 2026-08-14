package org.zstack.core.thread;

public class ThreadPool {
    private String syncSignature;
    private int threadNum;

    public String getSyncSignature() {
        return syncSignature;
    }

    public void setSyncSignature(String syncSignature) {
        this.syncSignature = syncSignature;
    }

    public int getThreadNum() {
        return threadNum;
    }

    public void setThreadNum(int threadNum) {
        this.threadNum = threadNum;
    }
}
