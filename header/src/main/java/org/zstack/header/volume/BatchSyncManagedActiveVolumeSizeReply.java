package org.zstack.header.volume;

import org.zstack.header.message.MessageReply;

public class BatchSyncManagedActiveVolumeSizeReply extends MessageReply {
    private int successCount;

    private int failCount;

    public void setSuccessCount(int successCount) {
        this.successCount = successCount;
    }

    public int getSuccessCount() {
        return successCount;
    }

    public void setFailCount(int failCount) {
        this.failCount = failCount;
    }

    public int getFailCount() {
        return failCount;
    }

    public synchronized void addSuccessCount(int successCount) {
        this.successCount = this.successCount + successCount;
    }

    public synchronized void addFailCount(int failCount) {
        this.failCount = this.failCount + failCount;
    }
}
